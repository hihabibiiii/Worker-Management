const {HttpsError} = require("firebase-functions/v2/https");
const {
  ALLOWED_PRODUCTS,
  hashToken,
  getObfuscatedAccountId,
  parseSubscriptionDetails,
  deriveActiveEntitlement,
} = require("./subscriptionLogic");

/**
 * Core verification service logic supporting dependency injection for testing.
 * Strictly enforces Firestore read-before-write transactional ordering.
 * @param {object} params
 * @param {string} params.uid
 * @param {string} params.purchaseToken
 * @param {string} params.productId
 * @param {string} params.packageName
 * @param {object} params.db Firestore database instance
 * @param {object} params.androidPublisher Google Play API client
 * @param {object} [params.fieldValue] FieldValue provider
 * @return {Promise<object>}
 */
async function processSubscriptionVerification({
  uid,
  purchaseToken,
  productId,
  packageName,
  db,
  androidPublisher,
  fieldValue,
}) {
  if (!uid) {
    throw new HttpsError("unauthenticated", "Please sign in before verifying your subscription.");
  }

  if (typeof purchaseToken !== "string" || purchaseToken.length < 10 || purchaseToken.length > 4096) {
    throw new HttpsError("invalid-argument", "A valid purchase token is required.");
  }

  if (typeof productId !== "string" || !ALLOWED_PRODUCTS.has(productId)) {
    throw new HttpsError("invalid-argument", "Unsupported subscription product.");
  }

  const tokenHash = hashToken(purchaseToken);
  const tokenOwnerRef = db.collection("subscriptionTokenOwners").doc(tokenHash);
  const userPurchasesCol = db.collection("userSubscriptions").doc(uid).collection("purchases");
  const thisPurchaseRef = userPurchasesCol.doc(tokenHash);
  const entitlementRef = db.collection("subscriptionEntitlements").doc(uid);

  // Pre-check token ownership to reject hijacked tokens before contacting Google Play
  const initialOwnerSnap = await tokenOwnerRef.get();
  if (initialOwnerSnap.exists && initialOwnerSnap.data().uid !== uid) {
    throw new HttpsError("permission-denied", "This purchase is already linked to another account.");
  }

  let response;
  try {
    response = await androidPublisher.purchases.subscriptionsv2.get({
      packageName,
      token: purchaseToken,
    });
  } catch (error) {
    if (error.code === 404) {
      throw new HttpsError("not-found", "Subscription purchase not found on Google Play.");
    }
    throw new HttpsError("failed-precondition", "Google Play could not verify this purchase. Please try again.");
  }

  const subscription = response.data;

  // Validate Obfuscated External Account ID (with legacy raw UID fallback)
  const externalAccountId = subscription.externalAccountIdentifiers?.obfuscatedExternalAccountId;
  const expectedObfuscatedId = getObfuscatedAccountId(uid);

  if (
    externalAccountId &&
    externalAccountId !== expectedObfuscatedId &&
    externalAccountId !== uid
  ) {
    throw new HttpsError("permission-denied", "This purchase was completed by a different account.");
  }

  const details = parseSubscriptionDetails(subscription, productId);

  // Handle purchase acknowledgement with Google Play
  let isAcknowledged = subscription.acknowledgementState === "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED";
  let acknowledgementPending = false;

  if (
    subscription.acknowledgementState === "ACKNOWLEDGEMENT_STATE_PENDING" &&
    details.subscriptionState !== "SUBSCRIPTION_STATE_PENDING"
  ) {
    try {
      await androidPublisher.purchases.subscriptions.acknowledge({
        packageName,
        subscriptionId: details.productId,
        token: purchaseToken,
        requestBody: {},
      });
      isAcknowledged = true;
    } catch (ackError) {
      acknowledgementPending = true;
    }
  }

  let derived;
  const serverTimestamp = fieldValue?.serverTimestamp ? fieldValue.serverTimestamp() : new Date();

  await db.runTransaction(async (transaction) => {
    // ==========================================
    // 1. TRANSACTION READ PHASE (MUST BE FIRST)
    // ==========================================
    const ownerSnap = await transaction.get(tokenOwnerRef);

    let linkedOwnerSnap = null;
    let linkedTokenHash = null;
    let linkedPurchaseRef = null;

    if (details.linkedPurchaseToken) {
      linkedTokenHash = hashToken(details.linkedPurchaseToken);
      const linkedOwnerRef = db.collection("subscriptionTokenOwners").doc(linkedTokenHash);
      linkedPurchaseRef = userPurchasesCol.doc(linkedTokenHash);
      linkedOwnerSnap = await transaction.get(linkedOwnerRef);
    }

    const allPurchasesSnap = await transaction.get(userPurchasesCol);
    const existingEntitlementSnap = await transaction.get(entitlementRef);

    // ==========================================
    // 2. TRANSACTION LOGIC & VALIDATION PHASE
    // ==========================================
    if (ownerSnap.exists && ownerSnap.data().uid !== uid) {
      throw new HttpsError("permission-denied", "This purchase is already linked to another account.");
    }

    const purchasesList = [];
    if (allPurchasesSnap && allPurchasesSnap.docs) {
      allPurchasesSnap.docs.forEach((doc) => {
        if (doc.id !== tokenHash) {
          const docData = doc.data();
          // If this document is the linked token and was owned by this user, mark it replaced in memory
          const isLinkedTokenOwned =
            linkedTokenHash &&
            doc.id === linkedTokenHash &&
            linkedOwnerSnap?.exists &&
            linkedOwnerSnap.data().uid === uid;

          if (isLinkedTokenOwned) {
            purchasesList.push({tokenHash: doc.id, ...docData, isReplaced: true});
          } else {
            purchasesList.push({tokenHash: doc.id, ...docData});
          }
        }
      });
    }

    // Add current verified purchase
    const currentPurchaseRecord = {
      tokenHash,
      productId: details.productId,
      subscriptionState: details.subscriptionState,
      expiryTime: details.expiryTime,
      expiryMillis: details.expiryMillis,
      isPro: details.isPro,
      autoRenewEnabled: details.autoRenewEnabled,
      acknowledged: isAcknowledged,
      linkedPurchaseToken: details.linkedPurchaseToken,
      verifiedAt: serverTimestamp,
      source: "google_play",
    };

    purchasesList.push(currentPurchaseRecord);

    // Derive active entitlement with full anti-downgrade and tier resolution
    derived = deriveActiveEntitlement(purchasesList);

    // ==========================================
    // 3. TRANSACTION WRITE PHASE (ONLY AFTER READS)
    // ==========================================
    // A. Update linked purchase record if valid replacement
    if (linkedTokenHash && linkedPurchaseRef && linkedOwnerSnap?.exists && linkedOwnerSnap.data().uid === uid) {
      transaction.set(
          linkedPurchaseRef,
          {
            isReplaced: true,
            replacedByTokenHash: tokenHash,
            updatedAt: serverTimestamp,
          },
          {merge: true},
      );
    }

    // B. Save token ownership
    transaction.set(
        tokenOwnerRef,
        {
          uid,
          productId: details.productId,
          updatedAt: serverTimestamp,
        },
        {merge: true},
    );

    // C. Save individual purchase record
    transaction.set(thisPurchaseRef, currentPurchaseRecord, {merge: true});

    // D. Save derived active entitlement document
    const previousAcknowledged = existingEntitlementSnap.exists ?
      Boolean(existingEntitlementSnap.data().acknowledged) :
      false;

    transaction.set(
        entitlementRef,
        {
          uid,
          productId: derived.productId,
          subscriptionState: derived.subscriptionState,
          expiryTime: derived.expiryTime,
          isPro: derived.isPro,
          autoRenewEnabled: derived.autoRenewEnabled,
          activeTokenHash: derived.activeTokenHash,
          acknowledged: derived.acknowledged || isAcknowledged || previousAcknowledged,
          verifiedAt: serverTimestamp,
          source: "google_play",
        },
        {merge: true},
    );
  });

  return {
    success: true,
    productId: derived.productId,
    subscriptionState: derived.subscriptionState,
    expiryTime: derived.expiryTime,
    isPro: derived.isPro,
    autoRenewEnabled: derived.autoRenewEnabled,
    acknowledgementPending,
  };
}

module.exports = {
  processSubscriptionVerification,
};

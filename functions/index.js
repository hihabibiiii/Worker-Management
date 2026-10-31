const {setGlobalOptions} = require("firebase-functions");
const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore, FieldValue} = require("firebase-admin/firestore");
const {google} = require("googleapis");
const {processSubscriptionVerification} = require("./lib/verificationService");
const {
  initializeQuotaLedger,
  createSiteWithQuota,
  createWorkerWithQuota,
  softDeleteEntityWithQuota,
  restoreEntityWithQuota,
  permanentDeleteEntityWithQuota,
  migrateUserQuotaToAllocations,
  reconcileQuotaLedger,
} = require("./lib/quotaService");

setGlobalOptions({maxInstances: 10});

initializeApp();

const db = getFirestore();

const PACKAGE_NAME = "com.habibullahsalmani.workcraft";
const RUNTIME_SERVICE_ACCOUNT = "workcraft-subscription-verifie@workcraft-ad559.iam.gserviceaccount.com";

const googleAuth = new google.auth.GoogleAuth({
  scopes: ["https://www.googleapis.com/auth/androidpublisher"],
});

const androidPublisher = google.androidpublisher({
  version: "v3",
  auth: googleAuth,
});

/**
 * Callable function to verify and store a newly initiated or restored subscription purchase.
 */
exports.verifySubscriptionPurchase = onCall(
    {
      timeoutSeconds: 30,
      memory: "256MiB",
      serviceAccount: RUNTIME_SERVICE_ACCOUNT,
    },
    async (request) => {
      const uid = request.auth?.uid;
      const {purchaseToken, productId, obfuscatedAccountId} = request.data || {};
      return processSubscriptionVerification({
        uid,
        purchaseToken,
        productId,
        obfuscatedAccountId,
        packageName: PACKAGE_NAME,
        androidPublisher,
        db,
        fieldValue: FieldValue,
      });
    },
);

/**
 * Backward compatibility alias for verifySubscriptionPurchase.
 */
exports.verifyGooglePlaySubscription = exports.verifySubscriptionPurchase;

/**
 * Callable function to acknowledge a verified subscription with Google Play.
 */
exports.acknowledgeSubscriptionPurchase = onCall(
    {
      timeoutSeconds: 30,
      memory: "256MiB",
      serviceAccount: RUNTIME_SERVICE_ACCOUNT,
    },
    async (request) => {
      const uid = request.auth?.uid;
      if (!uid) {
        throw new HttpsError("unauthenticated", "User must be authenticated.");
      }
      const {purchaseToken, productId} = request.data || {};
      if (!purchaseToken || !productId) {
        throw new HttpsError("invalid-argument", "Missing purchaseToken or productId.");
      }

      await androidPublisher.purchases.subscriptions.acknowledge({
        packageName: PACKAGE_NAME,
        subscriptionId: productId,
        token: purchaseToken,
      });

      const entitlementRef = db.collection("subscriptionEntitlements").doc(uid);
      await entitlementRef.set(
          {
            acknowledged: true,
            updatedAt: FieldValue.serverTimestamp(),
          },
          {merge: true},
      );

      return {success: true, acknowledged: true};
    },
);

/**
 * Callable function to fetch the current user's authoritative entitlement record.
 */
exports.getMySubscription = onCall(
    {
      timeoutSeconds: 15,
      memory: "256MiB",
      serviceAccount: RUNTIME_SERVICE_ACCOUNT,
    },
    async (request) => {
      const uid = request.auth?.uid;
      if (!uid) {
        throw new HttpsError("unauthenticated", "User must be authenticated.");
      }

      const docSnap = await db.collection("subscriptionEntitlements").doc(uid).get();
      if (!docSnap.exists) {
        return {isPro: false, subscriptionState: "NO_SUBSCRIPTION"};
      }

      const data = docSnap.data();
      const isPro = Boolean(data.isPro && (data.expiryMillis ? data.expiryMillis > Date.now() : true));

      return {
        isPro,
        productId: data.productId || null,
        subscriptionState: data.subscriptionState || "UNKNOWN",
        expiryTime: data.expiryTime || null,
        autoRenewEnabled: Boolean(data.autoRenewEnabled),
        acknowledged: Boolean(data.acknowledged),
      };
    },
);

/**
 * Callable Cloud Function to initialize the quota ledger for existing or new users.
 */
exports.initializeQuotaLedger = onCall(
    {
      timeoutSeconds: 30,
      memory: "256MiB",
      serviceAccount: RUNTIME_SERVICE_ACCOUNT,
    },
    async (request) => {
      const uid = request.auth?.uid;
      return initializeQuotaLedger({
        uid,
        db,
        fieldValue: FieldValue,
      });
    },
);

/**
 * Callable Cloud Function to create a Site with atomic server-side quota enforcement.
 */
exports.createSiteWithQuota = onCall(
    {
      timeoutSeconds: 30,
      memory: "256MiB",
      serviceAccount: RUNTIME_SERVICE_ACCOUNT,
    },
    async (request) => {
      const uid = request.auth?.uid;
      const siteData = request.data?.siteData;
      return createSiteWithQuota({
        uid,
        siteData,
        db,
        fieldValue: FieldValue,
      });
    },
);

/**
 * Callable Cloud Function to create a Worker with atomic server-side quota enforcement.
 */
exports.createWorkerWithQuota = onCall(
    {
      timeoutSeconds: 30,
      memory: "256MiB",
      serviceAccount: RUNTIME_SERVICE_ACCOUNT,
    },
    async (request) => {
      const uid = request.auth?.uid;
      const workerData = request.data?.workerData;
      return createWorkerWithQuota({
        uid,
        workerData,
        db,
        fieldValue: FieldValue,
      });
    },
);

/**
 * Callable Cloud Function to soft delete a Site or Worker and update quota ledger atomically.
 */
exports.softDeleteEntityWithQuota = onCall(
    {
      timeoutSeconds: 30,
      memory: "256MiB",
      serviceAccount: RUNTIME_SERVICE_ACCOUNT,
    },
    async (request) => {
      const uid = request.auth?.uid;
      const entityType = request.data?.entityType;
      const syncId = request.data?.syncId;
      return softDeleteEntityWithQuota({
        uid,
        entityType,
        syncId,
        db,
        fieldValue: FieldValue,
      });
    },
);

/**
 * Callable Cloud Function to restore a Site or Worker if within quota.
 */
exports.restoreEntityWithQuota = onCall(
    {
      timeoutSeconds: 30,
      memory: "256MiB",
      serviceAccount: RUNTIME_SERVICE_ACCOUNT,
    },
    async (request) => {
      const uid = request.auth?.uid;
      const entityType = request.data?.entityType;
      const syncId = request.data?.syncId;
      return restoreEntityWithQuota({
        uid,
        entityType,
        syncId,
        db,
        fieldValue: FieldValue,
      });
    },
);

/**
 * Callable Cloud Function to permanently delete a Site or Worker and release quota.
 */
exports.permanentDeleteEntityWithQuota = onCall(
    {
      timeoutSeconds: 30,
      memory: "256MiB",
      serviceAccount: RUNTIME_SERVICE_ACCOUNT,
    },
    async (request) => {
      const uid = request.auth?.uid;
      const entityType = request.data?.entityType;
      const syncId = request.data?.syncId;
      return permanentDeleteEntityWithQuota({
        uid,
        entityType,
        syncId,
        db,
        fieldValue: FieldValue,
      });
    },
);

/**
 * Callable Cloud Function to migrate a user's legacy quota state to individual allocation documents.
 */
exports.migrateUserQuota = onCall(
    {
      timeoutSeconds: 60,
      memory: "256MiB",
      serviceAccount: RUNTIME_SERVICE_ACCOUNT,
    },
    async (request) => {
      const uid = request.auth?.uid;
      const {batchSize, startAfterSiteId, startAfterWorkerId} = request.data || {};
      return migrateUserQuotaToAllocations({
        uid,
        db,
        fieldValue: FieldValue,
        batchSize,
        startAfterSiteId,
        startAfterWorkerId,
      });
    },
);

/**
 * Administrative Cloud Function to reconcile quota allocations.
 * Requires Admin Custom Claim (request.auth.token.admin === true).
 */
exports.reconcileQuotaLedger = onCall(
    {
      timeoutSeconds: 60,
      memory: "512MiB",
      serviceAccount: RUNTIME_SERVICE_ACCOUNT,
    },
    async (request) => {
      const adminUid = request.auth?.uid;
      const isAdmin = Boolean(request.auth?.token?.admin);
      const targetUid = request.data?.targetUid;
      return reconcileQuotaLedger({
        adminUid,
        targetUid,
        isAdmin,
        db,
        fieldValue: FieldValue,
      });
    },
);

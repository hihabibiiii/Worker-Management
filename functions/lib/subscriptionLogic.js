const crypto = require("crypto");
const {HttpsError} = require("firebase-functions/v2/https");

const ALLOWED_PRODUCTS = new Set([
  "workcraft_pro_monthly",
  "workcraft_pro_yearly",
]);

const PRODUCT_TIERS = {
  "workcraft_pro_yearly": 2,
  "workcraft_pro_monthly": 1,
};

/**
 * Creates a deterministic SHA-256 hash of a purchase token.
 * @param {string} token
 * @return {string}
 */
function hashToken(token) {
  return crypto.createHash("sha256").update(token).digest("hex");
}

/**
 * Generates an obfuscated account identifier from a Firebase UID.
 * Matches the Kotlin client's SHA-256("workcraft_user_" + uid).
 * @param {string} uid
 * @return {string}
 */
function getObfuscatedAccountId(uid) {
  return crypto.createHash("sha256").update(`workcraft_user_${uid}`).digest("hex");
}

/**
 * Evaluates subscription purchase data against Google Play subscription semantics.
 * Strictly verifies expiry dates and prevents granting access to malformed records.
 * @param {object} subscription Google Play SubscriptionPurchaseV2 response
 * @param {string} requestedProductId
 * @return {object}
 */
function parseSubscriptionDetails(subscription, requestedProductId) {
  const lineItems = subscription.lineItems || [];

  if (!Array.isArray(lineItems) || lineItems.length === 0) {
    throw new HttpsError(
        "invalid-argument",
        "Google Play subscription record contains no line items.",
    );
  }

  const matchingItem = lineItems.find(
      (item) => item.productId === requestedProductId,
  );

  if (!matchingItem) {
    throw new HttpsError(
        "invalid-argument",
        "The purchased product does not match any valid subscription line item.",
    );
  }

  const expiryTime = matchingItem.expiryTime;
  if (!expiryTime || typeof expiryTime !== "string" || expiryTime.trim() === "") {
    throw new HttpsError(
        "failed-precondition",
        "Google Play subscription record is missing an expiry time.",
    );
  }

  const expiryMillis = Date.parse(expiryTime);
  if (!Number.isFinite(expiryMillis)) {
    throw new HttpsError(
        "internal",
        "Google Play returned an invalid subscription expiry time format.",
    );
  }

  const state = subscription.subscriptionState || "SUBSCRIPTION_STATE_UNSPECIFIED";
  const now = Date.now();
  const isWithinPaidPeriod = expiryMillis > now;

  let isEntitled = false;

  switch (state) {
    case "SUBSCRIPTION_STATE_ACTIVE":
    case "SUBSCRIPTION_STATE_IN_GRACE_PERIOD":
      // Active and in-grace-period both require a valid future expiry date.
      isEntitled = isWithinPaidPeriod;
      break;

    case "SUBSCRIPTION_STATE_CANCELED":
      // Canceled subscriptions remain entitled only until the end of the paid billing period.
      isEntitled = isWithinPaidPeriod;
      break;

    case "SUBSCRIPTION_STATE_ON_HOLD":
    case "SUBSCRIPTION_STATE_PAUSED":
    case "SUBSCRIPTION_STATE_PENDING":
    case "SUBSCRIPTION_STATE_EXPIRED":
    case "SUBSCRIPTION_STATE_UNSPECIFIED":
    default:
      isEntitled = false;
      break;
  }

  const autoRenewEnabled = Boolean(
      matchingItem.autoRenewingPlan?.autoRenewEnabled,
  );

  return {
    productId: matchingItem.productId,
    subscriptionState: state,
    expiryTime,
    expiryMillis,
    isPro: isEntitled,
    autoRenewEnabled,
    linkedPurchaseToken: subscription.linkedPurchaseToken || null,
  };
}

/**
 * Derives the user's active entitlement from all verified purchase records.
 * Resolves upgrades, renewals, and protects against downgrade by older tokens.
 * @param {Array<object>} purchases
 * @param {number} [now=Date.now()]
 * @return {object}
 */
function deriveActiveEntitlement(purchases, now = Date.now()) {
  if (!Array.isArray(purchases) || purchases.length === 0) {
    return {
      isPro: false,
      productId: null,
      subscriptionState: "NOT_FOUND",
      expiryTime: null,
      autoRenewEnabled: false,
      activeTokenHash: null,
      acknowledged: false,
    };
  }

  // Filter for currently active purchases (excluding replaced/superseded tokens)
  const activePurchases = purchases.filter((p) => {
    if (p.isReplaced) return false;
    const expiry = p.expiryTime ? Date.parse(p.expiryTime) : 0;
    const isStateActive =
      p.subscriptionState === "SUBSCRIPTION_STATE_ACTIVE" ||
      p.subscriptionState === "SUBSCRIPTION_STATE_IN_GRACE_PERIOD" ||
      p.subscriptionState === "SUBSCRIPTION_STATE_CANCELED";
    return p.isPro && isStateActive && expiry > now;
  });

  if (activePurchases.length === 0) {
    // Sort expired purchases to report the most recent expiration
    const sortedExpired = [...purchases].sort((a, b) => {
      const bExp = b.expiryTime ? Date.parse(b.expiryTime) : 0;
      const aExp = a.expiryTime ? Date.parse(a.expiryTime) : 0;
      return bExp - aExp;
    });

    const mostRecent = sortedExpired[0];
    return {
      isPro: false,
      productId: mostRecent.productId || null,
      subscriptionState: mostRecent.subscriptionState || "EXPIRED",
      expiryTime: mostRecent.expiryTime || null,
      autoRenewEnabled: false,
      activeTokenHash: mostRecent.tokenHash || null,
      acknowledged: Boolean(mostRecent.acknowledged),
    };
  }

  // Sort active purchases: Higher Tier first, then longest expiry
  activePurchases.sort((a, b) => {
    const tierA = PRODUCT_TIERS[a.productId] || 0;
    const tierB = PRODUCT_TIERS[b.productId] || 0;
    if (tierA !== tierB) {
      return tierB - tierA; // Higher tier wins (Yearly > Monthly)
    }
    const expiryA = a.expiryTime ? Date.parse(a.expiryTime) : 0;
    const expiryB = b.expiryTime ? Date.parse(b.expiryTime) : 0;
    return expiryB - expiryA; // Longest validity wins
  });

  const best = activePurchases[0];
  return {
    isPro: true,
    productId: best.productId,
    subscriptionState: best.subscriptionState,
    expiryTime: best.expiryTime,
    autoRenewEnabled: Boolean(best.autoRenewEnabled),
    activeTokenHash: best.tokenHash,
    acknowledged: Boolean(best.acknowledged),
  };
}

module.exports = {
  ALLOWED_PRODUCTS,
  PRODUCT_TIERS,
  hashToken,
  getObfuscatedAccountId,
  parseSubscriptionDetails,
  deriveActiveEntitlement,
};


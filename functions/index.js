const {setGlobalOptions} = require("firebase-functions");
const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore, FieldValue} = require("firebase-admin/firestore");
const {google} = require("googleapis");
const {processSubscriptionVerification} = require("./lib/verificationService");

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
 * Callable Cloud Function to verify a Google Play subscription and manage entitlement.
 */
exports.verifyGooglePlaySubscription = onCall(
    {
      timeoutSeconds: 60,
      memory: "256MiB",
      serviceAccount: RUNTIME_SERVICE_ACCOUNT,
    },
    async (request) => {
      const uid = request.auth?.uid;
      const purchaseToken = request.data?.purchaseToken;
      const productId = request.data?.productId;

      return processSubscriptionVerification({
        uid,
        purchaseToken,
        productId,
        packageName: PACKAGE_NAME,
        db,
        androidPublisher,
        fieldValue: FieldValue,
      });
    },
);

/**
 * Callable Cloud Function to fetch the caller's server-managed subscription entitlement.
 */
exports.getMySubscription = onCall(
    {
      timeoutSeconds: 30,
      memory: "256MiB",
      serviceAccount: RUNTIME_SERVICE_ACCOUNT,
    },
    async (request) => {
      if (!request.auth) {
        throw new HttpsError(
            "unauthenticated",
            "Please sign in to view your subscription status.",
        );
      }

      const uid = request.auth.uid;
      const snapshot = await db.collection("subscriptionEntitlements").doc(uid).get();

      if (!snapshot.exists) {
        return {
          isPro: false,
          subscriptionState: "NOT_FOUND",
          expiryTime: null,
          autoRenewEnabled: false,
        };
      }

      const data = snapshot.data();
      const expiryMillis = data.expiryTime ? Date.parse(data.expiryTime) : 0;
      const now = Date.now();
      const isWithinPaidPeriod = Number.isFinite(expiryMillis) && expiryMillis > now;

      const stateAllowsAccess =
        data.subscriptionState === "SUBSCRIPTION_STATE_ACTIVE" ||
        data.subscriptionState === "SUBSCRIPTION_STATE_IN_GRACE_PERIOD" ||
        (data.subscriptionState === "SUBSCRIPTION_STATE_CANCELED" && isWithinPaidPeriod);

      const isPro = Boolean(data.isPro && isWithinPaidPeriod && stateAllowsAccess);

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

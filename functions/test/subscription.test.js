const {test, describe} = require("node:test");
const assert = require("node:assert/strict");

const {
  parseSubscriptionDetails,
  hashToken,
  getObfuscatedAccountId,
  deriveActiveEntitlement,
} = require("../lib/subscriptionLogic");
const {processSubscriptionVerification} = require("../lib/verificationService");

/**
 * Creates an in-memory Firestore mock that strictly enforces real Firestore rules,
 * specifically that ALL transaction reads must precede ALL transaction writes.
 */
function createMockFirestore() {
  const store = new Map();
  const docVersions = new Map();

  function getDocPath(colPath, docId) {
    return `${colPath}/${docId}`;
  }

  return {
    _store: store,
    collection(colName) {
      return {
        doc(docId) {
          const path = getDocPath(colName, docId);
          return {
            id: docId,
            path,
            async get() {
              const data = store.get(path);
              return {
                exists: Boolean(data),
                data: () => (data ? {...data} : undefined),
              };
            },
            collection(subColName) {
              const subPath = `${path}/${subColName}`;
              return {
                doc(subDocId) {
                  const itemPath = `${subPath}/${subDocId}`;
                  return {
                    id: subDocId,
                    path: itemPath,
                    async get() {
                      const data = store.get(itemPath);
                      return {
                        exists: Boolean(data),
                        data: () => (data ? {...data} : undefined),
                      };
                    },
                  };
                },
                async get() {
                  const docs = [];
                  for (const [key, val] of store.entries()) {
                    if (key.startsWith(subPath + "/")) {
                      const id = key.substring(subPath.length + 1);
                      if (!id.includes("/")) {
                        docs.push({id, data: () => ({...val})});
                      }
                    }
                  }
                  return {docs};
                },
              };
            },
          };
        },
      };
    },
    async runTransaction(updateFunction) {
      let writeOccurred = false;
      const readVersions = new Map();
      const pendingWrites = [];

      const transaction = {
        async get(docRef) {
          // STRICT FIRESTORE CONSTRAINT: All reads must happen before any writes
          if (writeOccurred) {
            throw new Error("Firestore transactions require all reads to be executed before all writes.");
          }
          const currentVersion = docVersions.get(docRef.path) || 0;
          readVersions.set(docRef.path, currentVersion);
          return docRef.get();
        },
        set(docRef, data, options) {
          writeOccurred = true;
          pendingWrites.push({docRef, data, options});
        },
      };

      await updateFunction(transaction);

      // Verify no concurrent modifications occurred during transaction execution
      for (const [path, expectedVersion] of readVersions.entries()) {
        const actualVersion = docVersions.get(path) || 0;
        if (actualVersion !== expectedVersion) {
          throw new Error("Transaction failed due to concurrent modification.");
        }
      }

      // Commit transaction atomically
      for (const write of pendingWrites) {
        const path = write.docRef.path;
        const existing = store.get(path) || {};
        if (write.options?.merge) {
          store.set(path, {...existing, ...write.data});
        } else {
          store.set(path, {...write.data});
        }
        docVersions.set(path, (docVersions.get(path) || 0) + 1);
      }
    },
  };
}

describe("WorkCraft Subscription Backend Behavioral & Security Test Suite", () => {
  const futureExpiry = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString();
  const yearlyExpiry = new Date(Date.now() + 365 * 24 * 60 * 60 * 1000).toISOString();
  const pastExpiry = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();

  // Test 1: Missing Expiry Time
  test("1. Missing expiry time in Google Play record throws failed-precondition", () => {
    const raw = {
      subscriptionState: "SUBSCRIPTION_STATE_ACTIVE",
      lineItems: [{productId: "workcraft_pro_monthly"}],
    };
    assert.throws(
        () => parseSubscriptionDetails(raw, "workcraft_pro_monthly"),
        (err) => err.code === "failed-precondition",
    );
  });

  // Test 2: Malformed Expiry Time
  test("2. Malformed expiry timestamp throws internal error", () => {
    const raw = {
      subscriptionState: "SUBSCRIPTION_STATE_ACTIVE",
      lineItems: [{productId: "workcraft_pro_monthly", expiryTime: "not-a-timestamp"}],
    };
    assert.throws(
        () => parseSubscriptionDetails(raw, "workcraft_pro_monthly"),
        (err) => err.code === "internal",
    );
  });

  // Test 3: Expired Purchase Token
  test("3. Expired purchase token results in isPro: false", () => {
    const raw = {
      subscriptionState: "SUBSCRIPTION_STATE_ACTIVE",
      lineItems: [{productId: "workcraft_pro_monthly", expiryTime: pastExpiry}],
    };
    const res = parseSubscriptionDetails(raw, "workcraft_pro_monthly");
    assert.equal(res.isPro, false);
  });

  // Test 4: Grace Period and Canceled within paid period
  test("4. In-Grace-Period and Canceled-within-paid-period are both entitled", () => {
    const graceRaw = {
      subscriptionState: "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
      lineItems: [{productId: "workcraft_pro_monthly", expiryTime: futureExpiry}],
    };
    const cancelRaw = {
      subscriptionState: "SUBSCRIPTION_STATE_CANCELED",
      lineItems: [{productId: "workcraft_pro_monthly", expiryTime: futureExpiry}],
    };

    assert.equal(parseSubscriptionDetails(graceRaw, "workcraft_pro_monthly").isPro, true);
    assert.equal(parseSubscriptionDetails(cancelRaw, "workcraft_pro_monthly").isPro, true);
  });

  // Test 5: On-Hold, Paused, Pending states
  test("5. On-Hold, Paused, and Pending subscriptions deny Pro access", () => {
    const states = [
      "SUBSCRIPTION_STATE_ON_HOLD",
      "SUBSCRIPTION_STATE_PAUSED",
      "SUBSCRIPTION_STATE_PENDING",
    ];

    for (const state of states) {
      const raw = {
        subscriptionState: state,
        lineItems: [{productId: "workcraft_pro_yearly", expiryTime: futureExpiry}],
      };
      const res = parseSubscriptionDetails(raw, "workcraft_pro_yearly");
      assert.equal(res.isPro, false, `State ${state} must not grant Pro access`);
    }
  });

  // Test 6: Obfuscated Account ID calculation
  test("6. Obfuscated account ID calculation is deterministic and non-empty", () => {
    const uid = "firebaseUser_test_12345";
    const obf1 = getObfuscatedAccountId(uid);
    const obf2 = getObfuscatedAccountId(uid);
    assert.equal(obf1, obf2);
    assert.equal(obf1.length, 64);
    assert.notEqual(obf1, uid);
  });

  // Test 7: Obfuscated ID Mismatch Rejection
  test("7. Mismatched external obfuscated account ID is rejected with permission-denied", async () => {
    const mockDb = createMockFirestore();
    const token = "token_obf_mismatch_12345";

    const mockPublisher = {
      purchases: {
        subscriptionsv2: {
          get: async () => ({
            data: {
              subscriptionState: "SUBSCRIPTION_STATE_ACTIVE",
              acknowledgementState: "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED",
              externalAccountIdentifiers: {
                obfuscatedExternalAccountId: getObfuscatedAccountId("another_user"),
              },
              lineItems: [{productId: "workcraft_pro_monthly", expiryTime: futureExpiry}],
            },
          }),
        },
        subscriptions: {acknowledge: async () => ({})},
      },
    };

    await assert.rejects(
        async () => {
          await processSubscriptionVerification({
            uid: "legitimate_user",
            purchaseToken: token,
            productId: "workcraft_pro_monthly",
            packageName: "com.habibullahsalmani.workcraft",
            db: mockDb,
            androidPublisher: mockPublisher,
          });
        },
        (err) => err.code === "permission-denied",
    );
  });

  // Test 8: Token Ownership: User A Claims Token, User B Is Rejected
  test("8. Token Ownership: User A claims token, User B is rejected without corrupting records", async () => {
    const mockDb = createMockFirestore();
    const token = "token_abc_1234567890";
    const tokenHash = hashToken(token);

    const mockPublisher = {
      purchases: {
        subscriptionsv2: {
          get: async () => ({
            data: {
              subscriptionState: "SUBSCRIPTION_STATE_ACTIVE",
              acknowledgementState: "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED",
              lineItems: [{productId: "workcraft_pro_monthly", expiryTime: futureExpiry}],
            },
          }),
        },
        subscriptions: {acknowledge: async () => ({})},
      },
    };

    // User A claims token
    const resA = await processSubscriptionVerification({
      uid: "user_A",
      purchaseToken: token,
      productId: "workcraft_pro_monthly",
      packageName: "com.habibullahsalmani.workcraft",
      db: mockDb,
      androidPublisher: mockPublisher,
    });
    assert.equal(resA.isPro, true);

    const ownerDataA = mockDb._store.get(`subscriptionTokenOwners/${tokenHash}`);
    assert.equal(ownerDataA.uid, "user_A");

    // User B attempts to claim the same token -> must be rejected
    await assert.rejects(
        async () => {
          await processSubscriptionVerification({
            uid: "user_B",
            purchaseToken: token,
            productId: "workcraft_pro_monthly",
            packageName: "com.habibullahsalmani.workcraft",
            db: mockDb,
            androidPublisher: mockPublisher,
          });
        },
        (err) => err.code === "permission-denied",
    );

    // Verify User A remains owner and User B was not granted entitlement
    const ownerDataAfter = mockDb._store.get(`subscriptionTokenOwners/${tokenHash}`);
    assert.equal(ownerDataAfter.uid, "user_A");
    assert.equal(mockDb._store.get("subscriptionEntitlements/user_B"), undefined);
  });

  // Test 9: Concurrent Claims of the Same Token
  test("9. Concurrent claims of same token aborts with transaction conflict", async () => {
    const mockDb = createMockFirestore();
    const token = "token_concurrent_12345";
    const tokenHash = hashToken(token);

    const mockPublisher = {
      purchases: {
        subscriptionsv2: {
          get: async () => ({
            data: {
              subscriptionState: "SUBSCRIPTION_STATE_ACTIVE",
              acknowledgementState: "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED",
              lineItems: [{productId: "workcraft_pro_monthly", expiryTime: futureExpiry}],
            },
          }),
        },
        subscriptions: {acknowledge: async () => ({})},
      },
    };

    // Simulate collision: While User 2 is running transaction, User 1 commits ownership
    const originalRunTransaction = mockDb.runTransaction;
    let user1Finished = false;

    mockDb.runTransaction = async function(fn) {
      if (!user1Finished) {
        user1Finished = true;
        // User 1 commits ownership directly into store
        mockDb._store.set(`subscriptionTokenOwners/${tokenHash}`, {uid: "user_first"});
      }
      return originalRunTransaction.call(this, fn);
    };

    await assert.rejects(
        async () => {
          await processSubscriptionVerification({
            uid: "user_second",
            purchaseToken: token,
            productId: "workcraft_pro_monthly",
            packageName: "com.habibullahsalmani.workcraft",
            db: mockDb,
            androidPublisher: mockPublisher,
          });
        },
        (err) => err.code === "permission-denied",
    );
  });

  // Test 10: Anti-Downgrade: Active Yearly + Expired Monthly
  test("10. Active yearly entitlement is NOT downgraded by an expired monthly token", () => {
    const yearlyPurchase = {
      tokenHash: "token_yearly_hash",
      productId: "workcraft_pro_yearly",
      subscriptionState: "SUBSCRIPTION_STATE_ACTIVE",
      expiryTime: yearlyExpiry,
      isPro: true,
      autoRenewEnabled: true,
    };

    const expiredMonthlyPurchase = {
      tokenHash: "token_monthly_expired_hash",
      productId: "workcraft_pro_monthly",
      subscriptionState: "SUBSCRIPTION_STATE_EXPIRED",
      expiryTime: pastExpiry,
      isPro: false,
      autoRenewEnabled: false,
    };

    const derived = deriveActiveEntitlement([yearlyPurchase, expiredMonthlyPurchase]);
    assert.equal(derived.isPro, true);
    assert.equal(derived.productId, "workcraft_pro_yearly");
    assert.equal(derived.expiryTime, yearlyExpiry);
  });

  // Test 11: Subscription Upgrade with Linked Purchase Token
  test("11. Subscription upgrade links to previous token and marks old purchase replaced", async () => {
    const mockDb = createMockFirestore();
    const oldMonthlyToken = "token_old_monthly_123456";
    const newYearlyToken = "token_new_yearly_987654";
    const oldTokenHash = hashToken(oldMonthlyToken);

    // Initial monthly verification
    const mockMonthlyPublisher = {
      purchases: {
        subscriptionsv2: {
          get: async () => ({
            data: {
              subscriptionState: "SUBSCRIPTION_STATE_ACTIVE",
              acknowledgementState: "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED",
              lineItems: [{productId: "workcraft_pro_monthly", expiryTime: futureExpiry}],
            },
          }),
        },
        subscriptions: {acknowledge: async () => ({})},
      },
    };

    await processSubscriptionVerification({
      uid: "user_upgrade",
      purchaseToken: oldMonthlyToken,
      productId: "workcraft_pro_monthly",
      packageName: "com.habibullahsalmani.workcraft",
      db: mockDb,
      androidPublisher: mockMonthlyPublisher,
    });

    const entBefore = mockDb._store.get("subscriptionEntitlements/user_upgrade");
    assert.equal(entBefore.productId, "workcraft_pro_monthly");

    // Upgrade to yearly with linkedPurchaseToken
    const mockYearlyPublisher = {
      purchases: {
        subscriptionsv2: {
          get: async () => ({
            data: {
              subscriptionState: "SUBSCRIPTION_STATE_ACTIVE",
              acknowledgementState: "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED",
              linkedPurchaseToken: oldMonthlyToken,
              lineItems: [{productId: "workcraft_pro_yearly", expiryTime: yearlyExpiry}],
            },
          }),
        },
        subscriptions: {acknowledge: async () => ({})},
      },
    };

    const resUpgrade = await processSubscriptionVerification({
      uid: "user_upgrade",
      purchaseToken: newYearlyToken,
      productId: "workcraft_pro_yearly",
      packageName: "com.habibullahsalmani.workcraft",
      db: mockDb,
      androidPublisher: mockYearlyPublisher,
    });

    assert.equal(resUpgrade.isPro, true);
    assert.equal(resUpgrade.productId, "workcraft_pro_yearly");

    // Old monthly purchase is marked isReplaced
    const oldPurchase = mockDb._store.get(`userSubscriptions/user_upgrade/purchases/${oldTokenHash}`);
    assert.equal(oldPurchase.isReplaced, true);
  });

  // Test 12: Replaced Yearly Plan Does NOT Grant Pro
  test("12. A replaced yearly plan is ignored by deriveActiveEntitlement", () => {
    const replacedYearly = {
      tokenHash: "token_yearly_replaced",
      productId: "workcraft_pro_yearly",
      subscriptionState: "SUBSCRIPTION_STATE_ACTIVE",
      expiryTime: yearlyExpiry,
      isPro: true,
      isReplaced: true,
    };

    const derived = deriveActiveEntitlement([replacedYearly]);
    assert.equal(derived.isPro, false);
  });

  // Test 13: Acknowledgement Succeeds But Firestore Transaction Fails, Subsequent Retry Recovers
  test("13. Acknowledgement succeeds but Firestore fails; retry completes without re-acknowledgement", async () => {
    const mockDb = createMockFirestore();
    const token = "token_ack_test_12345";
    let ackCalls = 0;
    let simulateFirestoreFailure = true;

    const mockPublisher = {
      purchases: {
        subscriptionsv2: {
          get: async () => ({
            data: {
              subscriptionState: "SUBSCRIPTION_STATE_ACTIVE",
              acknowledgementState: ackCalls > 0 ? "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED" : "ACKNOWLEDGEMENT_STATE_PENDING",
              lineItems: [{productId: "workcraft_pro_monthly", expiryTime: futureExpiry}],
            },
          }),
        },
        subscriptions: {
          acknowledge: async () => {
            ackCalls++;
            return {};
          },
        },
      },
    };

    const originalRunTransaction = mockDb.runTransaction;
    mockDb.runTransaction = async function(fn) {
      if (simulateFirestoreFailure) {
        throw new Error("Simulated Firestore write failure");
      }
      return originalRunTransaction.call(this, fn);
    };

    // First attempt fails at Firestore write
    await assert.rejects(
        async () => {
          await processSubscriptionVerification({
            uid: "user_ack",
            purchaseToken: token,
            productId: "workcraft_pro_monthly",
            packageName: "com.habibullahsalmani.workcraft",
            db: mockDb,
            androidPublisher: mockPublisher,
          });
        },
        (err) => err.message === "Simulated Firestore write failure",
    );

    assert.equal(ackCalls, 1); // Google Play acknowledged

    // Second attempt succeeds; Google Play acknowledge is skipped
    simulateFirestoreFailure = false;
    const resRetry = await processSubscriptionVerification({
      uid: "user_ack",
      purchaseToken: token,
      productId: "workcraft_pro_monthly",
      packageName: "com.habibullahsalmani.workcraft",
      db: mockDb,
      androidPublisher: mockPublisher,
    });

    assert.equal(resRetry.isPro, true);
    assert.equal(ackCalls, 1); // No duplicate acknowledge call made
  });

  // Test 14: Acknowledgement Fails, Retry Recovers
  test("14. Acknowledgement failure flags pending, subsequent retry completes acknowledgement", async () => {
    const mockDb = createMockFirestore();
    const token = "token_retry_ack_12345";
    let attempt = 0;

    const mockPublisher = {
      purchases: {
        subscriptionsv2: {
          get: async () => ({
            data: {
              subscriptionState: "SUBSCRIPTION_STATE_ACTIVE",
              acknowledgementState: attempt > 1 ? "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED" : "ACKNOWLEDGEMENT_STATE_PENDING",
              lineItems: [{productId: "workcraft_pro_monthly", expiryTime: futureExpiry}],
            },
          }),
        },
        subscriptions: {
          acknowledge: async () => {
            attempt++;
            if (attempt === 1) {
              throw new Error("Network timeout during acknowledge");
            }
            return {};
          },
        },
      },
    };

    // First verify attempt: acknowledgement fails
    const res1 = await processSubscriptionVerification({
      uid: "user_retry",
      purchaseToken: token,
      productId: "workcraft_pro_monthly",
      packageName: "com.habibullahsalmani.workcraft",
      db: mockDb,
      androidPublisher: mockPublisher,
    });

    assert.equal(res1.acknowledgementPending, true);

    // Second verify attempt: acknowledgement succeeds
    const res2 = await processSubscriptionVerification({
      uid: "user_retry",
      purchaseToken: token,
      productId: "workcraft_pro_monthly",
      packageName: "com.habibullahsalmani.workcraft",
      db: mockDb,
      androidPublisher: mockPublisher,
    });

    assert.equal(res2.acknowledgementPending, false);
    assert.equal(res2.isPro, true);
  });

  // Test 15: Repeated Verification Calls Are Idempotent
  test("15. Repeated verification calls are idempotent", async () => {
    const mockDb = createMockFirestore();
    const token = "token_idempotent_12345";

    const mockPublisher = {
      purchases: {
        subscriptionsv2: {
          get: async () => ({
            data: {
              subscriptionState: "SUBSCRIPTION_STATE_ACTIVE",
              acknowledgementState: "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED",
              lineItems: [{productId: "workcraft_pro_monthly", expiryTime: futureExpiry}],
            },
          }),
        },
        subscriptions: {acknowledge: async () => ({})},
      },
    };

    const res1 = await processSubscriptionVerification({
      uid: "user_idem",
      purchaseToken: token,
      productId: "workcraft_pro_monthly",
      packageName: "com.habibullahsalmani.workcraft",
      db: mockDb,
      androidPublisher: mockPublisher,
    });

    const res2 = await processSubscriptionVerification({
      uid: "user_idem",
      purchaseToken: token,
      productId: "workcraft_pro_monthly",
      packageName: "com.habibullahsalmani.workcraft",
      db: mockDb,
      androidPublisher: mockPublisher,
    });

    assert.equal(res1.isPro, res2.isPro);
    assert.equal(res1.productId, res2.productId);
    assert.equal(res1.expiryTime, res2.expiryTime);
  });

  // Test 16: Unverified Purchase Token (404)
  test("16. Google Play 404 Not Found never creates Firestore entitlement", async () => {
    const mockDb = createMockFirestore();
    const mockPublisher = {
      purchases: {
        subscriptionsv2: {
          get: async () => {
            const err = new Error("Not Found");
            err.code = 404;
            throw err;
          },
        },
      },
    };

    await assert.rejects(
        async () => {
          await processSubscriptionVerification({
            uid: "user_fake",
            purchaseToken: "invalid_fake_token_12345",
            productId: "workcraft_pro_monthly",
            packageName: "com.habibullahsalmani.workcraft",
            db: mockDb,
            androidPublisher: mockPublisher,
          });
        },
        (err) => err.code === "not-found",
    );

    assert.equal(mockDb._store.get("subscriptionEntitlements/user_fake"), undefined);
  });

  // Test 17: Strict Transaction Read-Before-Write Ordering Validation
  test("17. Verification service strictly follows read-before-write transactional ordering", async () => {
    const mockDb = createMockFirestore();
    const token = "token_ordering_12345";

    const mockPublisher = {
      purchases: {
        subscriptionsv2: {
          get: async () => ({
            data: {
              subscriptionState: "SUBSCRIPTION_STATE_ACTIVE",
              acknowledgementState: "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED",
              linkedPurchaseToken: "token_linked_ord_999",
              lineItems: [{productId: "workcraft_pro_yearly", expiryTime: yearlyExpiry}],
            },
          }),
        },
        subscriptions: {acknowledge: async () => ({})},
      },
    };

    // Pre-populate linked token owner
    mockDb._store.set(`subscriptionTokenOwners/${hashToken("token_linked_ord_999")}`, {uid: "user_ord"});

    // This must succeed without throwing "Firestore transactions require all reads to be executed before all writes"
    const res = await processSubscriptionVerification({
      uid: "user_ord",
      purchaseToken: token,
      productId: "workcraft_pro_yearly",
      packageName: "com.habibullahsalmani.workcraft",
      db: mockDb,
      androidPublisher: mockPublisher,
    });

    assert.equal(res.isPro, true);
  });

  // Test 18: Exported callable functions verification (Canonical name and backward-compatibility alias)
  test("18. Cloud Functions index exports verifySubscriptionPurchase and verifyGooglePlaySubscription alias", () => {
    const index = require("../index");
    assert.equal(typeof index.verifySubscriptionPurchase, "function");
    assert.equal(typeof index.verifyGooglePlaySubscription, "function");
    assert.equal(index.verifyGooglePlaySubscription, index.verifySubscriptionPurchase);
  });

  // Test 19: Full purchase verification updates Firestore entitlement correctly with all required fields
  test("19. Full purchase verification updates Firestore entitlement correctly with all fields", async () => {
    const mockDb = createMockFirestore();
    const token = "token_full_entitlement_update_12345";
    const testUid = "user_entitlement_verified";

    const mockPublisher = {
      purchases: {
        subscriptionsv2: {
          get: async () => ({
            data: {
              subscriptionState: "SUBSCRIPTION_STATE_ACTIVE",
              acknowledgementState: "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED",
              externalAccountIdentifiers: {
                obfuscatedExternalAccountId: getObfuscatedAccountId(testUid),
              },
              lineItems: [{productId: "workcraft_pro_yearly", expiryTime: yearlyExpiry}],
            },
          }),
        },
        subscriptions: {acknowledge: async () => ({})},
      },
    };

    const res = await processSubscriptionVerification({
      uid: testUid,
      purchaseToken: token,
      productId: "workcraft_pro_yearly",
      packageName: "com.habibullahsalmani.workcraft",
      db: mockDb,
      androidPublisher: mockPublisher,
    });

    assert.equal(res.success, true);
    assert.equal(res.isPro, true);
    assert.equal(res.productId, "workcraft_pro_yearly");
    assert.equal(res.subscriptionState, "SUBSCRIPTION_STATE_ACTIVE");

    // Inspect Firestore stored entitlement document
    const entDoc = mockDb._store.get(`subscriptionEntitlements/${testUid}`);
    assert.ok(entDoc, "Entitlement document must exist in Firestore");
    assert.equal(entDoc.uid, testUid);
    assert.equal(entDoc.isPro, true);
    assert.equal(entDoc.productId, "workcraft_pro_yearly");
    assert.equal(entDoc.subscriptionState, "SUBSCRIPTION_STATE_ACTIVE");
    assert.equal(entDoc.expiryTime, yearlyExpiry);
    assert.equal(entDoc.acknowledged, true);
    assert.equal(entDoc.source, "google_play");
    assert.equal(entDoc.activeTokenHash, hashToken(token));
  });

  // Test 20: Invalid purchase token format and unsupported product validation
  test("20. Invalid purchase token format and unsupported products are rejected with invalid-argument", async () => {
    const mockDb = createMockFirestore();
    const mockPublisher = {
      purchases: {
        subscriptionsv2: {get: async () => ({})},
        subscriptions: {acknowledge: async () => ({})},
      },
    };

    // Short token (< 10 chars)
    await assert.rejects(
        async () => {
          await processSubscriptionVerification({
            uid: "user_invalid_token",
            purchaseToken: "short",
            productId: "workcraft_pro_monthly",
            packageName: "com.habibullahsalmani.workcraft",
            db: mockDb,
            androidPublisher: mockPublisher,
          });
        },
        (err) => err.code === "invalid-argument",
    );

    // Unsupported product ID
    await assert.rejects(
        async () => {
          await processSubscriptionVerification({
            uid: "user_invalid_token",
            purchaseToken: "valid_length_token_12345",
            productId: "unsupported_fake_tier",
            packageName: "com.habibullahsalmani.workcraft",
            db: mockDb,
            androidPublisher: mockPublisher,
          });
        },
        (err) => err.code === "invalid-argument",
    );
  });
});


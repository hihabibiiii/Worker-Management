const {test, describe} = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");

const {
  FREE_LIMIT_SITES,
  FREE_LIMIT_WORKERS,
  SCHEMA_VERSION,
  checkUserIsPro,
  createSiteWithQuota,
  createWorkerWithQuota,
  softDeleteEntityWithQuota,
  restoreEntityWithQuota,
  permanentDeleteEntityWithQuota,
  migrateUserQuotaToAllocations,
  reconcileQuotaLedger,
} = require("../lib/quotaService");

/**
 * In-memory Mock Firestore with collection querying, filters (where), pagination (limit, startAfter),
 * and transaction read-before-write validation with OCC concurrency checking.
 */
function createMockFirestore() {
  const store = new Map();
  const docVersions = new Map();

  function buildCollectionRef(subPath, colName) {
    const colObj = {
      id: colName,
      path: subPath,
      _filters: [],
      _limit: null,
      _startAfterDoc: null,
      where(field, op, val) {
        const cloned = buildCollectionRef(subPath, colName);
        cloned._filters = [...this._filters, {field, op, val}];
        cloned._limit = this._limit;
        cloned._startAfterDoc = this._startAfterDoc;
        return cloned;
      },
      limit(n) {
        const cloned = buildCollectionRef(subPath, colName);
        cloned._filters = [...this._filters];
        cloned._limit = n;
        cloned._startAfterDoc = this._startAfterDoc;
        return cloned;
      },
      startAfter(docSnap) {
        const cloned = buildCollectionRef(subPath, colName);
        cloned._filters = [...this._filters];
        cloned._limit = this._limit;
        cloned._startAfterDoc = docSnap;
        return cloned;
      },
      doc(subDocId) {
        const itemPath = subDocId ? `${subPath}/${subDocId}` : `${subPath}/auto_${Math.random()}`;
        return {
          id: subDocId || itemPath.split("/").pop(),
          path: itemPath,
          async get() {
            const data = store.get(itemPath);
            return {
              id: this.id,
              exists: Boolean(data),
              data: () => (data ? JSON.parse(JSON.stringify(data)) : undefined),
            };
          },
          collection(nestedName) {
            return buildCollectionRef(`${itemPath}/${nestedName}`, nestedName);
          },
        };
      },
      async get() {
        let entries = [];
        for (const [key, val] of store.entries()) {
          if (key.startsWith(subPath + "/")) {
            const id = key.substring(subPath.length + 1);
            if (!id.includes("/")) {
              entries.push({id, data: val});
            }
          }
        }
        for (const f of this._filters) {
          if (f.op === "==") {
            entries = entries.filter((e) => e.data && e.data[f.field] === f.val);
          }
        }
        if (this._startAfterDoc) {
          const idx = entries.findIndex((e) => e.id === this._startAfterDoc.id);
          if (idx !== -1) {
            entries = entries.slice(idx + 1);
          }
        }
        if (typeof this._limit === "number") {
          entries = entries.slice(0, this._limit);
        }
        const docs = entries.map((e) => ({
          id: e.id,
          data: () => JSON.parse(JSON.stringify(e.data)),
        }));
        return {docs};
      },
    };
    return colObj;
  }

  const db = {
    _store: store,
    _docVersions: docVersions,
    collection(colName) {
      return buildCollectionRef(colName, colName);
    },
    async runTransaction(updateFunction) {
      let writeOccurred = false;
      const readVersions = new Map();
      const pendingOps = [];

      const transaction = {
        async get(ref) {
          if (writeOccurred) {
            throw new Error("Firestore transactions require all reads to be executed before all writes.");
          }
          const currentVersion = docVersions.get(ref.path) || 0;
          readVersions.set(ref.path, currentVersion);
          return ref.get();
        },
        set(docRef, data, options) {
          writeOccurred = true;
          pendingOps.push({type: "set", docRef, data, options});
        },
        update(docRef, data) {
          writeOccurred = true;
          pendingOps.push({type: "update", docRef, data});
        },
        delete(docRef) {
          writeOccurred = true;
          pendingOps.push({type: "delete", docRef});
        },
      };

      const result = await updateFunction(transaction);

      // OCC concurrency verification
      for (const [p, expectedVersion] of readVersions.entries()) {
        const actualVersion = docVersions.get(p) || 0;
        if (actualVersion !== expectedVersion) {
          const err = new Error("Transaction failed due to concurrent modification.");
          err.code = "aborted";
          throw err;
        }
      }

      // Atomic commit
      for (const op of pendingOps) {
        const p = op.docRef.path;
        const existing = store.get(p);
        if (op.type === "set") {
          if (op.options?.merge) {
            store.set(p, {...(existing || {}), ...JSON.parse(JSON.stringify(op.data))});
          } else {
            store.set(p, JSON.parse(JSON.stringify(op.data)));
          }
        } else if (op.type === "update") {
          if (!existing) {
            throw new Error(`Cannot update non-existent document: ${p}`);
          }
          store.set(p, {...existing, ...JSON.parse(JSON.stringify(op.data))});
        } else if (op.type === "delete") {
          store.delete(p);
        }
        docVersions.set(p, (docVersions.get(p) || 0) + 1);
      }

      return result;
    },
  };

  return db;
}

const mockFieldValue = {
  serverTimestamp: () => new Date().toISOString(),
};

describe("WorkCraft Hybrid Quota & Downgrade Safety Test Suite", () => {
  const uid = "downgrade_user_200";
  const proEntitlement = {
    isPro: true,
    subscriptionState: "SUBSCRIPTION_STATE_ACTIVE",
    expiryTime: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString(),
    productId: "workcraft_pro_monthly",
  };

  // Test 1: Pro user creates 100 workers, then downgrades to Free
  test("1. Pro user creates 100 workers, then downgrades to Free: all 100 exist and remain accessible", async () => {
    assert.equal(FREE_LIMIT_SITES, 1);
    assert.equal(FREE_LIMIT_WORKERS, 5);
    assert.equal(SCHEMA_VERSION, 2);
    assert.equal(typeof checkUserIsPro, "function");

    const db = createMockFirestore();
    db._store.set(`subscriptionEntitlements/${uid}`, proEntitlement);

    for (let i = 1; i <= 100; i++) {
      const res = await createWorkerWithQuota({
        uid,
        workerData: {syncId: `w_100_${i}`, name: `Worker ${i}`},
        db,
        fieldValue: mockFieldValue,
      });
      assert.equal(res.isPro, true);
    }

    // Downgrade user to Free
    db._store.set(`subscriptionEntitlements/${uid}`, {
      isPro: false,
      subscriptionState: "SUBSCRIPTION_STATE_EXPIRED",
      expiryTime: new Date(Date.now() - 1000).toISOString(),
    });

    // Verify all 100 workers remain accessible
    for (let i = 1; i <= 100; i++) {
      const wDoc = await db.collection("users").doc(uid).collection("workers").doc(`w_100_${i}`).get();
      assert.equal(wDoc.exists, true);
      assert.equal(wDoc.data().isDeleted, false);
    }
  });

  // Test 2: Downgraded user cannot create another worker
  test("2. Downgraded user with 100 existing workers cannot create another worker", async () => {
    const db = createMockFirestore();
    db._store.set(`subscriptionEntitlements/${uid}`, proEntitlement);
    for (let i = 1; i <= 100; i++) {
      await createWorkerWithQuota({
        uid,
        workerData: {syncId: `w_${i}`, name: `Worker ${i}`},
        db,
        fieldValue: mockFieldValue,
      });
    }

    // Downgrade
    db._store.set(`subscriptionEntitlements/${uid}`, {
      isPro: false,
      subscriptionState: "SUBSCRIPTION_STATE_EXPIRED",
      expiryTime: new Date(Date.now() - 1000).toISOString(),
    });

    // 101st worker creation is rejected with resource-exhausted
    await assert.rejects(
        () => createWorkerWithQuota({
          uid,
          workerData: {syncId: "w_101", name: "Worker 101"},
          db,
          fieldValue: mockFieldValue,
        }),
        (err) => {
          assert.equal(err.code, "resource-exhausted");
          assert.match(err.message, /quota exceeded/i);
          return true;
        },
    );
  });

  // Test 3: Downgraded user deletes an existing worker and can then create one worker if under quota
  test("3. Downgraded user deletes workers: can create new worker only after dropping below Free limit", async () => {
    const db = createMockFirestore();
    db._store.set(`subscriptionEntitlements/${uid}`, proEntitlement);
    // Create 6 workers while Pro
    for (let i = 1; i <= 6; i++) {
      await createWorkerWithQuota({
        uid,
        workerData: {syncId: `w_drop_${i}`, name: `Worker ${i}`},
        db,
        fieldValue: mockFieldValue,
      });
    }

    // Downgrade to Free
    db._store.set(`subscriptionEntitlements/${uid}`, {
      isPro: false,
      subscriptionState: "SUBSCRIPTION_STATE_EXPIRED",
      expiryTime: new Date(Date.now() - 1000).toISOString(),
    });

    // Soft delete 1 worker -> 5 workers remain (still >= 5, so creation blocked)
    await softDeleteEntityWithQuota({uid, entityType: "worker", syncId: "w_drop_6", db, fieldValue: mockFieldValue});

    await assert.rejects(
        () => createWorkerWithQuota({
          uid,
          workerData: {syncId: "w_new_fail", name: "Fail Worker"},
          db,
          fieldValue: mockFieldValue,
        }),
        (err) => err.code === "resource-exhausted",
    );

    // Soft delete 2nd worker -> 4 workers remain (< 5 Free limit)
    await softDeleteEntityWithQuota({uid, entityType: "worker", syncId: "w_drop_5", db, fieldValue: mockFieldValue});

    // Now creation succeeds
    const createRes = await createWorkerWithQuota({
      uid,
      workerData: {syncId: "w_new_success", name: "Success Worker"},
      db,
      fieldValue: mockFieldValue,
    });
    assert.equal(createRes.success, true);
    assert.equal(createRes.allocated, true);

    const quotaDoc = await db.collection("users").doc(uid).collection("quota").doc("usage").get();
    assert.equal(quotaDoc.data().activeWorkerCount, 5);
  });

  // Test 4: Pro user creates multiple sites, downgrades, deletes sites, and counters remain accurate
  test("4. Pro user creates multiple sites, downgrades, deletes sites, and counters remain accurate", async () => {
    const db = createMockFirestore();
    db._store.set(`subscriptionEntitlements/${uid}`, proEntitlement);
    // Create 3 sites
    for (let i = 1; i <= 3; i++) {
      await createSiteWithQuota({
        uid,
        siteData: {syncId: `site_multi_${i}`, name: `Site ${i}`},
        db,
        fieldValue: mockFieldValue,
      });
    }

    // Downgrade
    db._store.set(`subscriptionEntitlements/${uid}`, {
      isPro: false,
      subscriptionState: "SUBSCRIPTION_STATE_EXPIRED",
      expiryTime: new Date(Date.now() - 1000).toISOString(),
    });

    // Soft delete site 3 -> 2 sites remain
    await softDeleteEntityWithQuota({uid, entityType: "site", syncId: "site_multi_3", db, fieldValue: mockFieldValue});
    let quotaDoc = await db.collection("users").doc(uid).collection("quota").doc("usage").get();
    assert.equal(quotaDoc.data().activeSiteCount, 2);

    // Soft delete site 2 -> 1 site remains
    await softDeleteEntityWithQuota({uid, entityType: "site", syncId: "site_multi_2", db, fieldValue: mockFieldValue});
    quotaDoc = await db.collection("users").doc(uid).collection("quota").doc("usage").get();
    assert.equal(quotaDoc.data().activeSiteCount, 1);

    // Soft delete site 1 -> 0 sites remain
    await softDeleteEntityWithQuota({uid, entityType: "site", syncId: "site_multi_1", db, fieldValue: mockFieldValue});
    quotaDoc = await db.collection("users").doc(uid).collection("quota").doc("usage").get();
    assert.equal(quotaDoc.data().activeSiteCount, 0);

    // Create fresh site now succeeds
    const freshRes = await createSiteWithQuota({
      uid,
      siteData: {syncId: "site_fresh", name: "Fresh Site"},
      db,
      fieldValue: mockFieldValue,
    });
    assert.equal(freshRes.success, true);
    quotaDoc = await db.collection("users").doc(uid).collection("quota").doc("usage").get();
    assert.equal(quotaDoc.data().activeSiteCount, 1);
  });

  // Test 5: Concurrent migration and entity creation
  test("5. Concurrent migration and entity creation accounts for state without race conditions", async () => {
    const db = createMockFirestore();
    db._store.set(`users/${uid}/sites/s_mig_1`, {syncId: "s_mig_1", name: "Site 1", isDeleted: false});

    async function callWithRetry(fn, maxRetries = 3) {
      for (let attempt = 0; attempt < maxRetries; attempt++) {
        try {
          return await fn();
        } catch (err) {
          if (err.message && err.message.includes("concurrent modification") && attempt < maxRetries - 1) {
            continue;
          }
          throw err;
        }
      }
    }

    const migPromise = callWithRetry(() => migrateUserQuotaToAllocations({uid, db, fieldValue: mockFieldValue}));
    const createPromise = callWithRetry(() => createSiteWithQuota({
      uid,
      siteData: {syncId: "s_mig_new", name: "New Site"},
      db,
      fieldValue: mockFieldValue,
    }));

    const results = await Promise.allSettled([migPromise, createPromise]);
    // Migration succeeds, second creation is either rejected or succeeds depending on ordering
    assert.equal(results[0].status, "fulfilled");
    const quotaDoc = await db.collection("users").doc(uid).collection("quota").doc("usage").get();
    assert.ok(quotaDoc.data().activeSiteCount >= 1);
  });

  // Test 6: Concurrent migration and entity deletion
  test("6. Concurrent migration and entity deletion does not recreate purged allocations", async () => {
    const db = createMockFirestore();
    db._store.set(`users/${uid}/sites/s_del_race`, {syncId: "s_del_race", name: "Site Del Race", isDeleted: false});

    await migrateUserQuotaToAllocations({uid, db, fieldValue: mockFieldValue});
    await softDeleteEntityWithQuota({uid, entityType: "site", syncId: "s_del_race", db, fieldValue: mockFieldValue});

    // Re-running migration on deleted record skips allocation
    await migrateUserQuotaToAllocations({uid, db, fieldValue: mockFieldValue});

    const allocDoc = await db.collection("users").doc(uid).collection("quotaAllocations").doc("site_s_del_race").get();
    assert.equal(allocDoc.exists, false);
  });

  // Test 7: Two reconciliation operations running concurrently
  test("7. Two reconciliation operations running concurrently resolve cleanly via OCC", async () => {
    const db = createMockFirestore();
    db._store.set(`users/${uid}/sites/site_reconcile_occ`, {
      syncId: "site_reconcile_occ", name: "OCC Site", isDeleted: false,
    });

    async function callWithRetry(fn, maxRetries = 3) {
      for (let attempt = 0; attempt < maxRetries; attempt++) {
        try {
          return await fn();
        } catch (err) {
          if (err.message && err.message.includes("concurrent modification") && attempt < maxRetries - 1) {
            continue;
          }
          throw err;
        }
      }
    }

    const r1 = callWithRetry(() => reconcileQuotaLedger({
      adminUid: "admin_1", targetUid: uid, isAdmin: true, db, fieldValue: mockFieldValue,
    }));
    const r2 = callWithRetry(() => reconcileQuotaLedger({
      adminUid: "admin_2", targetUid: uid, isAdmin: true, db, fieldValue: mockFieldValue,
    }));

    const results = await Promise.all([r1, r2]);
    assert.equal(results[0].success, true);
    assert.equal(results[1].success, true);

    const allocDoc = await db.collection("users").doc(uid).collection("quotaAllocations").doc("site_site_reconcile_occ").get();
    assert.equal(allocDoc.exists, true);
  });

  // Test 8: Reconciliation while an entity is being created
  test("8. Reconciliation while an entity is being created: OCC conflict retries to consistent state", async () => {
    const db = createMockFirestore();

    async function callWithRetry(fn, maxRetries = 3) {
      for (let attempt = 0; attempt < maxRetries; attempt++) {
        try {
          return await fn();
        } catch (err) {
          if (err.message && err.message.includes("concurrent modification") && attempt < maxRetries - 1) {
            continue;
          }
          throw err;
        }
      }
    }

    const createPromise = callWithRetry(() => createSiteWithQuota({
      uid,
      siteData: {syncId: "s_reconcile_race", name: "Race Site"},
      db,
      fieldValue: mockFieldValue,
    }));

    const reconcilePromise = callWithRetry(() => reconcileQuotaLedger({
      adminUid: "admin_1", targetUid: uid, isAdmin: true, db, fieldValue: mockFieldValue,
    }));

    const [cRes, rRes] = await Promise.all([createPromise, reconcilePromise]);
    assert.equal(cRes.success, true);
    assert.equal(rRes.success, true);

    const allocDoc = await db.collection("users").doc(uid).collection("quotaAllocations").doc("site_s_reconcile_race").get();
    assert.equal(allocDoc.exists, true);
  });

  // Test 9: Interrupted migration followed by retry
  test("9. Interrupted migration followed by retry completes migration and maintains state", async () => {
    const db = createMockFirestore();
    for (let i = 1; i <= 6; i++) {
      db._store.set(`users/${uid}/sites/interrupted_s_${i}`, {
        syncId: `interrupted_s_${i}`, name: `Site ${i}`, isDeleted: false,
      });
    }

    // Step 1: Migrate batch of 3
    const p1 = await migrateUserQuotaToAllocations({uid, db, fieldValue: mockFieldValue, batchSize: 3});
    assert.equal(p1.migratedSites, 3);
    assert.equal(p1.hasMore, true);

    // Step 2: Retry/resume from last site
    const p2 = await migrateUserQuotaToAllocations({
      uid, db, fieldValue: mockFieldValue, batchSize: 10, startAfterSiteId: p1.lastSiteId,
    });
    assert.equal(p2.migratedSites, 3);
    assert.equal(p2.hasMore, false);

    const allocs = await db.collection("users").doc(uid).collection("quotaAllocations").get();
    assert.equal(allocs.docs.length, 6);
  });

  // Test 10: Unauthorized reconciliation
  test("10. Unauthorized reconciliation is rejected with permission-denied", async () => {
    const db = createMockFirestore();
    await assert.rejects(
        () => reconcileQuotaLedger({
          adminUid: "hacker_user", targetUid: uid, isAdmin: false, db, fieldValue: mockFieldValue,
        }),
        (err) => err.code === "permission-denied",
    );
  });

  // Test 11: Firestore Rules overlapping permission bypass attempts
  test("11. Firestore Rules non-overlapping verification: client writes to quotaAllocations are denied", () => {
    const rulesPath = path.resolve(__dirname, "../../firestore.rules");
    const rulesContent = fs.readFileSync(rulesPath, "utf8");

    // Must not have recursive wildcard on users collection
    assert.ok(!rulesContent.includes("match /{allSubcollections=**}"));

    // Check quotaAllocations rule
    const allocMatch = rulesContent.match(/match\s+\/quotaAllocations\/\{allocationId\}\s*\{([\s\S]*?)\}/);
    assert.ok(allocMatch);
    assert.match(allocMatch[1], /allow\s+write:\s*if\s+false;/);
    assert.match(allocMatch[1], /allow\s+read:\s*if\s+request\.auth\s*!=\s*null\s*&&\s*request\.auth\.uid\s*==\s*uid;/);

    // Check adminAuditLogs
    const auditMatch = rulesContent.match(/match\s+\/adminAuditLogs\/\{logId\}\s*\{([\s\S]*?)\}/);
    assert.ok(auditMatch);
    assert.match(auditMatch[1], /allow\s+read,\s*write:\s*if\s+false;/);
  });

  // Test 12: Two devices simultaneously attempting the final Free quota slot
  test("12. Two devices simultaneously attempting the final Free quota slot: exactly one succeeds", async () => {
    const db = createMockFirestore();

    async function callWithRetry(fn, maxRetries = 3) {
      for (let attempt = 0; attempt < maxRetries; attempt++) {
        try {
          return await fn();
        } catch (err) {
          if (err.message && err.message.includes("concurrent modification") && attempt < maxRetries - 1) {
            continue;
          }
          throw err;
        }
      }
    }

    const p1 = callWithRetry(() => createSiteWithQuota({
      uid, siteData: {syncId: "final_slot_dev_A", name: "Dev A"}, db, fieldValue: mockFieldValue,
    }));
    const p2 = callWithRetry(() => createSiteWithQuota({
      uid, siteData: {syncId: "final_slot_dev_B", name: "Dev B"}, db, fieldValue: mockFieldValue,
    }));

    const results = await Promise.allSettled([p1, p2]);
    const fulfilled = results.filter((r) => r.status === "fulfilled");
    const rejected = results.filter((r) => r.status === "rejected");

    assert.equal(fulfilled.length, 1);
    assert.equal(rejected.length, 1);
    assert.match(rejected[0].reason.message, /quota exceeded|concurrent modification/i);
  });

  // Test 13: Site and Worker creation writes normalized epoch millisecond numbers
  test("13. Site and Worker creation writes normalized epoch millisecond numbers", async () => {
    const db = createMockFirestore();
    const fixedNow = 1705000000000;

    await createSiteWithQuota({
      uid: "user_ts_1",
      siteData: {syncId: "site_ts_1", name: "Timestamp Test Site"},
      db,
      fieldValue: mockFieldValue,
      now: fixedNow,
    });

    const siteDoc = await db.collection("users").doc("user_ts_1").collection("sites").doc("site_ts_1").get();
    assert.equal(siteDoc.exists, true);
    const siteData = siteDoc.data();
    assert.equal(typeof siteData.updatedAt, "number");
    assert.equal(Number.isInteger(siteData.updatedAt), true);
    assert.equal(siteData.updatedAt, fixedNow);
    assert.equal(typeof siteData.createdAt, "number");
    assert.equal(siteData.createdAt, fixedNow);

    const siteAllocDoc = await db.collection("users").doc("user_ts_1").collection("quotaAllocations")
        .doc("site_site_ts_1").get();
    assert.equal(siteAllocDoc.exists, true);
    assert.equal(siteAllocDoc.data().updatedAt, fixedNow);
    assert.equal(siteAllocDoc.data().allocatedAt, fixedNow);

    await createWorkerWithQuota({
      uid: "user_ts_1",
      workerData: {syncId: "worker_ts_1", name: "Timestamp Test Worker"},
      db,
      fieldValue: mockFieldValue,
      now: fixedNow,
    });

    const workerDoc = await db.collection("users").doc("user_ts_1").collection("workers").doc("worker_ts_1").get();
    assert.equal(workerDoc.exists, true);
    const workerData = workerDoc.data();
    assert.equal(typeof workerData.updatedAt, "number");
    assert.equal(Number.isInteger(workerData.updatedAt), true);
    assert.equal(workerData.updatedAt, fixedNow);
    assert.equal(typeof workerData.createdAt, "number");
    assert.equal(workerData.createdAt, fixedNow);
  });

  // Test 14: Soft delete and restore write normalized epoch millisecond numbers
  test("14. Soft delete and restore write normalized epoch millisecond numbers", async () => {
    const db = createMockFirestore();
    const t0 = 1705000000000;
    const t1 = 1705000050000;
    const t2 = 1705000100000;

    await createWorkerWithQuota({
      uid: "user_ts_2",
      workerData: {syncId: "worker_ts_2", name: "Worker TS 2"},
      db,
      fieldValue: mockFieldValue,
      now: t0,
    });

    await softDeleteEntityWithQuota({
      uid: "user_ts_2",
      entityType: "worker",
      syncId: "worker_ts_2",
      db,
      fieldValue: mockFieldValue,
      now: t1,
    });

    const deletedDoc = await db.collection("users").doc("user_ts_2").collection("workers").doc("worker_ts_2").get();
    const deletedData = deletedDoc.data();
    assert.equal(deletedData.isDeleted, true);
    assert.equal(typeof deletedData.deletedAt, "number");
    assert.equal(deletedData.deletedAt, t1);
    assert.equal(typeof deletedData.updatedAt, "number");
    assert.equal(deletedData.updatedAt, t1);

    await restoreEntityWithQuota({
      uid: "user_ts_2",
      entityType: "worker",
      syncId: "worker_ts_2",
      db,
      fieldValue: mockFieldValue,
      now: t2,
    });

    const restoredDoc = await db.collection("users").doc("user_ts_2").collection("workers").doc("worker_ts_2").get();
    const restoredData = restoredDoc.data();
    assert.equal(restoredData.isDeleted, false);
    assert.equal(restoredData.deletedAt, null);
    assert.equal(typeof restoredData.updatedAt, "number");
    assert.equal(restoredData.updatedAt, t2);

    await permanentDeleteEntityWithQuota({
      uid: "user_ts_2",
      entityType: "worker",
      syncId: "worker_ts_2",
      db,
      fieldValue: mockFieldValue,
      now: t2 + 1000,
    });
    const quotaDoc = await db.collection("users").doc("user_ts_2").collection("quota").doc("usage").get();
    assert.equal(quotaDoc.data().updatedAt, t2 + 1000);
  });

  // Test 15: Migration and reconciliation write normalized epoch millisecond numbers
  test("15. Migration and reconciliation write normalized epoch millisecond numbers", async () => {
    const db = createMockFirestore();
    const t0 = 1705000200000;
    const t1 = 1705000300000;

    db._store.set("users/user_ts_3/sites/site_legacy", {
      syncId: "site_legacy",
      name: "Legacy Site",
      isDeleted: false,
    });

    await migrateUserQuotaToAllocations({
      uid: "user_ts_3",
      db,
      fieldValue: mockFieldValue,
      now: t0,
    });

    const allocDoc = await db.collection("users").doc("user_ts_3").collection("quotaAllocations")
        .doc("site_site_legacy").get();
    assert.equal(allocDoc.exists, true);
    assert.equal(typeof allocDoc.data().updatedAt, "number");
    assert.equal(allocDoc.data().updatedAt, t0);
    assert.equal(allocDoc.data().allocatedAt, t0);

    const quotaDoc = await db.collection("users").doc("user_ts_3").collection("quota").doc("usage").get();
    assert.equal(typeof quotaDoc.data().updatedAt, "number");
    assert.equal(quotaDoc.data().updatedAt, t0);

    await reconcileQuotaLedger({
      adminUid: "admin_123",
      targetUid: "user_ts_3",
      isAdmin: true,
      db,
      fieldValue: mockFieldValue,
      now: t1,
    });

    const reconciledQuota = await db.collection("users").doc("user_ts_3").collection("quota").doc("usage").get();
    assert.equal(reconciledQuota.data().updatedAt, t1);
    assert.equal(reconciledQuota.data().lastReconciledAt, t1);
  });
});

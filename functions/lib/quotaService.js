const {HttpsError} = require("firebase-functions/v2/https");

const FREE_LIMIT_SITES = 1;
const FREE_LIMIT_WORKERS = 5;
const SCHEMA_VERSION = 2;

/**
 * Checks whether the user is actively entitled to Pro.
 * Evaluates server-verified entitlement record in /subscriptionEntitlements/{uid}.
 * Never trusts any client-supplied flag.
 * @param {object|null} entitlementData
 * @param {number} [now=Date.now()]
 * @return {boolean}
 */
function checkUserIsPro(entitlementData, now = Date.now()) {
  if (!entitlementData || !entitlementData.isPro) {
    return false;
  }
  const expiryMillis = entitlementData.expiryMillis ||
    (entitlementData.expiryTime ? Date.parse(entitlementData.expiryTime) : 0);

  if (!Number.isFinite(expiryMillis) || expiryMillis <= now) {
    return false;
  }

  const state = entitlementData.subscriptionState;
  const isStateActive =
    state === "SUBSCRIPTION_STATE_ACTIVE" ||
    state === "SUBSCRIPTION_STATE_IN_GRACE_PERIOD" ||
    state === "SUBSCRIPTION_STATE_CANCELED";

  return Boolean(isStateActive);
}

/**
 * Validates document ID string format.
 * @param {string} id
 * @return {boolean}
 */
function isValidDocumentId(id) {
  return typeof id === "string" && id.trim().length > 0 && id.length <= 128;
}

/**
 * Resolves active allocations for a specific entity type inside a transaction.
 * For Free or downgraded users, reads all active allocations from /quotaAllocations
 * to guarantee accurate enforcement without trusting stale counters or client flags.
 * @param {object} params
 * @return {Promise<{allocations: Array<object>, activeCount: number}>}
 */
async function resolveActiveAllocations({
  transaction,
  uid,
  entityType,
  db,
}) {
  const allocCol = db.collection("users").doc(uid).collection("quotaAllocations");
  const query = allocCol.where("entityType", "==", entityType);

  const snap = await transaction.get(query);
  const allocations = [];
  if (snap && snap.docs) {
    for (const doc of snap.docs) {
      allocations.push({id: doc.id, ...doc.data()});
    }
  }
  return {allocations, activeCount: allocations.length};
}

/**
 * Creates a Site with atomic hybrid quota enforcement.
 * Free users: Shared counter doc (/quota/usage) and individual allocation doc updated in same transaction.
 * Pro users: Individual allocation doc created without shared counter lock contention.
 */
async function createSiteWithQuota({
  uid,
  siteData,
  db,
  fieldValue,
  now = Date.now(),
}) {
  if (!uid) {
    throw new HttpsError("unauthenticated", "User must be authenticated.");
  }
  if (!siteData || typeof siteData !== "object") {
    throw new HttpsError("invalid-argument", "Missing or invalid site data.");
  }
  const syncId = siteData.syncId;
  if (!isValidDocumentId(syncId)) {
    throw new HttpsError("invalid-argument", "Missing or invalid site syncId.");
  }
  const name = typeof siteData.name === "string" ? siteData.name.trim() : "";
  if (!name) {
    throw new HttpsError("invalid-argument", "Site name cannot be blank.");
  }

  const allocationId = `site_${syncId}`;
  const entitlementRef = db.collection("subscriptionEntitlements").doc(uid);
  const quotaRef = db.collection("users").doc(uid).collection("quota").doc("usage");
  const siteRef = db.collection("users").doc(uid).collection("sites").doc(syncId);
  const allocRef = db.collection("users").doc(uid).collection("quotaAllocations").doc(allocationId);

  const nowMillis = (typeof now === "number" && Number.isFinite(now)) ? now : Date.now();

  return db.runTransaction(async (transaction) => {
    // 1. All transaction reads before any writes
    const [entitlementSnap, quotaSnap, siteSnap, allocSnap] = await Promise.all([
      transaction.get(entitlementRef),
      transaction.get(quotaRef),
      transaction.get(siteRef),
      transaction.get(allocRef),
    ]);

    // 2. Idempotency: If site and allocation are already active, return success without duplicate allocation
    if (siteSnap.exists && allocSnap.exists) {
      const existing = siteSnap.data();
      if (!existing.isDeleted) {
        return {
          success: true,
          syncId,
          isPro: false,
          allocated: false,
          message: "Site already active (idempotent)",
        };
      } else {
        throw new HttpsError(
            "failed-precondition",
            "Site was previously soft-deleted. Use restore operation instead.",
        );
      }
    }

    const isPro = checkUserIsPro(entitlementSnap.exists ? entitlementSnap.data() : null, now);
    const quotaData = quotaSnap.exists ? quotaSnap.data() : {};

    let activeSiteCount = 0;
    let activeSiteIds = [];

    // 3. Quota validation for Free / Downgraded users
    if (!isPro) {
      // Query individual allocations to guarantee exact count even after downgrade from Pro
      const {activeCount, allocations} = await resolveActiveAllocations({
        transaction,
        uid,
        entityType: "site",
        db,
      });

      if (activeCount >= FREE_LIMIT_SITES) {
        throw new HttpsError(
            "resource-exhausted",
            `Free plan quota exceeded: Maximum ${FREE_LIMIT_SITES} active site allowed. ` +
            "Upgrade to Pro for unlimited sites.",
        );
      }
      activeSiteCount = activeCount + 1;
      activeSiteIds = Array.from(new Set([...allocations.map((a) => a.syncId), syncId]));
    }

    const newSiteDoc = {
      ...siteData,
      syncId,
      name,
      isDeleted: false,
      deletedAt: null,
      updatedAt: nowMillis,
      createdAt: (typeof siteData.createdAt === "number" && Number.isFinite(siteData.createdAt)) ?
        siteData.createdAt : nowMillis,
    };

    const newAllocDoc = {
      allocationId,
      entityType: "site",
      syncId,
      allocatedAt: nowMillis,
      updatedAt: nowMillis,
    };

    // 4. Atomic writes
    transaction.set(siteRef, newSiteDoc);
    transaction.set(allocRef, newAllocDoc);

    // Free users update the shared counter to guarantee OCC concurrency conflict on race
    if (!isPro) {
      transaction.set(
          quotaRef,
          {
            activeSiteCount,
            activeWorkerCount: typeof quotaData.activeWorkerCount === "number" ? quotaData.activeWorkerCount : 0,
            activeSiteIds,
            activeWorkerIds: Array.isArray(quotaData.activeWorkerIds) ? quotaData.activeWorkerIds : [],
            schemaVersion: SCHEMA_VERSION,
            initialized: true,
            updatedAt: nowMillis,
          },
          {merge: true},
      );
    }

    return {
      success: true,
      syncId,
      allocationId,
      isPro,
      allocated: true,
    };
  });
}

/**
 * Creates a Worker with atomic hybrid quota enforcement.
 * Free users: Shared counter doc (/quota/usage) and individual allocation doc updated in same transaction.
 * Pro users: Individual allocation doc created without shared counter lock contention.
 */
async function createWorkerWithQuota({
  uid,
  workerData,
  db,
  fieldValue,
  now = Date.now(),
}) {
  if (!uid) {
    throw new HttpsError("unauthenticated", "User must be authenticated.");
  }
  if (!workerData || typeof workerData !== "object") {
    throw new HttpsError("invalid-argument", "Missing or invalid worker data.");
  }
  const syncId = workerData.syncId;
  if (!isValidDocumentId(syncId)) {
    throw new HttpsError("invalid-argument", "Missing or invalid worker syncId.");
  }
  const name = typeof workerData.name === "string" ? workerData.name.trim() : "";
  if (!name) {
    throw new HttpsError("invalid-argument", "Worker name cannot be blank.");
  }

  const allocationId = `worker_${syncId}`;
  const entitlementRef = db.collection("subscriptionEntitlements").doc(uid);
  const quotaRef = db.collection("users").doc(uid).collection("quota").doc("usage");
  const workerRef = db.collection("users").doc(uid).collection("workers").doc(syncId);
  const allocRef = db.collection("users").doc(uid).collection("quotaAllocations").doc(allocationId);

  const nowMillis = (typeof now === "number" && Number.isFinite(now)) ? now : Date.now();

  return db.runTransaction(async (transaction) => {
    // 1. All transaction reads before any writes
    const [entitlementSnap, quotaSnap, workerSnap, allocSnap] = await Promise.all([
      transaction.get(entitlementRef),
      transaction.get(quotaRef),
      transaction.get(workerRef),
      transaction.get(allocRef),
    ]);

    // 2. Idempotency: If worker and allocation are already active, return success without duplicate allocation
    if (workerSnap.exists && allocSnap.exists) {
      const existing = workerSnap.data();
      if (!existing.isDeleted) {
        return {
          success: true,
          syncId,
          isPro: false,
          allocated: false,
          message: "Worker already active (idempotent)",
        };
      } else {
        throw new HttpsError(
            "failed-precondition",
            "Worker was previously soft-deleted. Use restore operation instead.",
        );
      }
    }

    const isPro = checkUserIsPro(entitlementSnap.exists ? entitlementSnap.data() : null, now);
    const quotaData = quotaSnap.exists ? quotaSnap.data() : {};

    let activeWorkerCount = 0;
    let activeWorkerIds = [];

    if (!isPro) {
      const {activeCount, allocations} = await resolveActiveAllocations({
        transaction,
        uid,
        entityType: "worker",
        db,
      });

      if (activeCount >= FREE_LIMIT_WORKERS) {
        throw new HttpsError(
            "resource-exhausted",
            `Free plan quota exceeded: Maximum ${FREE_LIMIT_WORKERS} active workers allowed. ` +
            "Upgrade to Pro for unlimited workers.",
        );
      }
      activeWorkerCount = activeCount + 1;
      activeWorkerIds = Array.from(new Set([...allocations.map((a) => a.syncId), syncId]));
    }

    const newWorkerDoc = {
      ...workerData,
      syncId,
      name,
      isDeleted: false,
      deletedAt: null,
      updatedAt: nowMillis,
      createdAt: (typeof workerData.createdAt === "number" && Number.isFinite(workerData.createdAt)) ?
        workerData.createdAt : nowMillis,
    };

    const newAllocDoc = {
      allocationId,
      entityType: "worker",
      syncId,
      allocatedAt: nowMillis,
      updatedAt: nowMillis,
    };

    // 3. Atomic writes
    transaction.set(workerRef, newWorkerDoc);
    transaction.set(allocRef, newAllocDoc);

    // Free users update the shared counter to guarantee OCC concurrency conflict on race
    if (!isPro) {
      transaction.set(
          quotaRef,
          {
            activeSiteCount: typeof quotaData.activeSiteCount === "number" ? quotaData.activeSiteCount : 0,
            activeWorkerCount,
            activeSiteIds: Array.isArray(quotaData.activeSiteIds) ? quotaData.activeSiteIds : [],
            activeWorkerIds,
            schemaVersion: SCHEMA_VERSION,
            initialized: true,
            updatedAt: nowMillis,
          },
          {merge: true},
      );
    }

    return {
      success: true,
      syncId,
      allocationId,
      isPro,
      allocated: true,
    };
  });
}

/**
 * Soft deletes an entity, atomically removing its allocation record and updating quota.
 */
async function softDeleteEntityWithQuota({
  uid,
  entityType,
  syncId,
  db,
  fieldValue,
  now = Date.now(),
}) {
  if (!uid) {
    throw new HttpsError("unauthenticated", "User must be authenticated.");
  }
  if (entityType !== "site" && entityType !== "worker") {
    throw new HttpsError("invalid-argument", "Invalid entityType: must be 'site' or 'worker'.");
  }
  if (!isValidDocumentId(syncId)) {
    throw new HttpsError("invalid-argument", "Missing or invalid syncId.");
  }

  const collectionName = entityType === "site" ? "sites" : "workers";
  const allocationId = `${entityType}_${syncId}`;

  const entitlementRef = db.collection("subscriptionEntitlements").doc(uid);
  const entityRef = db.collection("users").doc(uid).collection(collectionName).doc(syncId);
  const allocRef = db.collection("users").doc(uid).collection("quotaAllocations").doc(allocationId);
  const quotaRef = db.collection("users").doc(uid).collection("quota").doc("usage");

  const nowMillis = (typeof now === "number" && Number.isFinite(now)) ? now : Date.now();

  return db.runTransaction(async (transaction) => {
    const [entitlementSnap, entitySnap, allocSnap] = await Promise.all([
      transaction.get(entitlementRef),
      transaction.get(entityRef),
      transaction.get(allocRef),
      transaction.get(quotaRef),
    ]);

    if (!entitySnap.exists) {
      throw new HttpsError("not-found", `${entityType} with syncId ${syncId} not found.`);
    }

    const entityData = entitySnap.data();
    if (entityData.isDeleted) {
      return {
        success: true,
        syncId,
        alreadyDeleted: true,
        message: `${entityType} is already soft-deleted.`,
      };
    }

    const isPro = checkUserIsPro(entitlementSnap.exists ? entitlementSnap.data() : null, now);

    let activeAllocData = null;
    if (!isPro) {
      activeAllocData = await resolveActiveAllocations({
        transaction,
        uid,
        entityType,
        db,
      });
    }

    // Update entity
    transaction.update(entityRef, {
      isDeleted: true,
      deletedAt: nowMillis,
      updatedAt: nowMillis,
    });

    // Delete allocation document
    if (allocSnap.exists) {
      transaction.delete(allocRef);
    }

    // Update counter for Free users using exact remaining allocations
    if (!isPro && activeAllocData) {
      const {activeCount, allocations} = activeAllocData;
      const countField = entityType === "site" ? "activeSiteCount" : "activeWorkerCount";
      const idField = entityType === "site" ? "activeSiteIds" : "activeWorkerIds";
      const remainingCount = Math.max(0, activeCount - 1);
      const remainingIds = allocations.map((a) => a.syncId).filter((id) => id !== syncId);

      transaction.set(
          quotaRef,
          {
            [countField]: remainingCount,
            [idField]: remainingIds,
            schemaVersion: SCHEMA_VERSION,
            initialized: true,
            updatedAt: nowMillis,
          },
          {merge: true},
      );
    }

    return {
      success: true,
      syncId,
      softDeleted: true,
    };
  });
}

/**
 * Restores a soft-deleted entity if within quota, atomically recreating its allocation document.
 */
async function restoreEntityWithQuota({
  uid,
  entityType,
  syncId,
  db,
  fieldValue,
  now = Date.now(),
}) {
  if (!uid) {
    throw new HttpsError("unauthenticated", "User must be authenticated.");
  }
  if (entityType !== "site" && entityType !== "worker") {
    throw new HttpsError("invalid-argument", "Invalid entityType: must be 'site' or 'worker'.");
  }
  if (!isValidDocumentId(syncId)) {
    throw new HttpsError("invalid-argument", "Missing or invalid syncId.");
  }

  const collectionName = entityType === "site" ? "sites" : "workers";
  const allocationId = `${entityType}_${syncId}`;
  const quotaLimit = entityType === "site" ? FREE_LIMIT_SITES : FREE_LIMIT_WORKERS;

  const entitlementRef = db.collection("subscriptionEntitlements").doc(uid);
  const entityRef = db.collection("users").doc(uid).collection(collectionName).doc(syncId);
  const allocRef = db.collection("users").doc(uid).collection("quotaAllocations").doc(allocationId);
  const quotaRef = db.collection("users").doc(uid).collection("quota").doc("usage");

  const nowMillis = (typeof now === "number" && Number.isFinite(now)) ? now : Date.now();

  return db.runTransaction(async (transaction) => {
    const [entitlementSnap, entitySnap, allocSnap] = await Promise.all([
      transaction.get(entitlementRef),
      transaction.get(entityRef),
      transaction.get(allocRef),
      transaction.get(quotaRef),
    ]);

    if (!entitySnap.exists) {
      throw new HttpsError("not-found", `${entityType} with syncId ${syncId} not found.`);
    }

    const entityData = entitySnap.data();
    if (!entityData.isDeleted && allocSnap.exists) {
      return {
        success: true,
        syncId,
        alreadyActive: true,
        message: `${entityType} is already active.`,
      };
    }

    const isPro = checkUserIsPro(entitlementSnap.exists ? entitlementSnap.data() : null, now);

    let newActiveCount = 0;
    let newActiveIds = [];

    if (!isPro) {
      const {activeCount, allocations} = await resolveActiveAllocations({
        transaction,
        uid,
        entityType,
        db,
      });

      if (activeCount >= quotaLimit) {
        throw new HttpsError(
            "resource-exhausted",
            `Cannot restore ${entityType}: Free plan quota exceeded ` +
            `(Maximum ${quotaLimit} active ${entityType}s allowed). ` +
            `Upgrade to Pro or delete an active ${entityType} first.`,
        );
      }
      newActiveCount = activeCount + 1;
      newActiveIds = Array.from(new Set([...allocations.map((a) => a.syncId), syncId]));
    }

    // Atomic update
    transaction.update(entityRef, {
      isDeleted: false,
      deletedAt: null,
      updatedAt: nowMillis,
    });

    transaction.set(allocRef, {
      allocationId,
      entityType,
      syncId,
      allocatedAt: nowMillis,
      updatedAt: nowMillis,
    });

    if (!isPro) {
      const countField = entityType === "site" ? "activeSiteCount" : "activeWorkerCount";
      const idField = entityType === "site" ? "activeSiteIds" : "activeWorkerIds";

      transaction.set(
          quotaRef,
          {
            [countField]: newActiveCount,
            [idField]: newActiveIds,
            schemaVersion: SCHEMA_VERSION,
            initialized: true,
            updatedAt: nowMillis,
          },
          {merge: true},
      );
    }

    return {
      success: true,
      syncId,
      restored: true,
    };
  });
}

/**
 * Permanently deletes an entity, ensuring no stale allocation document remains.
 */
async function permanentDeleteEntityWithQuota({
  uid,
  entityType,
  syncId,
  db,
  fieldValue,
  now = Date.now(),
}) {
  if (!uid) {
    throw new HttpsError("unauthenticated", "User must be authenticated.");
  }
  if (entityType !== "site" && entityType !== "worker") {
    throw new HttpsError("invalid-argument", "Invalid entityType: must be 'site' or 'worker'.");
  }
  if (!isValidDocumentId(syncId)) {
    throw new HttpsError("invalid-argument", "Missing or invalid syncId.");
  }

  const collectionName = entityType === "site" ? "sites" : "workers";
  const allocationId = `${entityType}_${syncId}`;

  const entitlementRef = db.collection("subscriptionEntitlements").doc(uid);
  const entityRef = db.collection("users").doc(uid).collection(collectionName).doc(syncId);
  const allocRef = db.collection("users").doc(uid).collection("quotaAllocations").doc(allocationId);
  const quotaRef = db.collection("users").doc(uid).collection("quota").doc("usage");

  const nowMillis = (typeof now === "number" && Number.isFinite(now)) ? now : Date.now();

  return db.runTransaction(async (transaction) => {
    const [entitlementSnap, entitySnap, allocSnap] = await Promise.all([
      transaction.get(entitlementRef),
      transaction.get(entityRef),
      transaction.get(allocRef),
      transaction.get(quotaRef),
    ]);

    const isPro = checkUserIsPro(entitlementSnap.exists ? entitlementSnap.data() : null, now);

    let activeAllocData = null;
    if (!isPro) {
      activeAllocData = await resolveActiveAllocations({
        transaction,
        uid,
        entityType,
        db,
      });
    }

    if (entitySnap.exists) {
      transaction.delete(entityRef);
    }
    if (allocSnap.exists) {
      transaction.delete(allocRef);
    }

    if (!isPro && activeAllocData) {
      const {activeCount, allocations} = activeAllocData;
      const countField = entityType === "site" ? "activeSiteCount" : "activeWorkerCount";
      const idField = entityType === "site" ? "activeSiteIds" : "activeWorkerIds";
      const remainingCount = allocSnap.exists ? Math.max(0, activeCount - 1) : activeCount;
      const remainingIds = allocations.map((a) => a.syncId).filter((id) => id !== syncId);

      transaction.set(
          quotaRef,
          {
            [countField]: remainingCount,
            [idField]: remainingIds,
            schemaVersion: SCHEMA_VERSION,
            initialized: true,
            updatedAt: nowMillis,
          },
          {merge: true},
      );
    }

    return {
      success: true,
      syncId,
      deleted: true,
    };
  });
}

/**
 * Migrates a user's legacy records to individual quota allocation documents.
 * Paginated and idempotent; maintains migration state and never deletes existing Site or Worker records.
 */
async function migrateUserQuotaToAllocations({
  uid,
  db,
  fieldValue,
  batchSize = 100,
  startAfterSiteId = null,
  startAfterWorkerId = null,
  now = Date.now(),
}) {
  if (!uid) {
    throw new HttpsError("unauthenticated", "User must be authenticated.");
  }

  const nowMillis = (typeof now === "number" && Number.isFinite(now)) ? now : Date.now();

  // 1. Fetch sites page
  let sitesQuery = db.collection("users").doc(uid).collection("sites").limit(batchSize);
  if (startAfterSiteId) {
    const startDoc = await db.collection("users").doc(uid).collection("sites").doc(startAfterSiteId).get();
    if (startDoc.exists) {
      sitesQuery = sitesQuery.startAfter(startDoc);
    }
  }
  const sitesSnap = await sitesQuery.get();

  // 2. Fetch workers page
  let workersQuery = db.collection("users").doc(uid).collection("workers").limit(batchSize);
  if (startAfterWorkerId) {
    const startDoc = await db.collection("users").doc(uid).collection("workers").doc(startAfterWorkerId).get();
    if (startDoc.exists) {
      workersQuery = workersQuery.startAfter(startDoc);
    }
  }
  const workersSnap = await workersQuery.get();

  let migratedSites = 0;
  let migratedWorkers = 0;
  let lastSiteId = null;
  let lastWorkerId = null;

  // Run atomic writes for allocations and accumulated migration state
  await db.runTransaction(async (transaction) => {
    const quotaRef = db.collection("users").doc(uid).collection("quota").doc("usage");
    const migrationRef = db.collection("users").doc(uid).collection("quota").doc("migrationState");

    const [quotaSnap, migrationSnap] = await Promise.all([
      transaction.get(quotaRef),
      transaction.get(migrationRef),
    ]);

    const quotaData = quotaSnap.exists ? quotaSnap.data() : {};
    const migrationData = migrationSnap.exists ? migrationSnap.data() : {};

    const pageActiveSiteIds = [];
    if (sitesSnap && sitesSnap.docs) {
      for (const doc of sitesSnap.docs) {
        lastSiteId = doc.id;
        const data = doc.data();
        if (!data.isDeleted) {
          const syncId = data.syncId || doc.id;
          pageActiveSiteIds.push(syncId);
          const allocRef = db.collection("users").doc(uid).collection("quotaAllocations").doc(`site_${syncId}`);
          transaction.set(allocRef, {
            allocationId: `site_${syncId}`,
            entityType: "site",
            syncId,
            allocatedAt: nowMillis,
            updatedAt: nowMillis,
          }, {merge: true});
          migratedSites++;
        }
      }
    }

    const pageActiveWorkerIds = [];
    if (workersSnap && workersSnap.docs) {
      for (const doc of workersSnap.docs) {
        lastWorkerId = doc.id;
        const data = doc.data();
        if (!data.isDeleted) {
          const syncId = data.syncId || doc.id;
          pageActiveWorkerIds.push(syncId);
          const allocRef = db.collection("users").doc(uid).collection("quotaAllocations").doc(`worker_${syncId}`);
          transaction.set(allocRef, {
            allocationId: `worker_${syncId}`,
            entityType: "worker",
            syncId,
            allocatedAt: nowMillis,
            updatedAt: nowMillis,
          }, {merge: true});
          migratedWorkers++;
        }
      }
    }

    const existingSiteIds = Array.isArray(quotaData.activeSiteIds) ? quotaData.activeSiteIds : [];
    const existingWorkerIds = Array.isArray(quotaData.activeWorkerIds) ? quotaData.activeWorkerIds : [];

    const accumulatedSiteIds = Array.from(new Set([...existingSiteIds, ...pageActiveSiteIds]));
    const accumulatedWorkerIds = Array.from(new Set([...existingWorkerIds, ...pageActiveWorkerIds]));

    const hasMore = (sitesSnap.docs.length >= batchSize) || (workersSnap.docs.length >= batchSize);

    // Update migration state document
    transaction.set(
        migrationRef,
        {
          status: hasMore ? "IN_PROGRESS" : "COMPLETED",
          totalMigratedSites: (migrationData.totalMigratedSites || 0) + migratedSites,
          totalMigratedWorkers: (migrationData.totalMigratedWorkers || 0) + migratedWorkers,
          lastMigratedSiteId: lastSiteId,
          lastMigratedWorkerId: lastWorkerId,
          schemaVersion: SCHEMA_VERSION,
          updatedAt: nowMillis,
        },
        {merge: true},
    );

    // Update quota document: accumulate IDs without dropping legacy data
    transaction.set(
        quotaRef,
        {
          schemaVersion: SCHEMA_VERSION,
          activeSiteCount: accumulatedSiteIds.length,
          activeWorkerCount: accumulatedWorkerIds.length,
          activeSiteIds: accumulatedSiteIds,
          activeWorkerIds: accumulatedWorkerIds,
          initialized: !hasMore,
          updatedAt: nowMillis,
        },
        {merge: true},
    );
  });

  const hasMoreSites = sitesSnap.docs.length >= batchSize;
  const hasMoreWorkers = workersSnap.docs.length >= batchSize;

  return {
    success: true,
    migratedSites,
    migratedWorkers,
    hasMore: hasMoreSites || hasMoreWorkers,
    lastSiteId,
    lastWorkerId,
  };
}

/**
 * Administrative reconciliation operation to correct desynchronized quota allocations.
 * Requires Admin Custom Claims (request.auth.token.admin === true).
 * Strictly inspects actual documents and logs audit entries.
 */
async function reconcileQuotaLedger({
  adminUid,
  targetUid,
  isAdmin,
  db,
  fieldValue,
  now = Date.now(),
}) {
  if (!isAdmin || !adminUid) {
    throw new HttpsError("permission-denied", "Only administrators can trigger quota reconciliation.");
  }
  if (!isValidDocumentId(targetUid)) {
    throw new HttpsError("invalid-argument", "Invalid or missing target UID.");
  }

  const sitesCol = db.collection("users").doc(targetUid).collection("sites");
  const workersCol = db.collection("users").doc(targetUid).collection("workers");
  const allocCol = db.collection("users").doc(targetUid).collection("quotaAllocations");
  const quotaRef = db.collection("users").doc(targetUid).collection("quota").doc("usage");

  const nowMillis = (typeof now === "number" && Number.isFinite(now)) ? now : Date.now();

  return db.runTransaction(async (transaction) => {
    // 1. Read all actual documents, allocations, and current quota document for full OCC conflict protection
    const [sitesSnap, workersSnap, allocSnap] = await Promise.all([
      transaction.get(sitesCol),
      transaction.get(workersCol),
      transaction.get(allocCol),
      transaction.get(quotaRef),
    ]);

    const actualActiveSites = new Map();
    if (sitesSnap && sitesSnap.docs) {
      for (const doc of sitesSnap.docs) {
        const d = doc.data();
        if (!d.isDeleted) {
          actualActiveSites.set(d.syncId || doc.id, d);
        }
      }
    }

    const actualActiveWorkers = new Map();
    if (workersSnap && workersSnap.docs) {
      for (const doc of workersSnap.docs) {
        const d = doc.data();
        if (!d.isDeleted) {
          actualActiveWorkers.set(d.syncId || doc.id, d);
        }
      }
    }

    const existingAllocations = new Map();
    if (allocSnap && allocSnap.docs) {
      for (const doc of allocSnap.docs) {
        existingAllocations.set(doc.id, doc.data());
      }
    }

    let addedAllocations = 0;
    let purgedAllocations = 0;

    // 2. Add missing allocations
    for (const [syncId] of actualActiveSites.entries()) {
      const allocId = `site_${syncId}`;
      if (!existingAllocations.has(allocId)) {
        const allocRef = allocCol.doc(allocId);
        transaction.set(allocRef, {
          allocationId: allocId,
          entityType: "site",
          syncId,
          allocatedAt: nowMillis,
          updatedAt: nowMillis,
          reconciled: true,
        });
        addedAllocations++;
      }
    }

    for (const [syncId] of actualActiveWorkers.entries()) {
      const allocId = `worker_${syncId}`;
      if (!existingAllocations.has(allocId)) {
        const allocRef = allocCol.doc(allocId);
        transaction.set(allocRef, {
          allocationId: allocId,
          entityType: "worker",
          syncId,
          allocatedAt: nowMillis,
          updatedAt: nowMillis,
          reconciled: true,
        });
        addedAllocations++;
      }
    }

    // 3. Purge orphaned allocations
    for (const [allocId, data] of existingAllocations.entries()) {
      if (data.entityType === "site" && !actualActiveSites.has(data.syncId)) {
        transaction.delete(allocCol.doc(allocId));
        purgedAllocations++;
      } else if (data.entityType === "worker" && !actualActiveWorkers.has(data.syncId)) {
        transaction.delete(allocCol.doc(allocId));
        purgedAllocations++;
      }
    }

    // 4. Update summary quota counter document
    transaction.set(
        quotaRef,
        {
          schemaVersion: SCHEMA_VERSION,
          activeSiteCount: actualActiveSites.size,
          activeWorkerCount: actualActiveWorkers.size,
          activeSiteIds: Array.from(actualActiveSites.keys()),
          activeWorkerIds: Array.from(actualActiveWorkers.keys()),
          lastReconciledAt: nowMillis,
          reconciledBy: adminUid,
          updatedAt: nowMillis,
        },
        {merge: true},
    );

    // 5. Write audit log
    const auditRef = db.collection("adminAuditLogs").doc();
    transaction.set(auditRef, {
      action: "RECONCILE_QUOTA_LEDGER",
      adminUid,
      targetUid,
      addedAllocations,
      purgedAllocations,
      totalActiveSites: actualActiveSites.size,
      totalActiveWorkers: actualActiveWorkers.size,
      timestamp: nowMillis,
    });

    return {
      success: true,
      targetUid,
      addedAllocations,
      purgedAllocations,
      totalActiveSites: actualActiveSites.size,
      totalActiveWorkers: actualActiveWorkers.size,
    };
  });
}

/**
 * Initializes the quota ledger and allocations for an existing user.
 */
async function initializeQuotaLedger({
  uid,
  db,
  fieldValue,
  now = Date.now(),
}) {
  return migrateUserQuotaToAllocations({
    uid,
    db,
    fieldValue,
    batchSize: 500,
  });
}

module.exports = {
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
  initializeQuotaLedger,
};

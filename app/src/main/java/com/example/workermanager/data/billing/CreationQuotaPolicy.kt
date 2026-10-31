package com.example.workermanager.data.billing

enum class QuotaEntityType(val displayName: String) {
    SITE("site"),
    WORKER("worker")
}

class QuotaExceededException(
    val entityType: QuotaEntityType,
    val currentCount: Int,
    val limit: Int,
    message: String = "Free plan quota exceeded: Maximum $limit active ${if (limit == 1) entityType.displayName else "${entityType.displayName}s"} allowed. Upgrade to WorkCraft Pro."
) : IllegalStateException(message)

object CreationQuotaPolicy {
    fun canCreateSite(currentActiveCount: Int, proAccessLevel: ProAccessLevel): Boolean {
        if (proAccessLevel.isProUnlocked) return true
        return currentActiveCount < ProAccessLevel.FREE_SITE_LIMIT
    }

    fun canCreateWorker(currentActiveCount: Int, proAccessLevel: ProAccessLevel): Boolean {
        if (proAccessLevel.isProUnlocked) return true
        return currentActiveCount < ProAccessLevel.FREE_WORKER_LIMIT
    }

    fun validateSiteCreation(currentActiveCount: Int, proAccessLevel: ProAccessLevel) {
        if (!canCreateSite(currentActiveCount, proAccessLevel)) {
            throw QuotaExceededException(
                entityType = QuotaEntityType.SITE,
                currentCount = currentActiveCount,
                limit = ProAccessLevel.FREE_SITE_LIMIT,
                message = "Free accounts can manage up to ${ProAccessLevel.FREE_SITE_LIMIT} active site. Upgrade to WorkCraft Pro for unlimited sites."
            )
        }
    }

    fun validateWorkerCreation(currentActiveCount: Int, proAccessLevel: ProAccessLevel) {
        if (!canCreateWorker(currentActiveCount, proAccessLevel)) {
            throw QuotaExceededException(
                entityType = QuotaEntityType.WORKER,
                currentCount = currentActiveCount,
                limit = ProAccessLevel.FREE_WORKER_LIMIT,
                message = "Free accounts can manage up to ${ProAccessLevel.FREE_WORKER_LIMIT} active workers. Upgrade to WorkCraft Pro for unlimited workers."
            )
        }
    }
}


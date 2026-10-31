package com.example.workermanager.data.remote.sync

import com.example.workermanager.data.entity.SiteEntity
import com.example.workermanager.data.entity.WorkerEntity
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object RecordRevisionManager {
    private val localSequenceMap = ConcurrentHashMap<String, AtomicLong>()

    fun recordLocalEdit(syncId: String): Long {
        if (syncId.isBlank()) return 0L
        return localSequenceMap.computeIfAbsent(syncId) { AtomicLong(0L) }.incrementAndGet()
    }

    fun getLocalSequence(syncId: String): Long {
        return localSequenceMap[syncId]?.get() ?: 0L
    }

    fun reset() {
        localSequenceMap.clear()
        MonotonicClock.reset()
    }

    /**
     * Serializes SiteEntity using unambiguous length-prefixed binary encoding (TLV),
     * preventing any possible delimiter collision.
     */
    fun serializeSite(site: SiteEntity): ByteArray {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        dos.writeByte(1)
        dos.writeLong(site.id)

        dos.writeByte(2)
        dos.writeUTF(site.syncId)

        dos.writeByte(3)
        dos.writeUTF(site.name)

        dos.writeByte(4)
        dos.writeUTF(site.address)

        dos.writeByte(5)
        dos.writeUTF(site.manager)

        dos.writeByte(6)
        dos.writeLong(site.startDate.toEpochDay())

        dos.writeByte(7)
        dos.writeInt(site.status.ordinal)

        dos.writeByte(8)
        dos.writeBoolean(site.isDeleted)

        dos.writeByte(9)
        dos.writeBoolean(site.deletedAt != null)
        if (site.deletedAt != null) dos.writeLong(site.deletedAt)

        dos.writeByte(10)
        dos.writeInt(site.syncOrigin)

        dos.writeByte(11)
        dos.writeLong(site.updatedAt)

        dos.writeByte(12)
        dos.writeBoolean(site.isPendingSync)

        dos.writeByte(13)
        dos.writeLong(getLocalSequence(site.syncId))

        dos.flush()
        return baos.toByteArray()
    }

    /**
     * Serializes WorkerEntity using unambiguous length-prefixed binary encoding (TLV),
     * preventing any possible delimiter collision.
     */
    fun serializeWorker(worker: WorkerEntity): ByteArray {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        dos.writeByte(1)
        dos.writeLong(worker.id)

        dos.writeByte(2)
        dos.writeUTF(worker.syncId)

        dos.writeByte(3)
        dos.writeUTF(worker.name)

        dos.writeByte(4)
        dos.writeUTF(worker.phone)

        dos.writeByte(5)
        dos.writeInt(worker.workerType.ordinal)

        dos.writeByte(6)
        dos.writeDouble(worker.dailyRate)

        dos.writeByte(7)
        dos.writeDouble(worker.overtimeRate)

        dos.writeByte(8)
        dos.writeLong(worker.joiningDate.toEpochDay())

        dos.writeByte(9)
        dos.writeBoolean(worker.currentSiteId != null)
        if (worker.currentSiteId != null) dos.writeLong(worker.currentSiteId)

        dos.writeByte(10)
        dos.writeBoolean(worker.isActive)

        dos.writeByte(11)
        dos.writeUTF(worker.notes)

        dos.writeByte(12)
        dos.writeBoolean(worker.photoPath != null)
        if (worker.photoPath != null) dos.writeUTF(worker.photoPath)

        dos.writeByte(13)
        dos.writeBoolean(worker.isDeleted)

        dos.writeByte(14)
        dos.writeBoolean(worker.deletedAt != null)
        if (worker.deletedAt != null) dos.writeLong(worker.deletedAt)

        dos.writeByte(15)
        dos.writeInt(worker.syncOrigin)

        dos.writeByte(16)
        dos.writeLong(worker.updatedAt)

        dos.writeByte(17)
        dos.writeBoolean(worker.isPendingSync)

        dos.writeByte(18)
        dos.writeLong(getLocalSequence(worker.syncId))

        dos.flush()
        return baos.toByteArray()
    }

    /**
     * Computes a deterministic, unambiguous SHA-256 revision hash for a SiteEntity.
     */
    fun computeSiteRevision(site: SiteEntity): String {
        val bytes = serializeSite(site)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Computes a deterministic, unambiguous SHA-256 revision hash for a WorkerEntity.
     */
    fun computeWorkerRevision(worker: WorkerEntity): String {
        val bytes = serializeWorker(worker)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}

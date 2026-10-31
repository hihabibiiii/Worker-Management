package com.example.workermanager.data.remote.sync

import java.util.concurrent.atomic.AtomicLong

object MonotonicClock {
    private val lastTimestamp = AtomicLong(0L)

    /**
     * Returns a strictly monotonically increasing epoch millisecond timestamp.
     * Guarantees that two consecutive calls (even within the exact same system clock millisecond
     * or under backward clock adjustments) will never return the same timestamp.
     */
    fun currentTimeMillis(): Long {
        val now = System.currentTimeMillis()
        return lastTimestamp.updateAndGet { prev ->
            if (now > prev) now else prev + 1L
        }
    }

    /**
     * Resets the clock (useful for unit test isolation).
     */
    fun reset() {
        lastTimestamp.set(0L)
    }
}


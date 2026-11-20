package com.example.stability

import android.util.Log
import com.example.model.StabilitySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Monitors runtime stability, memory footprint, icon cache efficiency,
 * and safely logs non-fatal errors so the launcher remains responsive and crash-free.
 */
object StabilityMonitor {
    private const val TAG = "MonoGlassStability"

    private val startTimeMs = System.currentTimeMillis()
    private val handledErrorsCount = AtomicInteger(0)
    private val lastErrorSummary = AtomicReference<String?>(null)
    private val cacheHits = AtomicLong(0L)
    private val cacheMisses = AtomicLong(0L)
    private val cachedIconCount = AtomicInteger(0)
    private val loadedAppsCount = AtomicInteger(0)

    @Volatile
    private var exceptionGuardInstalled = false

    private val _snapshot = MutableStateFlow(StabilitySnapshot())
    val snapshot: StateFlow<StabilitySnapshot> = _snapshot.asStateFlow()

    fun installUncaughtExceptionGuard() {
        if (exceptionGuardInstalled) return
        exceptionGuardInstalled = true
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception on thread=${thread.name}", throwable)
            lastErrorSummary.set("${throwable::class.simpleName}: ${throwable.message}")
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    fun recordHandledError(source: String, throwable: Throwable) {
        val count = handledErrorsCount.incrementAndGet()
        val msg = "$source: ${throwable::class.simpleName ?: "Error"} (${throwable.message ?: "unknown"})"
        lastErrorSummary.set(msg)
        Log.w(TAG, "Handled safely [$count] -> $msg", throwable)
        refreshSnapshot()
    }

    fun recordCacheHit(currentSize: Int) {
        cacheHits.incrementAndGet()
        cachedIconCount.set(currentSize)
    }

    fun recordCacheMiss(currentSize: Int) {
        cacheMisses.incrementAndGet()
        cachedIconCount.set(currentSize)
    }

    fun updateLoadedAppsCount(count: Int) {
        loadedAppsCount.set(count)
        refreshSnapshot()
    }

    fun refreshSnapshot() {
        val runtime = Runtime.getRuntime()
        val usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024f * 1024f)
        val maxMb = runtime.maxMemory() / (1024f * 1024f)
        val uptimeSec = ((System.currentTimeMillis() - startTimeMs) / 1000L).coerceAtLeast(0L)

        _snapshot.update {
            StabilitySnapshot(
                uptimeSeconds = uptimeSec,
                heapUsedMb = usedMb,
                heapMaxMb = maxMb,
                cachedIconCount = cachedIconCount.get(),
                iconCacheHits = cacheHits.get(),
                iconCacheMisses = cacheMisses.get(),
                loadedAppsCount = loadedAppsCount.get(),
                handledErrorsCount = handledErrorsCount.get(),
                lastErrorSummary = lastErrorSummary.get()
            )
        }
    }
}

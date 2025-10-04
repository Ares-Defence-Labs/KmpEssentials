package com.architect.kmpessentials.stopwatch

import com.architect.kmpessentials.aliases.DefaultAction
import com.architect.kmpessentials.aliases.DefaultThrowableAction
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.*
import kotlin.time.Duration
import kotlin.time.TimeSource
import com.benasher44.uuid.uuid4

object KmpStopwatch {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = SynchronizedObject()
    private val clock = TimeSource.Monotonic

    private data class Entry(
        val mark: TimeSource.Monotonic.ValueTimeMark,
        val tickJob: Job?,
        val timeoutJob: Job?
    )

    // guarded by `lock`
    private val entries = mutableMapOf<String, Entry>()

    /**
     * Starts a timer that can:
     *  - call [onIteration] every [iterationIntervalMillis] (if provided and > 0)
     *  - call [onTimeout] once after [timeoutMillis] (if provided)
     * Returns a unique id for this timer that can be tracked & cleared
     */
    fun startTimerWithAfterAction(
        iterationIntervalMillis: Long = 0L,
        timeoutMillis: Long? = null,
        onIteration: DefaultAction? = null,
        onTimeout: DefaultAction? = null,
        onError: DefaultThrowableAction? = null
    ): String {
        val id = uuid4().toString()
        val mark = clock.markNow()

        // Periodic tick job (optional)
        val tickJob: Job? =
            if (onIteration != null && iterationIntervalMillis > 0L) {
                scope.launch {
                    // start counting after the first interval
                    while (isActive) {
                        delay(iterationIntervalMillis)
                        val stillActive = synchronized(lock) { entries.containsKey(id) }
                        if (!stillActive) break
                        try {
                            onIteration()
                        } catch (e: Throwable) {
                            onError?.invoke(e)
                        }
                    }
                }
            } else null

        // One-shot timeout job (optional)
        val timeoutJob: Job? =
            if (onTimeout != null && timeoutMillis != null && timeoutMillis > 0L) {
                scope.launch {
                    delay(timeoutMillis)
                    val stillActive = synchronized(lock) { entries.containsKey(id) }
                    if (stillActive) {
                        try {
                            onTimeout()
                        } catch (e: Throwable) {
                            onError?.invoke(e)
                        }

                        stopTimerWithId(id)
                    }
                }
            } else null

        synchronized(lock) {
            entries[id] = Entry(mark, tickJob, timeoutJob)
        }
        return id
    }

    fun stopTimerWithId(id: String) {
        val (tick, timeout) = synchronized(lock) {
            val removed = entries.remove(id)
            Pair(removed?.tickJob, removed?.timeoutJob)
        }
        tick?.cancel()
        timeout?.cancel()
    }

    fun stopAllTimers() {
        val jobs: List<Job> = synchronized(lock) {
            val js = entries.values.flatMap { listOfNotNull(it.tickJob, it.timeoutJob) }
            entries.clear()
            js
        }
        jobs.forEach { it.cancel() }
    }

    fun elapsed(id: String): Duration? {
        val mark = synchronized(lock) { entries[id]?.mark } ?: return null
        return mark.elapsedNow()
    }
}
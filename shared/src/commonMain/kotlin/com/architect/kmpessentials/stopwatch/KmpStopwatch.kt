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

    private class Entry(
        var startMark: TimeSource.Monotonic.ValueTimeMark,
        var accumulatedElapsed: Duration,
        val iterationIntervalMillis: Long,
        val timeoutMillis: Long?,
        var tickJob: Job?,
        var timeoutJob: Job?,
        val onIteration: DefaultAction?,
        val onTimeout: DefaultAction?,
        val onError: DefaultThrowableAction?,
        var paused: Boolean
    )

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

        val entry = Entry(
            startMark = mark,
            accumulatedElapsed = Duration.ZERO,
            iterationIntervalMillis = iterationIntervalMillis,
            timeoutMillis = timeoutMillis,
            tickJob = null,
            timeoutJob = null,
            onIteration = onIteration,
            onTimeout = onTimeout,
            onError = onError,
            paused = false
        )

        synchronized(lock) {
            entries[id] = entry
        }

        if (onIteration != null && iterationIntervalMillis > 0L) {
            val tickJob = launchTickJob(id, iterationIntervalMillis)
            synchronized(lock) {
                entries[id]?.tickJob = tickJob
            }
        }

        if (onTimeout != null && timeoutMillis != null && timeoutMillis > 0L) {
            val timeoutJob = launchTimeoutJob(id, timeoutMillis)
            synchronized(lock) {
                entries[id]?.timeoutJob = timeoutJob
            }
        }

        return id
    }

    /**
     * Stop and remove a single timer.
     */
    fun stopTimerWithId(id: String) {
        val removed = synchronized(lock) {
            entries.remove(id)
        }
        removed?.tickJob?.cancel()
        removed?.timeoutJob?.cancel()
    }

    /**
     * Stop and remove ALL timers.
     */
    fun stopAllTimers() {
        val jobs = synchronized(lock) {
            val js = entries.values.flatMap { listOfNotNull(it.tickJob, it.timeoutJob) }
            entries.clear()
            js
        }
        jobs.forEach { it.cancel() }
    }

    fun pauseAllTimers() {
        synchronized(lock) {
            entries.values.forEach { entry ->
                if (entry.paused) return@forEach

                // Add time since the current running segment started
                val elapsedSinceStart = entry.startMark.elapsedNow()
                entry.accumulatedElapsed += elapsedSinceStart

                entry.paused = true
                entry.tickJob?.cancel()
                entry.timeoutJob?.cancel()
                entry.tickJob = null
                entry.timeoutJob = null
            }
        }
    }

    fun resumeAllTimers() {
        data class ResumeInfo(
            val id: String,
            val iterationIntervalMillis: Long,
            val hasIteration: Boolean,
            val remainingTimeoutMillis: Long?,
        )

        val toResume: List<ResumeInfo> = synchronized(lock) {
            entries
                .filter { it.value.paused }
                .map { (id, entry) ->
                    entry.paused = false
                    entry.startMark = clock.markNow()

                    val remainingTimeoutMillis = entry.timeoutMillis?.let { total ->
                        val used = entry.accumulatedElapsed.inWholeMilliseconds
                        (total - used).coerceAtLeast(0L)
                    }

                    ResumeInfo(
                        id = id,
                        iterationIntervalMillis = entry.iterationIntervalMillis,
                        hasIteration = entry.onIteration != null && entry.iterationIntervalMillis > 0L,
                        remainingTimeoutMillis = remainingTimeoutMillis
                    )
                }
        }

        toResume.forEach { info ->
            if (info.hasIteration && info.iterationIntervalMillis > 0L) {
                val tickJob = launchTickJob(info.id, info.iterationIntervalMillis)
                synchronized(lock) {
                    entries[info.id]?.tickJob = tickJob
                }
            }

            // Restart timeout job if applicable
            val timeoutLeft = info.remainingTimeoutMillis
            if (timeoutLeft != null) {
                if (timeoutLeft <= 0L) {
                    var onTimeout: DefaultAction? = null
                    var onError: DefaultThrowableAction? = null

                    synchronized(lock) {
                        val entry = entries[info.id]
                        onTimeout = entry?.onTimeout
                        onError = entry?.onError
                    }

                    try {
                        onTimeout?.invoke()
                    } catch (t: Throwable) {
                        onError?.invoke(t)
                    }

                    stopTimerWithId(info.id)
                } else {
                    val timeoutJob = launchTimeoutJob(info.id, timeoutLeft)
                    synchronized(lock) {
                        entries[info.id]?.timeoutJob = timeoutJob
                    }
                }
            }
        }
    }

    private fun launchTickJob(
        id: String,
        intervalMillis: Long
    ): Job = scope.launch {
        while (isActive) {
            delay(intervalMillis)

            var onIteration: DefaultAction? = null
            var onError: DefaultThrowableAction? = null

            synchronized(lock) {
                val entry = entries[id] ?: return@launch
                if (entry.paused) return@launch
                onIteration = entry.onIteration
                onError = entry.onError
            }

            try {
                onIteration?.invoke()
            } catch (t: Throwable) {
                onError?.invoke(t)
            }
        }
    }

    private fun launchTimeoutJob(
        id: String,
        delayMillis: Long
    ): Job = scope.launch {
        delay(delayMillis)

        var onTimeout: DefaultAction? = null
        var onError: DefaultThrowableAction? = null

        synchronized(lock) {
            val entry = entries[id] ?: return@launch
            if (entry.paused) return@launch
            onTimeout = entry.onTimeout
            onError = entry.onError
        }

        try {
            onTimeout?.invoke()
        } catch (t: Throwable) {
            onError?.invoke(t)
        }

        stopTimerWithId(id)
    }
}
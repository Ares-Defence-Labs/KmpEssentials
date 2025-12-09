// watchosMain/kotlin/com/architect/kmpessentials/backgrounding/KmpBackgrounding.watchos.kt
package com.architect.kmpessentials.backgrounding

import com.architect.kmpessentials.aliases.DefaultActionAsync
import kotlinx.cinterop.UnsafeNumber
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import platform.Foundation.NSError
import platform.Foundation.NSUUID
import platform.UserNotifications.*
import platform.WatchKit.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.cancellation.CancellationException

@OptIn(UnsafeNumber::class)
actual class KmpBackgrounding {
    actual companion object {
        private val sessions = mutableListOf<WKExtendedRuntimeSession>()
        private val backgroundJobs = mutableListOf<Job>()
        private val foregroundRequests = mutableListOf<String>()

        var foregroundIcon: Any? = null
        fun setForegroundIcon(foregroundIcon: Any?) { this.foregroundIcon = foregroundIcon }

        /**
         * Fire-and-forget worker. Starts an extended runtime session, launches the action,
         * and invalidates the session when done. No cancellation handle is retained.
         */
        actual fun createAndStartWorkerWithoutCancel(
            options: BackgroundOptions?,
            action: DefaultActionAsync
        ) {
            dispatch_async(dispatch_get_main_queue()) {
                val session = WKExtendedRuntimeSession().apply {
                    delegate = SimpleSessionDelegate(
                        onInvalidate = { /* no-op for fire-and-forget */ }
                    )
                    start()
                }

                GlobalScope.launch {
                    try {
                        action()
                    } finally {
                        // Ensure we end the session when the work completes
                        dispatch_async(dispatch_get_main_queue()) { session.invalidate() }
                    }
                }
            }
        }

        /**
         * Cancellable worker. Starts an extended runtime session, launches the action,
         * and keeps references so we can cancel later via cancelAllRunningWorkers().
         */
        actual fun createAndStartWorker(
            options: BackgroundOptions?,
            action: DefaultActionAsync
        ) {
            dispatch_async(dispatch_get_main_queue()) {
                val session = WKExtendedRuntimeSession().apply {
                    delegate = SimpleSessionDelegate(
                        onInvalidate = {
                        }
                    )
                    start()
                }
                sessions.add(session)

                val job = GlobalScope.launch {
                    try {
                        action()
                    } catch (_: CancellationException) {
                        // Expected when cancelAllRunningWorkers() is called
                    } finally {
                        // Clean up session on completion (from main queue)
                        dispatch_async(dispatch_get_main_queue()) {
                            session.invalidate()
                            sessions.remove(session)
                        }
                    }
                }

                backgroundJobs.add(job)
            }
        }

        /**
         * Cancels all known jobs and invalidates all extended sessions.
         * Also clears any “foreground” notification requests this class created.
         */
        actual fun cancelAllRunningWorkers() {
            // Cancel jobs
            backgroundJobs.forEach { if (!it.isCancelled) it.cancel() }
            backgroundJobs.clear()

            // Invalidate all sessions
            dispatch_async(dispatch_get_main_queue()) {
                sessions.forEach { it.invalidate() }
                sessions.clear()
            }

            if (foregroundRequests.isNotEmpty()) {
                val center = UNUserNotificationCenter.currentNotificationCenter()
                center.removePendingNotificationRequestsWithIdentifiers(foregroundRequests)
                center.removeDeliveredNotificationsWithIdentifiers(foregroundRequests)
                foregroundRequests.clear()
            }
        }

        /**
         * Foreground-like worker on watchOS: we cannot run a true foreground service.
         * emulate intent by:
         *  1) Requesting notification permission (once).
         *  2) Posting a local notification with [title]/[message] as a status banner.
         *  3) Starting an extended runtime session and running [action].
         */
        actual fun createAndStartForegroundWorker(
            title: String,
            message: String,
            action: DefaultActionAsync
        ) {
            // 1) Ask for notification permission if needed
            requestNotificationAuth { granted ->
                // 2) Post a small “running” notification (best-effort UX analogue)
                val reqId = NSUUID().UUIDString()
                if (granted) {
                    val center = UNUserNotificationCenter.currentNotificationCenter()
                    val content = UNMutableNotificationContent().apply {
                        setTitle(title)
                        setBody(message)
                    }
                    val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(0.1, false)
                    val request = UNNotificationRequest.requestWithIdentifier(reqId, content, trigger)
                    center.addNotificationRequest(request, withCompletionHandler = null)
                    foregroundRequests.add(reqId)
                }

                // 3) Start the extended session + job with cancellation support
                createAndStartWorker(options = null, action = action)
            }
        }

        // --- Helpers ---

        private fun requestNotificationAuth(done: (Boolean) -> Unit) {
            val center = UNUserNotificationCenter.currentNotificationCenter()
            center.getNotificationSettingsWithCompletionHandler { settings ->
                when (settings?.authorizationStatus) {
                    UNAuthorizationStatusAuthorized,
                    UNAuthorizationStatusProvisional,
                    UNAuthorizationStatusEphemeral -> done(true)
                    else -> {
                        center.requestAuthorizationWithOptions(
                            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound
                        ) { granted, _ -> done(granted) }
                    }
                }
            }
        }
    }
}

/** Minimal WKExtendedRuntimeSession delegate wrapper */
private class SimpleSessionDelegate(
    private val onInvalidate: () -> Unit
) : NSObject(), WKExtendedRuntimeSessionDelegateProtocol {

    override fun extendedRuntimeSessionDidStart(session: WKExtendedRuntimeSession) {
        // no-op
    }

    override fun extendedRuntimeSessionWillExpire(session: WKExtendedRuntimeSession) {
    }

    @OptIn(UnsafeNumber::class)
    override fun extendedRuntimeSession(
        extendedRuntimeSession: WKExtendedRuntimeSession,
        didInvalidateWithReason: WKExtendedRuntimeSessionInvalidationReason,
        error: NSError?
    ) {
        onInvalidate()
    }
}
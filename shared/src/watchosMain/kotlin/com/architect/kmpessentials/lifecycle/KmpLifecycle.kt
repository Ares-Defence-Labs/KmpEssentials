// watchosMain/kotlin/com/architect/kmpessentials/lifecycle/KmpLifecycle.watchos.kt
package com.architect.kmpessentials.lifecycle

import com.architect.kmpessentials.aliases.DefaultAction
import com.architect.kmpessentials.aliases.DefaultActionAsync
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import platform.Foundation.NSNotificationCenter
import platform.WatchKit.WKApplicationState.WKApplicationStateActive
import platform.WatchKit.WKApplicationState.WKApplicationStateBackground
import platform.WatchKit.WKApplicationState.WKApplicationStateInactive
import platform.WatchKit.WKExtension
import platform.darwin.NSObject

private enum class AppState {
    FOREGROUND,
    BACKGROUND
}

actual class KmpLifecycle {
    actual companion object {
        private var currentState = atomic<AppState?>(null)

        private var backgroundAction: (() -> Unit)? = null
        private var foregroundAction: (() -> Unit)? = null
        private var destroyAction: DefaultAction? = null
        private var isInForeground: Boolean

        // We keep strong refs to observers (even though addObserverForName returns an opaque token),
        // to avoid GC surprises in some toolchains.
        @Suppress("UNUSED")
        private val observers = mutableListOf<NSObject>()

        init {
            // Initial state from WatchKit
            isInForeground = when (WKExtension.sharedExtension().applicationState) {
                WKApplicationStateActive -> true
                WKApplicationStateInactive -> false
                WKApplicationStateBackground -> false
                else -> false
            }
            setupLifecycleObservers()
        }

        actual fun setAppLifecycleBackground(action: () -> Unit) {
            backgroundAction = action
        }

        actual fun setAppLifecycleForeground(action: () -> Unit) {
            foregroundAction = action
        }

        actual fun setAppLifecycleDestroy(action: DefaultAction) {
            destroyAction = action
        }

        actual suspend fun waitForAppToReturnToForegroundWithTimeout(
            milliseconds: Long,
            action: DefaultActionAsync
        ) {
            val startTime = Clock.System.now().toEpochMilliseconds()
            withTimeoutOrNull(milliseconds) {
                while (!isInForeground) {        // poll foreground
                    delay(100)                    // check every 100 ms
                    if (Clock.System.now().toEpochMilliseconds() - startTime >= milliseconds) {
                        break
                    }
                }
                true
            }
            action()
        }

        actual suspend fun waitForAppToReturnToBackgroundWithTimeout(
            milliseconds: Long,
            action: DefaultActionAsync
        ) {
            val startTime = Clock.System.now().toEpochMilliseconds()
            withTimeoutOrNull(milliseconds) {
                while (isInForeground) {          // poll background
                    delay(100)                    // check every 100 ms
                    if (Clock.System.now().toEpochMilliseconds() - startTime >= milliseconds) {
                        break
                    }
                }
                true
            }
            action()
        }

        actual suspend fun waitForAppToReturnToForeground(action: DefaultActionAsync) {
            while (!isInForeground) {
                delay(100)
            }
            action()
        }

        actual fun isCurrentlyInForeground(): Boolean = isInForeground

        actual fun resetAppLifecycleActions() {
            backgroundAction = null
            foregroundAction = null
            destroyAction = null
        }

        // --- internals ---

        private fun transitionTo(newState: AppState) {
            val previous = currentState.value
            if (previous != newState) {
                currentState.value = newState
                when (newState) {
                    AppState.FOREGROUND -> {
                        isInForeground = true
                        foregroundAction?.invoke()
                    }

                    AppState.BACKGROUND -> {
                        isInForeground = false
                        backgroundAction?.invoke()
                    }
                }
            }
        }

        private fun setupLifecycleObservers() {
            val nc = NSNotificationCenter.defaultCenter

            // On watchOS there are several relevant notifications.
            // We register by *name string* to avoid missing symbol issues across SDK shims.
            // Commonly seen names include:
            //  - "WKApplicationDidBecomeActiveNotification"
            //  - "WKApplicationWillResignActiveNotification"
            //  - "WKExtensionHostWillEnterForegroundNotification"
            //  - "WKExtensionHostDidEnterBackgroundNotification"

            fun observe(name: String, handler: () -> Unit) {
                val token = nc.addObserverForName(
                    name = name,
                    `object` = null,
                    queue = null
                ) { _ -> handler() }
                // Keep a reference
                if (token is NSObject) observers.add(token)
            }

            // Foreground-style signals
            observe("WKApplicationDidBecomeActiveNotification") {
                transitionTo(AppState.FOREGROUND)
            }
            observe("WKExtensionHostWillEnterForegroundNotification") {
                transitionTo(AppState.FOREGROUND)
            }

            // Background-style signals
            observe("WKApplicationWillResignActiveNotification") {
                transitionTo(AppState.BACKGROUND)
            }
            observe("WKExtensionHostDidEnterBackgroundNotification") {
                transitionTo(AppState.BACKGROUND)
            }

            // Termination: watchOS does not reliably broadcast a terminate notification to extensions.
            // If you *must* run cleanup, also call setAppLifecycleDestroy from your WKExtensionDelegate
            // (e.g., in applicationWillResignActive / applicationDidEnterBackground) and invoke it there.
            // We still try to listen for a best-effort name if the platform provides it.
            observe("WKApplicationWillTerminateNotification") {
                destroyAction?.invoke()
                isInForeground = false
                currentState.value = null
            }
        }
    }
}
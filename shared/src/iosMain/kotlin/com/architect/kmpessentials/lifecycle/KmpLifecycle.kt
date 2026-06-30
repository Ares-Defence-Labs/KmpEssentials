package com.architect.kmpessentials.lifecycle

import com.architect.kmpessentials.aliases.DefaultAction
import com.architect.kmpessentials.aliases.DefaultActionAsync
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import platform.Foundation.*
import platform.UIKit.*
import kotlinx.atomicfu.atomic

private enum class AppState {
    FOREGROUND,
    FOREGROUND_INTERRUPTED,
    BACKGROUND
}

actual class KmpLifecycle {
    actual companion object {
        private var currentState = atomic<AppState?>(null)

        private var backgroundAction: (() -> Unit)? = null
        private var foregroundAction: (() -> Unit)? = null
        private var destroyAction: DefaultAction? = null

        private var isInForeground: Boolean = false
        private var isInBackground: Boolean = false

        init {
            currentState.value = when (UIApplication.sharedApplication.applicationState) {
                UIApplicationState.UIApplicationStateActive -> AppState.FOREGROUND
                UIApplicationState.UIApplicationStateBackground -> AppState.BACKGROUND
                else -> AppState.FOREGROUND_INTERRUPTED
            }

            isInForeground = currentState.value == AppState.FOREGROUND
            isInBackground = currentState.value == AppState.BACKGROUND

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
            withTimeoutOrNull(milliseconds) {
                while (!isInForeground) {
                    delay(100)
                }
            }
            action()
        }

        actual suspend fun waitForAppToReturnToBackgroundWithTimeout(
            milliseconds: Long,
            action: DefaultActionAsync
        ) {
            withTimeoutOrNull(milliseconds) {
                while (!isInBackground) {
                    delay(100)
                }
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

        fun isCurrentlyInBackground(): Boolean = isInBackground

        actual fun resetAppLifecycleActions() {
            backgroundAction = null
            foregroundAction = null
            destroyAction = null
        }

        private fun transitionTo(newState: AppState) {
            val previous = currentState.value
            if (previous == newState) return

            currentState.value = newState

            when (newState) {
                AppState.FOREGROUND -> {
                    isInForeground = true
                    isInBackground = false
                    foregroundAction?.invoke()
                }

                AppState.FOREGROUND_INTERRUPTED -> {
                    isInForeground = false
                    isInBackground = false
                }

                AppState.BACKGROUND -> {
                    isInForeground = false
                    isInBackground = true
                    backgroundAction?.invoke()
                }
            }
        }

        private fun setupLifecycleObservers() {
            val notificationCenter = NSNotificationCenter.defaultCenter

            notificationCenter.addObserverForName(
                name = UIApplicationWillResignActiveNotification,
                `object` = null,
                queue = null
            ) { _ ->
                transitionTo(AppState.FOREGROUND_INTERRUPTED)
            }

            notificationCenter.addObserverForName(
                name = UIApplicationDidEnterBackgroundNotification,
                `object` = null,
                queue = null
            ) { _ ->
                transitionTo(AppState.BACKGROUND)
            }

            notificationCenter.addObserverForName(
                name = UIApplicationWillEnterForegroundNotification,
                `object` = null,
                queue = null
            ) { _ ->
                transitionTo(AppState.FOREGROUND_INTERRUPTED)
            }

            notificationCenter.addObserverForName(
                name = UIApplicationDidBecomeActiveNotification,
                `object` = null,
                queue = null
            ) { _ ->
                transitionTo(AppState.FOREGROUND)
            }

            notificationCenter.addObserverForName(
                name = UIApplicationWillTerminateNotification,
                `object` = null,
                queue = null
            ) { _ ->
                destroyAction?.invoke()
                isInForeground = false
                isInBackground = false
                currentState.value = null
            }
        }
    }
}
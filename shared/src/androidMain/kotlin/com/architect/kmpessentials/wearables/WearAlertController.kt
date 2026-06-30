package com.architect.kmpessentials.wearables

import com.architect.kmpessentials.alerts.platforms.WearAlertState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object WearAlertController {
    private val _currentAlert = MutableStateFlow<WearAlertState?>(null)
    private val _isShowing = MutableStateFlow(false)

    val currentAlert: StateFlow<WearAlertState?> =
        _currentAlert.asStateFlow()

    val isShowing: StateFlow<Boolean> =
        _isShowing.asStateFlow()

    fun show(alert: WearAlertState) {
        _currentAlert.value = alert
        _isShowing.value = true
    }

    fun dismiss() {
        _isShowing.value = false
        _currentAlert.value = null
    }
}
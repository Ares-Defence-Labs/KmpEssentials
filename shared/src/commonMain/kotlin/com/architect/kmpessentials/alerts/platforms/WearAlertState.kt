package com.architect.kmpessentials.alerts.platforms

data class WearAlertState(
    val title: String = "",
    val message: String,
    val okText: String = "OK",
    val cancelText: String? = null,
    val secondaryText: String? = null,
    val okAction: () -> Unit = {},
    val cancelAction: () -> Unit = {},
    val secondaryAction: () -> Unit = {}
)
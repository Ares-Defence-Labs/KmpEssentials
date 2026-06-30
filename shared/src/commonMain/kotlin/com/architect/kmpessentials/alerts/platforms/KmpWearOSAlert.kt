package com.architect.kmpessentials.alerts.platforms

import com.architect.kmpessentials.aliases.DefaultAction

interface KmpWearAlertService {
    fun showAlert(
        message: String,
        title: String = ""
    )

    fun showAlert(
        message: String,
        title: String = "",
        okText: String = "OK",
        okAction: DefaultAction
    )

    fun showAlertWithConfirmation(
        message: String,
        title: String = "",
        okText: String = "OK",
        cancelText: String = "Cancel",
        okAction: DefaultAction,
        cancelAction: DefaultAction
    )

    fun showAlertWithTertiaryButtonsConfirmation(
        message: String,
        title: String = "",
        okText: String,
        secondaryText: String,
        cancelText: String = "Cancel",
        okAction: DefaultAction,
        secondaryAction: DefaultAction,
        cancelAction: DefaultAction = {}
    )
}


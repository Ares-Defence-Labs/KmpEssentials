package com.architect.kmpessentials.alerts

import com.architect.kmpessentials.alerts.platforms.KmpWearAlertService
import com.architect.kmpessentials.aliases.DefaultAction

object KmpWearAlert {
    private var service: KmpWearAlertService? = null

    fun initialise(service: KmpWearAlertService) {
        this.service = service
    }

    fun showAlert(message: String, title: String = "") {
        service?.showAlert(message, title)
            ?: error("KmpWearAlert has not been initialised. Call KmpWearAlert.initialise(...) from your Wear app.")
    }

    fun showAlert(
        message: String,
        title: String = "",
        okText: String = "OK",
        okAction: DefaultAction
    ) {
        service?.showAlert(message, title, okText, okAction)
            ?: error("KmpWearAlert has not been initialised. Call KmpWearAlert.initialise(...) from your Wear app.")
    }

    fun showAlertWithConfirmation(
        message: String,
        title: String = "",
        okText: String = "OK",
        cancelText: String = "Cancel",
        okAction: DefaultAction,
        cancelAction: DefaultAction
    ) {
        service?.showAlertWithConfirmation(
            message,
            title,
            okText,
            cancelText,
            okAction,
            cancelAction
        ) ?: error("KmpWearAlert has not been initialised. Call KmpWearAlert.initialise(...) from your Wear app.")
    }
}
package com.architect.kmpessentials.wearables

import com.architect.kmpessentials.alerts.platforms.KmpWearAlertService
import com.architect.kmpessentials.alerts.platforms.WearAlertState
import com.architect.kmpessentials.aliases.DefaultAction

class WearComposeAlertService : KmpWearAlertService {
    override fun showAlert(message: String, title: String) {
        WearAlertController.show(
            WearAlertState(
                title = title,
                message = message
            )
        )
    }

    override fun showAlert(
        message: String,
        title: String,
        okText: String,
        okAction: DefaultAction
    ) {
        WearAlertController.show(
            WearAlertState(
                title = title,
                message = message,
                okText = okText,
                okAction = okAction
            )
        )
    }

    override fun showAlertWithConfirmation(
        message: String,
        title: String,
        okText: String,
        cancelText: String,
        okAction: DefaultAction,
        cancelAction: DefaultAction
    ) {
        WearAlertController.show(
            WearAlertState(
                title = title,
                message = message,
                okText = okText,
                cancelText = cancelText,
                okAction = okAction,
                cancelAction = cancelAction
            )
        )
    }

    override fun showAlertWithTertiaryButtonsConfirmation(
        message: String,
        title: String,
        okText: String,
        secondaryText: String,
        cancelText: String,
        okAction: DefaultAction,
        secondaryAction: DefaultAction,
        cancelAction: DefaultAction
    ) {
        WearAlertController.show(
            WearAlertState(
                title = title,
                message = message,
                okText = okText,
                secondaryText = secondaryText,
                cancelText = cancelText,
                okAction = okAction,
                secondaryAction = secondaryAction,
                cancelAction = cancelAction
            )
        )
    }
}


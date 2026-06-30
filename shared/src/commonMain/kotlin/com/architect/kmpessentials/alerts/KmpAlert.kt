package com.architect.kmpessentials.alerts

import com.architect.kmpessentials.aliases.DefaultAction

/**
 * Used for rendering Alert Popups, and prompting confirmation from the user
 * */
expect class KmpAlert {
    companion object {
        /**
         * Renders a standard alert popup
         * */
        fun showAlert(message: String, title: String)

        /**
         * Renders a standard alert popup with an action you can invoke if users press on Ok
         * */
        fun showAlert(message: String, title: String, okText: String, okAction: DefaultAction)

        /**
         * Renders an alert popup with an action you can invoke if users press on Ok or Cancel Options.
         * Fully Configurable
         * */
        fun showAlertWithConfirmation(
            message: String,
            title: String,
            okText: String,
            cancelText: String,
            okAction: DefaultAction,
            cancelAction: DefaultAction
        )

        /**
         * Renders an alert popup with an action you can invoke if users press on Ok or Secondary, and a Cancel Options.
         * Fully Configurable
         * */
        fun showAlertWithTertiaryButtonsConfirmation(
            message: String,
            title: String,
            okText: String,
            secondaryText: String,
            cancelText: String = "Cancel",
            okAction: DefaultAction,
            secondaryAction: DefaultAction
        )
    }
}


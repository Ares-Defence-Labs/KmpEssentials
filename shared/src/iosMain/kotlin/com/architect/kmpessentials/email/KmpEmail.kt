package com.architect.kmpessentials.email

import com.architect.kmpessentials.aliases.DefaultAction
import com.architect.kmpessentials.email.delegates.EmailReceipientDelegate
import com.architect.kmpessentials.email.extensions.encodeURL
import com.architect.kmpessentials.internal.ActionBoolParams
import com.architect.kmpessentials.logging.KmpLogging
import com.architect.kmpessentials.logging.constants.ErrorCodes
import com.architect.kmpessentials.mainThread.KmpMainThread
import com.architect.kmpessentials.toast.KmpToast
import platform.Foundation.NSURL
import platform.MessageUI.MFMailComposeViewController
import platform.UIKit.UIApplication

actual class KmpEmail {
    actual companion object {
        private val mailDelegate = EmailReceipientDelegate()
        actual fun isEmailSupported(action: ActionBoolParams) {
            KmpMainThread.runViaMainThread {
                val mailtoUrl = NSURL.URLWithString("mailto:")
                val messageToUrl = NSURL.URLWithString("message://")
                action(
                    (mailtoUrl != null && UIApplication.sharedApplication.canOpenURL(mailtoUrl))
                            || (messageToUrl != null && UIApplication.sharedApplication.canOpenURL(
                        messageToUrl
                    )
                            )
                )
            }
        }

        actual fun openEmailClientApp(noAppsFound: DefaultAction?) {
            val mailtoUrl = NSURL.URLWithString("message://")
            if (mailtoUrl != null && UIApplication.sharedApplication.canOpenURL(mailtoUrl)) {
                UIApplication.sharedApplication.openURL(mailtoUrl)
            } else {
                KmpToast.showToastShort("No email app can be found or is available on this device.")
            }
        }

        actual fun sendEmailToAddress(
            address: String,
            emailSubject: String,
            emailMessage: String,
            promptInternal: Boolean
        ) {
            val promptEmailAction = {
                KmpMainThread.runViaMainThread {
                    val emailUrl =
                        NSURL.URLWithString("mailto:$address?subject=${emailSubject}&body=${emailMessage}")
                    if (emailUrl != null) {
                        if (UIApplication.sharedApplication.canOpenURL(emailUrl)) {
                            UIApplication.sharedApplication.openURL(
                                emailUrl,
                                options = emptyMap<Any?, Any?>(),
                                completionHandler = null
                            )
                        }
                    } else {
                        KmpLogging.writeError(
                            "KMP_ESSENTIALS_EMAIL",
                            "Failed to encode url message"
                        )
                    }
                }
            }

            KmpMainThread.runViaMainThread {
                if (promptInternal) {
                    try {
                        if (MFMailComposeViewController.canSendMail()) {
                            val mailController = MFMailComposeViewController().apply {
                                setToRecipients(arrayListOf(address))
                                setMailComposeDelegate(mailDelegate)
                                setSubject(emailSubject)
                                setMessageBody(emailMessage, false)
                            }

                            UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
                                mailController,
                                true
                            ) {

                            }
                        } else {
                            throw Exception("Cannot send email, defaulting to prompt")
                        }
                    } catch (ex: Exception) {
                        promptEmailAction()
                    }
                } else {
                    promptEmailAction()
                }
            }
        }

        actual fun sendEmailsToCCAddress(
            address: String,
            ccAddresses: Array<String>?,
            emailSubject: String,
            emailMessage: String, promptInternal: Boolean
        ) {
            val runEmailAction = {
                KmpMainThread.runViaMainThread {
                    val allRecipients = buildString {
                        append(address)
                        ccAddresses?.takeIf { it.isNotEmpty() }?.let {
                            append("?cc=")
                            append(it.joinToString(",") { cc -> cc })
                        }
                        append("&subject=${emailSubject}")
                        append("&body=${emailMessage}")
                    }

                    val mailtoString = "mailto:$allRecipients"
                    val emailUrl = NSURL.URLWithString(mailtoString)

                    if (emailUrl != null) {
                        if (UIApplication.sharedApplication.canOpenURL(emailUrl)) {
                            UIApplication.sharedApplication.openURL(emailUrl)
                        } else {
                            KmpLogging.writeError(
                                "KMP_ESSENTIALS_EMAIL",
                                "No mail app available to handle mailto URL."
                            )
                        }
                    } else {
                        KmpLogging.writeError(
                            "KMP_ESSENTIALS_EMAIL",
                            "Failed to encode url message"
                        )
                    }
                }
            }
            KmpMainThread.runViaMainThread {
                if (promptInternal) {
                    try {
                        if (MFMailComposeViewController.canSendMail()) {
                            val mailController = MFMailComposeViewController().apply {
                                setToRecipients(arrayListOf(address, ccAddresses))
                                setMailComposeDelegate(mailDelegate)
                                setSubject(emailSubject)
                                setMessageBody(emailMessage, false)
                            }

                            UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
                                mailController,
                                true
                            ) {

                            }
                        } else {
                            throw Exception("Cannot send email, defaulting to prompt")
                        }
                    } catch (ex: Exception) {
                        runEmailAction()
                    }
                } else {
                    runEmailAction()
                }
            }
        }
    }
}
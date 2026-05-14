package com.ecobook.fcm

import com.ecobook.utils.SecureStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureNotificationInboxStore @Inject constructor(
    private val secureStorage: SecureStorage
) : NotificationInboxStore {

    override fun readInbox(): String? = secureStorage.getNotificationInboxPayload()

    override fun writeInbox(payload: String) {
        secureStorage.saveNotificationInboxPayload(payload)
    }
}

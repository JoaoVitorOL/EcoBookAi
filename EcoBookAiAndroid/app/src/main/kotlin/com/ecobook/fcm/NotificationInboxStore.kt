package com.ecobook.fcm

interface NotificationInboxStore {
    fun readInbox(): String?
    fun writeInbox(payload: String)
}

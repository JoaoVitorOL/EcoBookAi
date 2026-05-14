package com.ecobook.fcm

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class NotificationNavigationManager @Inject constructor() {

    private val _pendingDestination = MutableStateFlow<NotificationDestination?>(null)
    val pendingDestination: StateFlow<NotificationDestination?> = _pendingDestination.asStateFlow()

    fun queue(destination: NotificationDestination?) {
        if (destination != null) {
            _pendingDestination.value = destination
        }
    }

    fun consume(destination: NotificationDestination) {
        if (_pendingDestination.value == destination) {
            _pendingDestination.value = null
        }
    }
}

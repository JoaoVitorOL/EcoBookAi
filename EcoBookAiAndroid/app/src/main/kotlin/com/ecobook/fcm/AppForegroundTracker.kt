package com.ecobook.fcm

import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppForegroundTracker @Inject constructor() {

    private val startedActivities = AtomicInteger(0)

    fun onActivityStarted() {
        startedActivities.incrementAndGet()
    }

    fun onActivityStopped() {
        startedActivities.updateAndGet { current -> (current - 1).coerceAtLeast(0) }
    }

    fun isAppVisible(): Boolean = startedActivities.get() > 0
}

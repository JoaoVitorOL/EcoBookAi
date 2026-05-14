package com.ecobook

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.ecobook.fcm.AppForegroundTracker
import com.ecobook.fcm.FcmTokenSyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class EcoBookApp : Application() {

    @Inject
    lateinit var fcmTokenSyncManager: FcmTokenSyncManager

    @Inject
    lateinit var appForegroundTracker: AppForegroundTracker

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

                override fun onActivityStarted(activity: Activity) {
                    appForegroundTracker.onActivityStarted()
                }

                override fun onActivityResumed(activity: Activity) = Unit

                override fun onActivityPaused(activity: Activity) = Unit

                override fun onActivityStopped(activity: Activity) {
                    appForegroundTracker.onActivityStopped()
                }

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

                override fun onActivityDestroyed(activity: Activity) = Unit
            }
        )

        fcmTokenSyncManager.syncCachedOrCurrentTokenAsync()
    }
}

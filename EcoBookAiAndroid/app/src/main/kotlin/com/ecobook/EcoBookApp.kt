package com.ecobook

import android.app.Application
import com.ecobook.fcm.FcmTokenSyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class EcoBookApp : Application() {

    @Inject
    lateinit var fcmTokenSyncManager: FcmTokenSyncManager

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        fcmTokenSyncManager.syncCachedOrCurrentTokenAsync()
    }
}

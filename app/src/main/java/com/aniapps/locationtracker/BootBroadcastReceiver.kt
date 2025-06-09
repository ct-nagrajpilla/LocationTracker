package com.aniapps.locationtracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class BootBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Trigger your location work when the device is rebooted
            val locationWorkRequest = OneTimeWorkRequestBuilder<LocationWorker>()
                .build()

            // Enqueue the work
            WorkManager.getInstance(context).enqueue(locationWorkRequest)
        }
    }
}
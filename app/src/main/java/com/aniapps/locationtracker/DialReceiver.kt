package com.aniapps.locationtracker


import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

class DialReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.e("#I am in receiver", "Yes")
        if (intent?.action == "android.provider.Telephony.SECRET_CODE") {
            val enteredCode = intent.data?.host
           // val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
           // val savedCode = sharedPrefs.getString("secret_code", null)

            if (enteredCode == "943") {
                val packageManager = context.packageManager
                val componentName = ComponentName(context, MainActivity::class.java)
                Log.e("#This is the code", "$enteredCode")
                packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )

                val launchIntent = Intent(context, MainActivity::class.java)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            }
        }
    }
}
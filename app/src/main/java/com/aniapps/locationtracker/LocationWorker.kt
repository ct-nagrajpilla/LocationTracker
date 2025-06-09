package com.aniapps.locationtracker


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.aniapps.db.DBStatus
import com.aniapps.db.LocalDB
import com.aniapps.db.LocationDBPojo
import com.aniapps.db.Pref
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.Date

class LocationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)

        try {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    location?.let {
                        logLocation(it)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LocationWorker", "Error getting location: ${e.message}")
            return Result.retry()
        }

        return Result.success()
    }

    private fun logLocation(location: Location) {
        Log.e("#######", "Locationsss: ${location.latitude}, ${location.longitude}")

        val db = LocalDB.getInstance(applicationContext)
        Pref.getIn().timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        Pref.getIn().sequence += 1
        Pref.getIn().name = "NagRaj"
        //frequency
        Pref.getIn().frequency = 1 * 60 * 1000;
        val myPojo = LocationDBPojo(
            Pref.getIn().name, Pref.getIn().app_code, "" + location.latitude,
            "" + location.longitude, Pref.getIn().timestamp, "0",
            "" + Pref.getIn().sequence, "n"
        )
        db.insertLocation(myPojo, object : DBStatus {
            override fun onSuccess() {
                Log.e("#####", "Locaiton Iserted")
            }

            override fun onFailure() {
                Log.e("#####", "Locaiton Insertion Failed")
            }
        })
        // Replace with your server upload logic
    }
}

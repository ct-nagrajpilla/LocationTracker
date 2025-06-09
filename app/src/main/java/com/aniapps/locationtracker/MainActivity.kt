package com.aniapps.locationtracker

import LocData
import MyLocs
import android.Manifest
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.aniapps.callbackclient.APIResponse
import com.aniapps.callbackclient.RetrofitClient
import com.aniapps.db.DBStatus
import com.aniapps.db.LocalDB
import com.aniapps.db.LocationDBPojo
import com.aniapps.db.Pref
import com.aniapps.locationtracker.databinding.ActivityMainBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private val LOCATION_PERMISSION_REQUEST_CODE: Int = 100
    private val BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE: Int = 101
    var appLocationService: AppLocationService? = null
    lateinit var myDB: LocalDB
    val myList = ArrayList<String>()
    private lateinit var mMap: GoogleMap


    // Pair(LatLng(37.7749, -122.4194), "2024-12-28 14:00:00"),


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //  checkAndRequestLocationPermissions()
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.trackerMap) as SupportMapFragment
        mapFragment.getMapAsync(this@MainActivity)
        if (Pref.getIn().name == "") {
            binding.layInitial.visibility = View.VISIBLE
            binding.layDropdown.visibility = View.GONE
        } else {
          //  getRecords();
            getSummary()



        }
        binding.btnStart.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        if (Pref.getIn().app_code == "") {
                            Pref.getIn().app_code = UUID.randomUUID().toString()

                        }
                        Toast.makeText(
                            this@MainActivity,
                            "" + binding.etName.text.toString() + "!!  Service Started",
                            Toast.LENGTH_SHORT
                        ).show()
                        if (isOnline(this@MainActivity)) {
                            myDB = LocalDB(this@MainActivity);
                            startJourney()
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Please check your network connection",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

        }


    }


    private fun getSummary() {
        val timeStamp: String
        appLocationService =
            AppLocationService(this@MainActivity)
        val latitude: String = "" + appLocationService!!.latitude
        val longitude: String = "" + appLocationService!!.longitude
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        timeStamp = dateFormat.format(Date())
        val params = HashMap<String, String>()
        params["action"] = "get_summary"
        params["timestamp"] = timeStamp
        params["lat"] = latitude
        params["long"] = longitude
        params["name"] = Pref.getIn().name

        RetrofitClient.getInstance()
            .doBackProcess(this@MainActivity, "", params, object : APIResponse {
                override fun onSuccess(res: String?) {
                    try {
                        val job = JSONObject(res)
                        Log.e("####", "Res : $res");
                        val status = job.getString("status")
                        if (status.equals("ok")) {
                            Log.e("#####", "RES")

                            val namesndates = Gson().fromJson(res, UserSummary::class.java)

                            binding.layInitial.visibility = View.GONE
                            binding.layDropdown.visibility = View.VISIBLE
                            val myNames=ArrayList<String>()

                            namesndates.names?.forEach {
                                if (it != null) {
                                    it.user?.let { it1 -> myNames.add(it1) }
                                }
                            }

                          //  val names = listOf("Location A", "Location B", "Location C", "Location D", "Location E")


                            val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, myNames)

                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            binding.spName.adapter = adapter
                            binding.spName.setSelection(0)
                            binding.spName.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                override fun onNothingSelected(parent: AdapterView<*>?) {

                                }

                                override fun onItemSelected(
                                    parent: AdapterView<*>?,
                                    view: View?,
                                    position: Int,
                                    id: Long
                                ) {
                                    val selectedItem = parent?.getItemAtPosition(position).toString()
                                    Log.e("Selcted Item", "Selected item: $selectedItem")
                                    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                    val timeStamp = currentDate.format(Date())

                                    getRecords(selectedItem,timeStamp)

                                }

                            }
                            val myDates=ArrayList<String>()

                            namesndates.dates?.forEach {
                                if (it != null) {
                                    it.date?.let { it1 -> myDates.add(it1) }
                                }
                            }


                            val datesadapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, myDates)

                            datesadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            binding.spDate.adapter = datesadapter
                            binding.spDate.setSelection(0)
                            binding.spDate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                override fun onNothingSelected(parent: AdapterView<*>?) {

                                }

                                override fun onItemSelected(
                                    parent: AdapterView<*>?,
                                    view: View?,
                                    position: Int,
                                    id: Long
                                ) {
                                    val selectedItem = parent?.getItemAtPosition(position).toString()
                                    Log.e("Selcted Item", "Selected item: $selectedItem")
                                    getRecords(myNames.get(0),selectedItem)
                                   //
                                }

                            }


                        }
                    } catch (e: Exception) {
                        e.printStackTrace()

                    }
                }

                override fun onFailure(res: String?) {

                }

            })

    }

    private fun getRecords(name:String, date: String) {
        val timeStamp: String
        appLocationService =
            AppLocationService(this@MainActivity)
        val latitude: String = "" + appLocationService!!.latitude
        val longitude: String = "" + appLocationService!!.longitude
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        timeStamp = dateFormat.format(Date())
        val params = HashMap<String, String>()
       // params["action"] = "get_summary"
        params["action"] = "get_records_by_name_and_date"
        params["timestamp"] = timeStamp
        params["lat"] = latitude
        params["long"] = longitude
        params["name"] = name
        params["date"] = date

        RetrofitClient.getInstance()
            .doBackProcess(this@MainActivity, "", params, object : APIResponse {
                override fun onSuccess(res: String?) {
                    try {
                        val job = JSONObject(res)
                        Log.e("####", "Res : $res");
                        val status = job.getString("status")
                        if (status.equals("ok")) {
                            //val userRecords = Gson().fromJson(res, UserRecords::class.java)
                            if (res != null) {
                                mapwithMarker(res)
                            //    refreshMapWithSelectedDate(selectedItem)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()

                    }
                }

                override fun onFailure(res: String?) {

                }

            })

    }


    fun mapwithMarker(data: String) {
        // Initialize Gson
        val gson = Gson()

        try {
            // Parse the JSON string into the Kotlin data class
           // val jsonString = loadJSONFromAsset("datewiselocs.json")
            val response = gson.fromJson(data, UserRecords::class.java)


            val myDates = ArrayList<String>()

            // List to store map data
            val mapData = mutableListOf<Map<String, Any>>()

            // Process the JSON data
            for (location in response.records!!) {
                // Process each date for the current location


                    // Process each data entry for the current date


                        val lat = location?.lat
                        val lng = location?.jsonMemberLong
                        val timestamp = location?.usertime

                        Log.e("LOCS", "Lat: $lat, Long: $lng, Timestamp: $timestamp")

                        // Add to mapData list
                        val dataMap = mutableMapOf<String, Any>()
                        dataMap["lat"] = (lat ?: return)
                        dataMap["lng"] = (lng ?: return)
                        dataMap["date"] = (timestamp ?: return)
                        mapData.add(dataMap)


            }



            // Add markers to the map
            /*  if (::googleMap.isInitialized) {*/
            for (data in mapData) {
                val position = LatLng(data["lat"] as Double, data["lng"] as Double)
                val date = data["date"] as String

                mMap.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(date)
                )
            }
            /* } else {
                 println("Google Map is not initialized!")
             }*/
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
        }
    }

    fun refreshMapWithSelectedDate(selectedDate: String) {
        // Clear the existing markers
        runOnUiThread {
            mMap.clear()
            // Filter the data for the selected date
            val selectedLocationData = mutableListOf<LocData>()
            val jsonString = loadJSONFromAsset("datewiselocs.json")
            val gson = Gson()
            val response = gson.fromJson(jsonString, MyLocs::class.java)
            // Iterate through locations and dates to find matching date
            for (location in response.locations) {
                for (dateData in location.dates) {
                    if (dateData.date == selectedDate) {
                        selectedLocationData.clear()
                        selectedLocationData.addAll(dateData.data)
                        Log.e("#####", "Dates Data $dateData.data")
                    }
                }
            }

            // Add new markers to the map based on the selected date
            for (data in selectedLocationData) {
                val position = LatLng(data.lat, data.long)
                Log.e("#####", "Dates Pos $position")
                mMap.addMarker(MarkerOptions().position(position).title(data.timestamp))
            }
        }
    }

    /*  fun mapLocater(name:String, date:String) {
          // Initialize Gson
          val gson = Gson()

          try {
              // Parse the JSON string
              val jsonString = loadJSONFromAsset("datewiselocs.json")

              val response = gson.fromJson(jsonString, DateLocTracker::class.java)

              // Prepare map data
              val mapData = mutableListOf<Map<String, Any>>()

              // Process the JSON data
              response.locations?.forEach { location ->
                  val date = location.date
                  println("Date: $date")

                  location.data?.forEach { data ->
                      val lat = data.lat
                      val lng = data.lng
                      val timestamp = data.timestamp

                      println("Lat: $lat, Long: $lng, Timestamp: $timestamp")

                      // Add to mapData list
                      if (timestamp != null) {
                          mapData.add(
                              mapOf(
                                  "lat" to lat,
                                  "lng" to lng,
                                  "date" to timestamp
                              )
                          )
                      }
                  }
              }

              // Add markers to the map
              googleMap?.let { map ->
                  mapData.forEach { data ->
                      val position = LatLng(data["lat"] as Double, data["lng"] as Double)
                      val date = data["date"] as String

                      map.addMarker(MarkerOptions().position(position).title(date))
                  }
              } ?: println("Google Map is not initialized!")
          } catch (e: JsonSyntaxException) {
              e.printStackTrace()
          }
      }*/

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        appLocationService =
            AppLocationService(this@MainActivity)
        val latitude: String = "" + appLocationService!!.latitude
        val longitude: String = "" + appLocationService!!.longitude
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val timeStamp = dateFormat.format(Date())
        val locations = listOf(
            Pair(LatLng(latitude.toDouble(), longitude.toDouble()), timeStamp)
        )
        for (location in locations) {
            val latLng = location.first
            val timestamp = location.second
            mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Timestamp: $timestamp")
            )
        }
        // Move the camera to the first location
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locations[0].first, 10f))
      //  mapwithMarker("")
    }

    /* override fun onMapReady(googleMap: GoogleMap) {
         mMap = googleMap

         // Load JSON data
         val json = loadJSONFromAsset("datewiselocs.json")
         if (json != null) {
             val gson = Gson()
             val listType = object : TypeToken<List<LocationData>>() {}.type
             val locationDataList: List<LocationData>? = try {
                 gson.fromJson(json, listType)
             } catch (e: Exception) {
                 e.printStackTrace()
                 null
             }

             if (locationDataList != null) {
                 // Iterate through dates and add markers for valid locations
                 for (locationData in locationDataList) {
                     for (entry in locationData.data) {
                         if (entry.lat != 0.0 && entry.long != 0.0 && entry.timestamp.isNotEmpty()) {
                             val position = LatLng(entry.lat, entry.long)
                             val markerOptions = MarkerOptions()
                                 .position(position)
                                 .title("Timestamp: ${entry.timestamp}")

                             // Add marker to the map
                             mMap.addMarker(markerOptions)
                         } else {
                             // Log or handle invalid entries if necessary
                             Log.e("MapMarker", "Invalid location data: $entry")
                         }
                     }
                 }

                 // Move camera to the first valid marker
                 val firstValidEntry = locationDataList.flatMap { it.data }
                     .firstOrNull { it.lat != 0.0 && it.long != 0.0 }

                 if (firstValidEntry != null) {
                     val firstPosition = LatLng(firstValidEntry.lat, firstValidEntry.long)
                     mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstPosition, 10f))
                 }
             } else {
                 Log.e("MapMarker", "Failed to parse location data")
             }
         }
     }*/

    /*override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Load JSON data from assets
        val json = loadJSONFromAsset("datewiselocs.json")
        if (json != null) {
            val gson = Gson()
            val listType = object : TypeToken<List<LocationData>>() {}.type
            val locationDataList: List<LocationData> = gson.fromJson(json, listType)

            for (locationData in locationDataList) {
                Log.e("JSONParsing", "Date: ${locationData.date}")
                for (entry in locationData.data) {
                    Log.e("JSONParsing", "Lat: ${entry.lat}, Long: ${entry.long}, Timestamp: ${entry.timestamp}")
                }
            }

            // Iterate through dates and add markers for each location
            for (locationData in locationDataList) {
                for (entry in locationData.data) {
                    val position = LatLng(entry.lat, entry.long)
                    val markerOptions = MarkerOptions()
                        .position(position)
                        .title("Timestamp: ${entry.timestamp}")

                    // Add marker to the map
                    mMap.addMarker(markerOptions)
                }
            }

            // Move camera to the first marker
            if (locationDataList.isNotEmpty() && locationDataList[0].data.isNotEmpty()) {
                val firstPosition = LatLng(
                    locationDataList[0].data[0].lat,
                    locationDataList[0].data[0].long
                )
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstPosition, 10f))
            }
        }
    }*/

    /*override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Load JSON data
        val jsonData = loadJSONFromAsset("mylocs.json")

        // Parse JSON and add markers
        if (jsonData != null) {
            val locationsArray = JSONArray(jsonData)
            for (i in 0 until locationsArray.length()) {
                val person = locationsArray.getJSONObject(i)
                val dates = person.getJSONArray("dates")
                for (j in 0 until dates.length()) {
                    val dateEntry = dates.getJSONObject(j)
                    val data = dateEntry.getJSONArray("data")
                    for (k in 0 until data.length()) {
                        val location = data.getJSONObject(k)
                        val lat = location.getDouble("lat")
                        val long = location.getDouble("long")
                        val timestamp = location.getString("timestamp")

                        // Add marker to the map
                        val latLng = LatLng(lat, long)
                        mMap.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title("$timestamp")
                        )
                    }
                }
            }

            // Move camera to the first location
            val firstLocation = LatLng(37.7749, -122.4194) // Example: San Francisco
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 10f))
        }
    }*/

    private fun loadJSONFromAsset(fileName: String): String? {
        return try {
            val inputStream = assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            var line: String? = reader.readLine()
            while (line != null) {
                stringBuilder.append(line)
                line = reader.readLine()
            }
            reader.close()
            stringBuilder.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }



    private fun startJourney() {
        val timeStamp: String
        appLocationService =
            AppLocationService(this@MainActivity)
        val latitude: String = "" + appLocationService!!.latitude
        val longitude: String = "" + appLocationService!!.longitude
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        timeStamp = dateFormat.format(Date())
        val params = HashMap<String, String>()
        params["timestamp"] = timeStamp
        params["lat"] = latitude
        params["long"] = longitude
        params["user"] = binding.etName.toString()
        params["system_info"] =
            "MANUFACTURER:- ${Build.MANUFACTURER} MODEL:- ${Build.MODEL}"
        params["action"] = "initial"

        RetrofitClient.getInstance()
            .doBackProcess(this@MainActivity, "", params, object : APIResponse {
                override fun onSuccess(res: String) {
                    try {
                        val job = JSONObject(res)
                        Log.e("####", "Res : $res");
                        val status = job.getString("status")
                        if (status.equals("ok")) {
                            Pref.getIn().name = binding.etName.text.toString()
                            //frequency
                            Pref.getIn().frequency = 15 * 60 * 1000;
                            // Pref.getIn().frequency = job.getInt("freequency")
                            Pref.getIn().service = job.getString("service")
                            Pref.getIn().deviceDetails =
                                "${Build.MANUFACTURER} : ${Build.MODEL} : ${Build.MODEL}"

                            val db = LocalDB.getInstance(this@MainActivity)
                            Pref.getIn().timestamp =
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
                            Pref.getIn().sequence += 1
                            var offline = "online";
                            if (isOnline(this@MainActivity)) {
                                offline = "online"
                            } else {
                                offline = "offline"
                            }
                            //frequency
                            Pref.getIn().frequency = 1 * 60 * 1000;
                            val myPojo = LocationDBPojo(
                                Pref.getIn().name, Pref.getIn().app_code, "" + latitude,
                                "" + longitude, Pref.getIn().timestamp, offline,
                                "" + Pref.getIn().sequence, Pref.getIn().deviceDetails
                            )
                            db.insertLocation(myPojo, object : DBStatus {
                                override fun onSuccess() {
                                    Log.e("#####", "Locaiton Inserted & Started")
                                    val constraints = androidx.work.Constraints.Builder()
                                        .setRequiresBatteryNotLow(true)
                                        .build()
                                    val locationWorkRequest =
                                        PeriodicWorkRequestBuilder<LocationWorker>(
                                            15,
                                            TimeUnit.MINUTES // Minimum interval for periodic work
                                        ).addTag("LocationTracking")
                                            .setConstraints(constraints)
                                            .build()
                                    WorkManager.getInstance(this@MainActivity)
                                        .enqueue(locationWorkRequest)
                                    //  hideAppIcon()
                                    finishAndRemoveTask()
                                }

                                override fun onFailure() {
                                    Log.e("#####", "Locaiton Insertion Failed")
                                }
                            })

                            //myLoactions("nagraj","")
                            //mapLocater()
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Tracking stopped",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onFailure(res: String) {
                    Toast.makeText(
                        this@MainActivity,
                        "Tracking Failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }


    private fun myLoactions(name: String, date: String) {
        val params = HashMap<String, String>()
        params["user"] = binding.etName.toString()
        params["action"] = "get_records_by_name"
        params["name"] = name
        if (date.equals("")) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            params["date"] = dateFormat.format(Date())
        } else {
            params["date"] = date
        }


        RetrofitClient.getInstance()
            .doBackProcess(this@MainActivity, "", params, object : APIResponse {
                override fun onSuccess(res: String) {
                    try {
                        val job = JSONObject(res)
                        Log.e("####", "Loc Res : $res");
                        val status = job.getString("status")
                        if (status.equals("ok")) {

                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Tracking stopped",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onFailure(res: String) {
                    Toast.makeText(
                        this@MainActivity,
                        "Tracking Failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    class LocationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

        override fun doWork(): Result {
            val fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(applicationContext)

            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    location?.let {
                        logLocation(it)
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

            Pref.getIn().timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
            Pref.getIn().sequence += 1
            var offline = "online";
            if (isOnline(applicationContext)) {
                offline = "online"
            } else {
                offline = "offline"
            }
            //frequency
            Pref.getIn().frequency = 1 * 60 * 1000;
            val myPojo = LocationDBPojo(
                Pref.getIn().name, Pref.getIn().app_code, "" + location.latitude,
                "" + location.longitude, Pref.getIn().timestamp, offline,
                "" + Pref.getIn().sequence, Pref.getIn().deviceDetails
            )
            LocalDB(applicationContext).insertLocation(myPojo, object : DBStatus {
                override fun onSuccess() {
                    Log.e("#####", "Locaiton inserted in workerthread")
                    LocalDB.getInstance(applicationContext)
                        .getAllLocations { locs: List<LocationDBPojo?> ->
                            if (locs.isNotEmpty()) {
                                locs[0]?.let { send(it) }
                            }
                        }
                }

                override fun onFailure() {
                    Log.e("#####", "Locaiton Insertion Failed")
                }
            })
            // Replace with your server upload logic
        }

        public fun send(locationPojo: LocationDBPojo) {
            RetrofitClient.getInstance()
                .doBackProcess(
                    applicationContext,
                    "",
                    makeLocationUpdateRequest(locationPojo),
                    object : APIResponse {
                        override fun onSuccess(res: String) {
                            try {
                                val job = JSONObject(res)
                                Log.e("####", "Res Loc : $res");
                                val status = job.getString("status")
                                if (status == "ok") {
                                    LocalDB.getInstance(applicationContext)
                                        .deleteLoc(locationPojo.getTimestamp(), object : DBStatus {
                                            override fun onSuccess() {
                                                LocalDB.getInstance(applicationContext)
                                                    .getAllLocations { locs: List<LocationDBPojo> ->
                                                        if (locs.isNotEmpty()) {
                                                            Handler(Looper.getMainLooper())
                                                                .postDelayed(
                                                                    {
                                                                        if (locs.isNotEmpty()) {
                                                                            send(locs[0])
                                                                        }
                                                                    },
                                                                    150
                                                                )
                                                        }
                                                    }
                                            }

                                            override fun onFailure() {
                                            }
                                        })
                                }
                            } catch (e: java.lang.Exception) {
                                e.printStackTrace()
                            }
                        }

                        override fun onFailure(res: String?) {

                        }
                    })


        }

        private fun makeLocationUpdateRequest(locationPojo: LocationDBPojo): java.util.HashMap<String, String> {
            val params = java.util.HashMap<String, String>()
            params["action"] = "track"
            params["lat"] = "" + locationPojo.getLatitude()
            params["long"] = "" + locationPojo.getLongitude()
            params["user"] = "" + locationPojo.getName()
            params["timestamp"] = "" + locationPojo.getTimestamp()
            params["networkstate"] = "" + locationPojo.getOffline()
            params["sequence"] = "" + locationPojo.getSequence()
            params["version_code"] = "1"
            params["device_details"] = Pref.getIn().deviceDetails
            params["device_id"] = "" + Pref.getIn().app_code
            return params
        }

        fun isOnline(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork != null) {
                val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                return (networkCapabilities != null) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
            return false
        }
    }

    private fun hideAppIcon() {
        val packageManager = packageManager
        val componentName = ComponentName(this, MainActivity::class.java)
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        val activeNetwork = connectivityManager.activeNetwork
        if (activeNetwork != null) {
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            return (networkCapabilities != null) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        return false
    }


    override fun onResume() {
        super.onResume()
        checkAndRequestLocationPermissions()
    }

    private fun checkAndRequestLocationPermissions() {
        // Android 10+ (API 29 and above)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), LOCATION_PERMISSION_REQUEST_CODE
            )
        } else if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestBackgroundLocationPermission()
        }
    }

    private fun requestBackgroundLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        ) {
            AlertDialog.Builder(this)
                .setTitle("Background Location Permission")
                .setMessage("This app requires background location access to track your location even when the app is closed.")
                .setPositiveButton("Allow") { dialog, which ->
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf<String>(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                        BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
                .setNegativeButton("Cancel", null)
                .create()
                .show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions!!, grantResults)

        if (requestCode == BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Location permissions granted.")
                checkAndRequestLocationPermissions()
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                ) {
                    openAppSettings()
                } else {
                    Toast.makeText(this, "Location permissions are required.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    }

    private fun openAppSettings() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Location permissions are required for this app to work. Please grant permissions in the app settings.")
            .setPositiveButton("Go to Settings",
                DialogInterface.OnClickListener { dialog, which ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.setData(uri)
                    startActivity(intent)
                })
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    fun setDataToSpinner(spn: Spinner, lst: List<String?>?) {
        val adapter: CustomAdapter = CustomAdapter(
            this,
            android.R.layout.simple_spinner_item, lst
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spn.adapter = adapter
        spn.setOnTouchListener { v, event ->
            spn.isFocusableInTouchMode = true
            spn.requestFocus()
            //  hideKeyboard()
            false
        }
    }
}

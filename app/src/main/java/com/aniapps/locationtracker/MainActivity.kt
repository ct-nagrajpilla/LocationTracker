package com.aniapps.locationtracker

import LocData
import MyLocs
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
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
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aniapps.callbackclient.APIResponse
import com.aniapps.callbackclient.RetrofitClient
import com.aniapps.db.DBStatus
import com.aniapps.db.LocalDB
import com.aniapps.db.LocationDBPojo
import com.aniapps.db.Pref
import com.aniapps.locationtracker.databinding.ActivityMainBinding
import com.aniapps.locationtracker.databinding.CustomAlertDialogueBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private val LOCATION_PERMISSION_REQUEST_CODE: Int = 100
    private val BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE: Int = 101
    var appLocationService: AppLocationService? = null
    lateinit var myDB: LocalDB
    var location: Location? = null
    private var fusedLocationClient: FusedLocationProviderClient ?= null
    private lateinit var settingsClient: SettingsClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationSettingsRequest: LocationSettingsRequest
    private lateinit var mMap: GoogleMap
    private var permissionDialog: Dialog? = null
    private var isSpinnersInitialized = false
    private var isFetchingRecords = false
    private var hasFetchedLocation = false
    private var isLocationPromptVisible = false
    private val markers = mutableListOf<Marker>()
    private var isPermissionDialogShowing = false

    private val gpsSwitchStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                val locationManager = context?.getSystemService(LOCATION_SERVICE) as LocationManager
                val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

                if (isGPSEnabled.not()) {
                    checkLocationSettingsAndStart()
                } else {
                    fusedLocationClient = null
                    fetchCurrentLocation()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Pref.getIn().name == "") {
            binding.layInitial.visibility = View.VISIBLE
            binding.layDropdown.visibility = View.GONE
        }
        setClickListener()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setClickListener() {
        binding.btnStart.setOnTouchListener { view, event ->
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                openAppSettings()
                return@setOnTouchListener true
            }

            if (isLocationEnabled().not()) {
                checkLocationSettingsAndStart()
                return@setOnTouchListener true
            }

            if (binding.etName.text.isNullOrBlank()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setOnTouchListener true
            }

            Toast.makeText(this, "${binding.etName.text}!! Service Started", Toast.LENGTH_SHORT).show()
            if (isOnline(this)) {
                myDB = LocalDB(this)
                startJourney()
            } else {
                Toast.makeText(this, "Please check your network connection", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }


    internal fun getSummary() {
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
                        val job = JSONObject(res?:"")
                        Log.e("####", "Res : $res");
                        val status = job.getString("status")
                        if (status.equals("ok")) {
                            Log.e("#####", "RES")

                            val namesndates = Gson().fromJson(res, UserSummary::class.java)

                            binding.layInitial.visibility = View.GONE
                            binding.layDropdown.visibility = View.VISIBLE
                            val myNames=ArrayList<String>()

                            namesndates.names?.forEach {
                                it?.user?.let { it1 -> myNames.add(it1) }
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
                                    triggerGetRecordsIfReady(selectedItem, timeStamp)
//                                    getRecords(selectedItem,timeStamp)
                                }

                            }
                            val myDates=ArrayList<String>()

                            namesndates.dates?.forEach {
                                it?.date?.let { it1 -> myDates.add(it1) }
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
                                    triggerGetRecordsIfReady(myNames[0], selectedItem)
//                                    getRecords(myNames.get(0),selectedItem)
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
        isFetchingRecords = true

        val timeStamp: String
        appLocationService = AppLocationService(this@MainActivity)
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
                    isFetchingRecords = false
                    try {
                        val job = JSONObject(res)
                        Log.e("####", "Res : $res");
                        val status = job.getString("status")
                        if (status.equals("ok")) {
                            //val userRecords = Gson().fromJson(res, UserRecords::class.java)
                            if (res != null) {
                                mapwithMarker(res)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()

                    }
                }

                override fun onFailure(res: String?) {
                    isFetchingRecords = false
                }

            })

    }

    internal fun triggerGetRecordsIfReady(selectedName:String, selectedDate: String) {
        if (selectedName.isNotBlank() && selectedDate.isNotBlank()) {
            if (isSpinnersInitialized.not()) {
                isSpinnersInitialized = true
                return
            }

            if (isFetchingRecords.not()) {
                getRecords(selectedName, selectedDate)
            }
        }
    }

    fun mapwithMarker(data: String) {
        val gson = Gson()

        try {
            val response = gson.fromJson(data, UserRecords::class.java)
            val mapData = mutableListOf<Map<String, Any>>()

            // Clear existing markers from map and list
            for (marker in markers) {
                marker.remove()
            }
            markers.clear()
            mMap.clear()

            for (location in response.records.orEmpty()) {
                val latStr = location?.lat
                val lngStr = location?.jsonMemberLong
                val timestamp = location?.usertime

                Log.e("LOCS", "Lat: $latStr, Long: $lngStr, Timestamp: $timestamp")

                val lat = latStr?.toDoubleOrNull()
                val lng = lngStr?.toDoubleOrNull()

                if (lat != null && lng != null && timestamp != null) {
                    val dataMap = mutableMapOf<String, Any>()
                    dataMap["lat"] = lat
                    dataMap["lng"] = lng
                    dataMap["date"] = timestamp
                    mapData.add(dataMap)
                }
            }

            if (mapData.isEmpty()) {
                Toast.makeText(this, "No location data for selected date", Toast.LENGTH_SHORT).show()
                return
            }

            val builder = LatLngBounds.Builder()
            for (data in mapData) {
                val lat = data["lat"] as Double
                val lng = data["lng"] as Double
                val date = data["date"] as? String ?: "Unknown Date"

                val position = LatLng(lat, lng)
                builder.include(position)

                val marker = mMap.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(date)
                )
                marker?.let { markers.add(it) }
            }

            mMap.setOnMapLoadedCallback {
                if (mapData.isNotEmpty()) {
                    val bounds = builder.build()
                    val padding = 100
                    val cu = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                    mMap.animateCamera(cu)
                }
            }

        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to parse location data", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
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
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true
        mMap.isBuildingsEnabled = true

        appLocationService = AppLocationService(this@MainActivity)

        val latitude = location?.latitude ?: appLocationService?.latitude ?: 0.0
        val longitude = location?.longitude ?: appLocationService?.longitude ?: 0.0

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val timeStamp = dateFormat.format(Date())

        val latLng = LatLng(latitude, longitude)
        val marker = mMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Timestamp: $timeStamp")
        )
        marker?.let { markers.add(it) }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))

        // Optionally: Load default data
        // mapwithMarker(jsonDataFromToday)
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
//                            Pref.getIn().frequency = 15 * 60 * 1000;
                            // Pref.getIn().frequency = job.getInt("freequency")
                            Pref.getIn().service = job.getString("service")
                            Pref.getIn().deviceDetails =
                                "${Build.MANUFACTURER} : ${Build.MODEL} : ${Build.MODEL}"

                            val db = LocalDB.getInstance(this@MainActivity)
                            Pref.getIn().timestamp =
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
                            Pref.getIn().sequence += 1
                            var offline = "online"
                            offline = if (isOnline(this@MainActivity)) {
                                "online"
                            } else {
                                "offline"
                            }
                            //frequency
                            Pref.getIn().frequency = 1 * 60 * 1000
                            val myPojo = LocationDBPojo(
                                Pref.getIn().name, Pref.getIn().app_code, "" + latitude,
                                "" + longitude, Pref.getIn().timestamp, offline,
                                "" + Pref.getIn().sequence, Pref.getIn().deviceDetails
                            )
                            db.insertLocation(myPojo, object : DBStatus {
                                override fun onSuccess() {
                                    Log.e("#####", "Locaiton Inserted & Started")
                                    enqueueOneTimeWork()
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

    fun enqueueOneTimeWork() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<LocationWorker>()
            .setInitialDelay(0, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "LOCATION_WORK",
            ExistingWorkPolicy.KEEP, // or KEEP, depending on your logic
            workRequest
        )
    }


    private fun myLoactions(name: String, date: String) {
        val params = HashMap<String, String>()
        params["user"] = binding.etName.toString()
        params["action"] = "get_records_by_name"
        params["name"] = name
        if (date == "") {
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
                        Log.e("####", "Loc Res : $res")
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

    class LocationWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        private val fusedLocationClient: FusedLocationProviderClient by lazy {
            LocationServices.getFusedLocationProviderClient(context)
        }

        override suspend fun doWork(): Result {

            return try {
                if (isGPSEnabled().not()) {
                    Log.w("LOC_WORKER", "Missing location permissions")
                    return Result.retry()
                }

                val location = fetchLocation()
                if (location == null) {
                    Log.e("LocationWorker", "Could not fetch location")
                    return Result.retry()
                }
                logLocation(location)
                scheduleNextRun()

                Result.success()
            } catch (e: Exception) {
                Log.e("LocationWorker", "Error: ${e.message}")
                Result.retry()
            }
        }

        fun isGPSEnabled(): Boolean {
            val locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }

        private suspend fun fetchLocation(): Location? {
            return try {
                // First try to get current location
                getCurrentLocation()?.let { return it }

                // If current location fails, try last known location
                getLastKnownLocation()?.let { return it }
                null
            } catch (e: Exception) {
                null
            }
        }

        private suspend fun getCurrentLocation(): Location? {
            return try {
                suspendCoroutine { continuation ->
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        CancellationTokenSource().token
                    ).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            continuation.resume(task.result)
                        } else {
                            continuation.resume(null)
                        }
                    }
                }
            } catch (e: Exception) {
                null
            }
        }

        private suspend fun getLastKnownLocation(): Location? {
            return try {
                suspendCoroutine { continuation ->
                    fusedLocationClient.lastLocation.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            continuation.resume(task.result)
                        } else {
                            continuation.resume(null)
                        }
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
        private fun scheduleNextRun() {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<LocationWorker>()
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "LOCATION_WORK",
                ExistingWorkPolicy.REPLACE, // or KEEP, depending on your logic
                workRequest
            )
        }

        private fun logLocation(location: Location?) {
            Log.e("#######", "Locationsss: ${location?.latitude}, ${location?.longitude}")

            Pref.getIn().timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
            Pref.getIn().sequence += 1
            var offline = "online"
            offline = if (isOnline(applicationContext)) {
                "online"
            } else {
                "offline"
            }
            //frequency
            Pref.getIn().frequency = 1 * 60 * 1000
            val myPojo = LocationDBPojo(
                Pref.getIn().name, Pref.getIn().app_code, "" + location?.latitude,
                "" + location?.longitude, Pref.getIn().timestamp, offline,
                "" + Pref.getIn().sequence, Pref.getIn().deviceDetails
            )

            LocalDB(applicationContext).insertLocation(myPojo, object : DBStatus {
                override fun onSuccess() {
                    Log.e("#####", "Locaiton inserted in workerthread")


                    Log.d("DB_TEST", "device is $offline")
                    Log.d("DB_TEST", "Inserted location at ${Date()}")

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

        fun send(locationPojo: LocationDBPojo) {
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
                return (networkCapabilities != null) && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
            return false
        }
    }

/*
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
*/

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
        val connectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        val activeNetwork = connectivityManager.activeNetwork
        if (activeNetwork != null) {
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            return (networkCapabilities != null) && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        return false
    }


    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        registerReceiver(gpsSwitchStateReceiver, intentFilter)
        if (hasFetchedLocation.not()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                if (isLocationEnabled()) {
                    fusedLocationClient = null
                    fetchCurrentLocation() // Force fresh fetch
                } else {
                    checkLocationSettingsAndStart()
                }
            } else {
                checkAndRequestLocationPermissions()
            }
        }

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


    fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun setupLocationRequest() {
        settingsClient = LocationServices.getSettingsClient(this)

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10_000L
        ).apply {
            setMinUpdateIntervalMillis(5_000L)
        }.build()


        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        locationSettingsRequest = builder.build()
    }

    internal fun checkLocationSettingsAndStart() {
        if (isLocationPromptVisible) return
        setupLocationRequest()

        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener {
                // All location settings are satisfied
                if (Pref.getIn().name.isNullOrBlank().not()) {
                    getSummary()
                } else {
                    fusedLocationClient = null
                    fetchCurrentLocation()
                }
            }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                        if (isLocationPromptVisible.not()) {
                            locationSettingLauncher.launch(intentSenderRequest)
                        }
                        isLocationPromptVisible = true
                    } catch (sendEx: IntentSender.SendIntentException) {
                        isLocationPromptVisible = false
                        sendEx.printStackTrace()
                    }
                } else {
                    isLocationPromptVisible = false
                    binding.trackerMap.visibility = View.GONE
                    Toast.makeText(this, "Cannot access location", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private val locationSettingLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        isLocationPromptVisible = false
        if (result.resultCode == RESULT_OK) {
            fusedLocationClient = null
            fetchCurrentLocation()
        } else {
            binding.trackerMap.visibility = View.GONE
            checkLocationSettingsAndStart()
        }
    }

    fun fetchCurrentLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        lifecycleScope.launch {
            try {
                val hasFine = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val hasCoarse = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                if (hasFine || hasCoarse) {
                    fusedLocationClient?.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        CancellationTokenSource().token
                    )?.addOnSuccessListener { location ->
                        if (location != null) {
                            onLocationReceived(location)
                        } else {
                            // fallback: get fresh location
                            getFreshLocation()
                        }
                    }?.addOnFailureListener {
                        getFreshLocation()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error getting location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getFreshLocation() {
        val hasFine = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        try {
            if (hasFine || hasCoarse) {
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                    .setMinUpdateIntervalMillis(500)
                    .build()

                val callback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        fusedLocationClient?.removeLocationUpdates(this)
                        val location = locationResult.lastLocation
                        if (location != null) {
                            onLocationReceived(location)
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Location unavailable",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                fusedLocationClient?.requestLocationUpdates(
                    locationRequest,
                    callback,
                    Looper.getMainLooper()
                )
            }
        } catch (e: Exception) {
            Toast.makeText(this@MainActivity, "Error getting location", Toast.LENGTH_SHORT).show()
        }
    }

    internal fun onLocationReceived(location: Location) {
        hasFetchedLocation = true
        binding.trackerMap.visibility = View.VISIBLE
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.trackerMap) as SupportMapFragment
        mapFragment.getMapAsync(this@MainActivity)
        this@MainActivity.location = location
        if (Pref.getIn().name.isNullOrBlank().not()) {
            getSummary()
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Location permissions granted.")
                checkAndRequestLocationPermissions()
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
                ) {
                    openAppSettings()
                }
                /*if (!ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                ) {
                    openAppSettings()
                } else {
                    Toast.makeText(this, "Location permissions are required.", Toast.LENGTH_SHORT).show()
                }*/
            }
        }

    }


    @SuppressLint("ClickableViewAccessibility")
    private fun openAppSettings() {
        if (isPermissionDialogShowing) return
        isPermissionDialogShowing = true

        // Inflate view binding for custom dialog
        val dialogBinding = CustomAlertDialogueBinding.inflate(layoutInflater)

        val dialog = Dialog(this, R.style.ThemeDialogCustom).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(dialogBinding.root)
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setCancelable(false)
        }

        dialogBinding.alertTitle.text = getString(R.string.permission_required)
        dialogBinding.alertText.text =
            getString(R.string.location_permissions_are_required_for_this_app_to_work_please_grant_permissions_in_the_app_settings)

        dialogBinding.btnOk.setOnTouchListener { view, event ->
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
            dialog.dismiss()
            isPermissionDialogShowing = false
            true
        }

        dialog.setOnDismissListener {
            isPermissionDialogShowing = false
        }

        dialog.show()
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(gpsSwitchStateReceiver)
        permissionDialog?.dismiss()
        permissionDialog = null
    }
}

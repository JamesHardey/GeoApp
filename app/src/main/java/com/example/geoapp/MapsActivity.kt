package com.example.geoapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SearchView
import android.widget.SearchView.OnQueryTextListener
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.geoapp.databinding.ActivityMapsBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.IOException

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var locationManager: LocationManager
    private val PERMISSIONS_FINE_LOCATION: Int = 99
    private val REQUEST_CHECK_SETTINGS = 101
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var searchPlaces: SearchView
    private lateinit var myLocation: FloatingActionButton
    private lateinit var locationRequest: LocationRequest
    private lateinit var addressList: List<Address?>
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var markerOptions: MarkerOptions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        searchPlaces = binding.searchPlaces
        myLocation = binding.myLocation

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        markerOptions = MarkerOptions()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        addressList = ArrayList()
        locationRequest = LocationRequest.create()
        locationRequest.interval = 1000 * 10
        locationRequest.fastestInterval = 1000 * 5
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        builder.setAlwaysShow(true)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())


        searchPlaces.setOnQueryTextListener(object: OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchLocation(it) }
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                return false
            }

        })

        myLocation.setOnClickListener {
            getLocation()
        }

    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val sydney = LatLng(-34.0, 151.0)
        markerOptions = MarkerOptions().position(sydney).title("Sydney")
        mMap.addMarker(markerOptions)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))

    }

    private fun searchLocation(query: String){
        if(query.isNotEmpty()){


            val geocoder = Geocoder(this)
            try{
                addressList = geocoder.getFromLocationName(query,1)
            } catch (e: IOException){
                e.printStackTrace()
            }
            if(addressList.isNotEmpty()){
                val address = addressList[0]
                if(address != null){
                    val latLng = LatLng(address.latitude,address.longitude)
                    markerOptions = MarkerOptions().position(latLng).title(query)
                    mMap.addMarker(markerOptions)
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                }


            }
            else{
                showToast("Location not found")
            }

        }
    }


    private fun getLocation() {
        if(ContextCompat.checkSelfPermission(this@MapsActivity,
                Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED){
            createLocationRequest()
        }

        else{
            if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M){
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_FINE_LOCATION)
            }
        }

    }

    @SuppressLint("MissingPermission")
    private fun updateGPS(){
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if(location!=null){
                val myLocation = LatLng(location.latitude,location.longitude)
                markerOptions = MarkerOptions().position(myLocation).title("My Location")
                mMap.addMarker(markerOptions)
                mMap.animateCamera(CameraUpdateFactory.newLatLng(myLocation))
            }
            else{
                showToast("No Location found")
            }

        }

    }

    private fun showToast(msg: String){
        Toast.makeText(this@MapsActivity,msg, Toast.LENGTH_SHORT)
    }

    @Override
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        if(requestCode == PERMISSIONS_FINE_LOCATION){
            createLocationRequest()
        }
        else{
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }



    private fun createLocationRequest() {

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { locationSettingsResponse ->
            // All location settings are satisfied. The client can initialize
            // location requests here.
            // ...
            updateGPS()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException){
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(this@MapsActivity,
                        REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

}



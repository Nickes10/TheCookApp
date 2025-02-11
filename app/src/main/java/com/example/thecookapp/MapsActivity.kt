package com.example.thecookapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.IOException
import java.util.Locale

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleMap: GoogleMap
    private val locationPermissionCode = 101
    private lateinit var geocoder: Geocoder
    private var currentMarker: Marker? = null  // Store the last placed marker
    private var userLatLng: LatLng? = null // Store the LatLng of the user

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geocoder = Geocoder(this, Locale.ENGLISH)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // If the user click on the button, add a Marker to the current location
        val fabAddMarker: FloatingActionButton = findViewById(R.id.fab_add_marker)
        fabAddMarker.setOnClickListener {
            val latLng = currentMarker?.position
            if (latLng != null) {
                // If marker exists, use its location
                getCityAndCountry(latLng)
            } else {
                // No marker, use user's location
                getCityAndCountry(userLatLng!!)
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isMapToolbarEnabled = false
        checkLocationPermission()

        // if the user click on the map, add a Marker to the selected location
        googleMap.setOnMapClickListener { latLng ->
            addMarkerAtPosition(latLng)
        }
    }

    private fun checkLocationPermission() {
        // Check if the app has the permission to the location of the device
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted, proceed with accessing the location
            googleMap.isMyLocationEnabled = true
            getUserLocation()
        } else {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionCode)
        }
    }

    private fun getUserLocation() {
        // Get position of the user
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, return or request permission
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionCode)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    userLatLng = LatLng(location.latitude, location.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 10f))
                } else {
                    Log.e("MapsActivity", "Current location is null")
                    Toast.makeText(this, "Unable to fetch current location", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("MapsActivity", "Error fetching current location: ${e.message}")
                Toast.makeText(this, "Error fetching current location", Toast.LENGTH_LONG).show()
            }
    }


    private fun addMarkerAtPosition(latLng: LatLng) {
        // Add graphically a marker where in the point of the map where the user clicks
        googleMap.clear()
        currentMarker = googleMap.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f))

        // Reverse geocoding to get city and country names
        try {
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val city = address.locality ?: "Unknown City"
                val country = address.countryName ?: "Unknown Country"
                Toast.makeText(this, "Location: $city, $country", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Unable to determine location name", Toast.LENGTH_LONG).show()
            }
        } catch (e: IOException) {
            Log.e("MapsActivity", "Geocoder failed: ${e.message}")
            Toast.makeText(this, "Geocoder failed", Toast.LENGTH_LONG).show()
        }
    }

    private fun getCityAndCountry(latLng: LatLng) {
        // Reverse geocoding to get city and country names
        try {
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val city = addresses[0].locality ?: "Unknown City"
                val country = addresses[0].countryName ?: "Unknown Country"
                Toast.makeText(this, "Location: $city, $country", Toast.LENGTH_LONG).show()

                val resultIntent = Intent().apply {
                    putExtra("city", city)
                    putExtra("country", country)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "Unable to determine location name", Toast.LENGTH_LONG).show()
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Geocoder failed", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted, proceed with accessing the location
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    googleMap.isMyLocationEnabled = true
                    getUserLocation()
                }
            } else {
                // Permission denied, inform the user of the necessity
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_LONG).show()
            }
        }
    }

}
package com.umsl.jwbmtd.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.squareup.picasso.Picasso
import com.umsl.jwbmtd.weatherapp.Models.WeatherData
import com.umsl.jwbmtd.weatherapp.databinding.ActivityMapsBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private final val FINE_PERMISSION_CODE: Int = 1
    private lateinit var lastLocation: Location
    private final val LOCATION_PERMISSION_REQUEST_CODE: Int = 1
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocation: FusedLocationProviderClient
    private var locationGranted: Boolean = false
    private val baseImageURL = "https://openweathermap.org/img/wn/"
    private val umslLatLon = LatLng(38.7092187, -90.3108972)
    private val weatherAPIKEY = com.umsl.jwbmtd.weatherapp.BuildConfig.WEATHER_API_KEY


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnRefresh.isEnabled = false

        binding.btnRefresh.setOnClickListener { event ->
            binding.progressBar.visibility = View.VISIBLE
            Toast.makeText(this, "Refreshed!", Toast.LENGTH_SHORT).show()
            initializeMap()
        }

        binding.progressBar.bringToFront() //puts progressbar in front of other views


        fusedLocation = LocationServices.getFusedLocationProviderClient(this)
        getLocationPermissions()
    }

    private fun getLocationPermissions() {

        // checks for location permissions, if none then requests them
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions()

        }else{
            locationGranted = true
            initializeMap()
        }



    }

    private fun initializeMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }


    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (locationGranted){

            fusedLocation.getLastLocation().addOnSuccessListener(this){location ->
                if (location != null){
                    val currentLocation = LatLng(location.latitude, location.longitude)
                    mMap.addMarker(MarkerOptions().position(currentLocation).title("Current Location"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation))
                    mMap.moveCamera(CameraUpdateFactory.zoomTo(8F))
                    mMap.addMarker(MarkerOptions().position(umslLatLon).title("UMSL"))
                    lastLocation = location

                    requestLocationWeatherData()
                    requestUMSLWeatherData()


                }
            }
        }








    }

    private fun requestLocationWeatherData() {
        val request = ServiceBuilder.buildService(WeatherEndPoints::class.java)
        val call = request.getLocationWeatherData(lastLocation.latitude, lastLocation.longitude, weatherAPIKEY,"imperial")

        call.enqueue(object : Callback<WeatherData>{
            override fun onResponse(call: Call<WeatherData>, response: Response<WeatherData>) {
                if (response.isSuccessful){
                    Log.i("onResponse", "${response.body()?.name}")

                    updateUI(response, "local")

                }
            }

            override fun onFailure(p0: Call<WeatherData>, t: Throwable) {
                Toast.makeText(this@MapsActivity, "${t.message}", Toast.LENGTH_LONG).show()
                Log.e("onFailure", "${t.message}")


            }

        })

    }

    private fun requestUMSLWeatherData(){
        val request = ServiceBuilder.buildService(WeatherEndPoints::class.java)
        val call = request.getLocationWeatherData(umslLatLon.latitude, umslLatLon.longitude, weatherAPIKEY, "imperial")

        call.enqueue(object : Callback<WeatherData>{
            override fun onResponse(call: Call<WeatherData>, response: Response<WeatherData>) {
                if (response.isSuccessful){
                    Log.i("onResponse", "${response.body()?.name}")

                    updateUI(response, "umsl")



                }
            }

            override fun onFailure(p0: Call<WeatherData>, t: Throwable) {
                Toast.makeText(this@MapsActivity, "${t.message}", Toast.LENGTH_LONG).show()
                Log.e("onFailure", "${t.message}")


            }

        })
    }

    //updates UI with weather data
    private fun updateUI(response: Response<WeatherData>, locationCode: String) {
        if (locationCode.equals("local")) {
            //set weather location name textview
            binding.tvCurrentLocation.text = "${response.body()?.name}"

            //set temp textview
            binding.tvCurLocTemp.text = "${response.body()!!.main.temp.toInt()}°F"


            //set weather icon imageview from url based on weather
            val imagePath = "${baseImageURL}${response.body()?.weather?.get(0)?.icon}@2x.png"
            Picasso.get()
                .load(imagePath)
                .fit()
                .centerInside()
                .into(binding.ivCurrentLocationIcon)


            //set weather description textview
            var capitalDesc = capitalizeFirstLetter("${response.body()?.weather?.get(0)?.description}")
            binding.tvCurrentLocationDescription.text = "${capitalDesc}"
        }

        if (locationCode.equals("umsl")){
            //set weather icon imageview from url based on weather
            val imagePath = "${baseImageURL}${response.body()?.weather?.get(0)?.icon}@2x.png"
            Picasso.get()
                .load(imagePath)
                .fit()
                .centerInside()
                .into(binding.ivUMSLIcon)


            //set weather description textview
            var capitalDesc = capitalizeFirstLetter("${response.body()?.weather?.get(0)?.description}")

            //set temp textview
            binding.tvUmslTemp.text = "${response.body()!!.main.temp.toInt()}°F"

            if(response.body()?.weather?.get(0)?.id!! >= 200 && response.body()!!.weather.get(0).id <= 531 ){
                capitalDesc.plus("Carry an umbrella!")
            }
            binding.tvUMSLDescription.text = "${capitalDesc}"
        }

        binding.btnRefresh.isEnabled = true
        binding.progressBar.visibility = View.INVISIBLE
    }

    fun capitalizeFirstLetter(string: String): String {
        return string.replaceFirstChar(Char::uppercaseChar)
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )


        return
    }

    //handles when user is asked for location permissions
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationGranted = false

        if(requestCode == FINE_PERMISSION_CODE){
            if(grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                locationGranted = true
                initializeMap()
            }
        }
    }
}
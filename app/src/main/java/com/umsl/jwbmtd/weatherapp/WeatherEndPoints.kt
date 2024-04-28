package com.umsl.jwbmtd.weatherapp

import com.umsl.jwbmtd.weatherapp.Models.WeatherData
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherEndPoints {

    @GET("weather")
    fun getLocationWeatherData(@Query("lat") lat: Double,
                               @Query("lon") lon: Double,
                               @Query("appid") key: String,
                               @Query("units") units: String
                               ): Call<WeatherData>
}



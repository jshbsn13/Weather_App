package com.umsl.jwbmtd.weatherapp.Models

data class WeatherData(
    var weather: Array<Weather> = arrayOf(),
    var name: String,
    var main: Main

)

data class Weather(
    var id: Int,
    var main: String,
    var description: String,
    var icon: String

)

data class Main(
    var temp: Double
)

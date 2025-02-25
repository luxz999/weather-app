package com.example.projectbutnotmy

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Locale
import java.util.Date
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnAboutDev = findViewById<Button>(R.id.btnAboutDev)

        btnAboutDev.setOnClickListener {
            showAboutDevPopup()
        }

        try {
            checkLocationPermission()
        } catch (e: Exception) {
            findViewById<TextView>(R.id.textViewTemperature).text = "Error: ${e.message}"
        }
    }

    private fun showAboutDevPopup() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("About Developer")
        dialogBuilder.setMessage("\nทิพย์ลูกกิ๊ก")
        dialogBuilder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }

        val alertDialog = dialogBuilder.create()
        alertDialog.show()
    }




    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getLastLocation()
        }
    }

    private fun getLastLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    fetchWeatherData(latitude, longitude)
                    val cityName = getCityName(latitude, longitude)
                    findViewById<TextView>(R.id.textViewCityName).text = cityName
                } else {
                    findViewById<TextView>(R.id.textViewTemperature).text = "Location not found"
                }
            }.addOnFailureListener { exception ->
                findViewById<TextView>(R.id.textViewTemperature).text = "Location Error: ${exception.message}"
            }
        }
    }

    private fun fetchWeatherData(latitude: Double, longitude: Double) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(WeatherApiService::class.java)
        val call = apiService.getWeatherData(latitude, longitude)

        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    val weatherData = response.body()
                    weatherData?.let {
                        updateUI(weatherData)
                    }
                } else {
                    findViewById<TextView>(R.id.textViewTemperature).text = "API Error: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                findViewById<TextView>(R.id.textViewTemperature).text = "Network Error: ${t.message}"
            }
        })
    }

    private fun updateUI(weatherData: WeatherResponse) {

        val currentTemperature = weatherData.current.temperature_2m
        val weatherCode = weatherData.current.weather_code
        val currentDateMain = weatherData.current.time
        val weatherDescription = getWeatherDescriptionThai(weatherCode)

        findViewById<TextView>(R.id.textViewTemperature).text = "$currentTemperature °C"
        findViewById<TextView>(R.id.textViewWeather).text = weatherDescription

        val cinputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val chourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val cparsedDate = cinputFormat.parse(currentDateMain)
        val chour = chourFormat.format(cparsedDate)
        val isDay = isDayTime(chour)
        val iconResId = getWeatherIconResource(weatherCode, isDay)
        findViewById<ImageView>(R.id.imageViewWeatherIcon).setImageResource(iconResId)

        findViewById<TextView>(R.id.textViewTemperature).text = "$currentTemperature °C"
        findViewById<TextView>(R.id.textViewWeather).text = weatherDescription

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val filteredTimes = mutableListOf<String>()
        val filteredTemperatures = mutableListOf<Double>()
        val filteredWeatherCodes = mutableListOf<Int>()
        val filteredPrecipitationProbabilities = mutableListOf<Int>()

        for (i in weatherData.hourly.time.indices) {
            if (weatherData.hourly.time[i].startsWith(currentDate)) {
                val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val originalTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(weatherData.hourly.time[i])
                val formattedTime = hourFormat.format(originalTime!!)
                filteredTimes.add(formattedTime)
                filteredTemperatures.add(weatherData.hourly.temperature_2m[i])
                filteredWeatherCodes.add(weatherData.hourly.weather_code[i])
                filteredPrecipitationProbabilities.add(weatherData.hourly.precipitation_probability[i])
            }
        }

        val hourlyAdapter = HourlyForecastAdapter(
            filteredTimes,
            filteredTemperatures,
            filteredWeatherCodes,
            filteredPrecipitationProbabilities,
            ::getWeatherIconResource,
            ::isDayTime
        )
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewHourly).apply {
            adapter = hourlyAdapter
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)

            val divider = DividerItemDecoration(this@MainActivity, LinearLayoutManager.HORIZONTAL)
            divider.setDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.divider)!!)
            addItemDecoration(divider)

            itemAnimator = DefaultItemAnimator()
            itemAnimator?.apply {
                addDuration = 300
                removeDuration = 300
                moveDuration = 300
                changeDuration = 300
            }
        }

        val dailyDates = weatherData.daily.time.map { date ->
            val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
            val originalDate = SimpleDateFormat("yyyy-MM-dd").parse(date)
            dateFormat.format(originalDate!!)
        }

        val dailyAdapter = DailyForecastAdapter(
            dailyDates,
            weatherData.daily.temperature_2m_max,
            weatherData.daily.temperature_2m_min,
            weatherData.daily.weather_code,
            ::getWeatherIconResource
        ) { selectedDate ->
            showHourlyWeatherPopup(selectedDate, weatherData)
        }
        findViewById<RecyclerView>(R.id.recyclerViewDaily).apply {
            adapter = dailyAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)

            val divider = DividerItemDecoration(this@MainActivity, LinearLayoutManager.VERTICAL)
            divider.setDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.divider)!!)
            addItemDecoration(divider)

            itemAnimator = DefaultItemAnimator()
            itemAnimator?.apply {
                addDuration = 300
                removeDuration = 300
                moveDuration = 300
                changeDuration = 300
            }

        }
    }

    private fun showHourlyWeatherPopup(selectedDate: String, weatherData: WeatherResponse) {
        Log.e("select Date", "${selectedDate}")

        val inputFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val calendar = Calendar.getInstance()
        val parsedDate = try {
            inputFormat.parse(selectedDate)
        } catch (e: Exception) {
            null
        }

        if (parsedDate == null) {
            Log.e("Date Conversion Error", "Failed to parse date: $selectedDate")
            return
        }

        calendar.time = parsedDate
        calendar.set(Calendar.YEAR, LocalDateTime.now().year)
        Log.e("year","${Calendar.YEAR}")
        val formattedDateWithYear = outputFormat.format(calendar.time)

        Log.e("Formatted Date with Year", formattedDateWithYear)

        val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val filteredTimes = mutableListOf<String>()
        val filteredTemperatures = mutableListOf<Double>()
        val filteredWeatherDescriptions = mutableListOf<String>()
        val filteredWeatherCodes = mutableListOf<Int>()

        for (i in weatherData.hourly.time.indices) {
            if (weatherData.hourly.time[i].startsWith(formattedDateWithYear)) {
                val formattedTime = hourFormat.format(SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(weatherData.hourly.time[i])!!)
                filteredTimes.add(formattedTime)
                filteredTemperatures.add(weatherData.hourly.temperature_2m[i])
                filteredWeatherDescriptions.add(getWeatherDescriptionThai(weatherData.hourly.weather_code[i]))
                filteredWeatherCodes.add(weatherData.hourly.weather_code[i])
            }
        }

        if (filteredTimes.isEmpty()) {
            Toast.makeText(this, "No hourly weather data available for the selected date.", Toast.LENGTH_SHORT).show()
            return
        }

        val bottomSheetView = layoutInflater.inflate(R.layout.popup_hourly_forecast, null)

        val titleTextView = bottomSheetView.findViewById<TextView>(R.id.textViewPopupTitle)
        titleTextView.text = SimpleDateFormat("dd/MM", Locale.getDefault()).format(calendar.time) // แสดงผลในรูปแบบ "วันที่/เดือน"

        val recyclerView = bottomSheetView.findViewById<RecyclerView>(R.id.recyclerViewPopupHourly)
        val adapter = PopupHourlyForecastAdapter(
            filteredTimes,
            filteredTemperatures,
            filteredWeatherDescriptions,
            filteredWeatherCodes,
            ::getWeatherIconResource
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }

    private fun getWeatherDescriptionThai(weatherCode: Int): String {
        return when (weatherCode) {
            0 -> "ปลอดโปร่ง"
            1 -> "บางส่วนมีเมฆ"
            2 -> "มีเมฆมาก"
            3 -> "มืดครึ้ม"
            45 -> "หมอก"
            48 -> "หมอกเย็น"
            51 -> "ฝนปรอยเบา"
            53 -> "ฝนปรอยปานกลาง"
            55 -> "ฝนปรอยหนัก"
            61 -> "ฝนเล็กน้อย"
            63 -> "ฝนปานกลาง"
            65 -> "ฝนหนัก"
            71 -> "หิมะตกเบา"
            73 -> "หิมะตกปานกลาง"
            75 -> "หิมะตกหนัก"
            80 -> "ฝนฟ้าคะนองเบา"
            81 -> "ฝนฟ้าคะนองปานกลาง"
            82 -> "ฝนฟ้าคะนองหนัก"
            95 -> "พายุฝนฟ้าคะนอง"
            96 -> "พายุฝนฟ้าคะนองพร้อมลูกเห็บ"
            99 -> "พายุฝนฟ้าคะนองพร้อมลูกเห็บหนัก"
            else -> "ไม่ทราบ"
        }
    }

    private fun getWeatherIconResource(weatherCode: Int, isDay: Boolean): Int {
        return when (weatherCode) {
            0 -> if (isDay) R.drawable.clear_day else R.drawable.clear_night
            1 -> if (isDay) R.drawable.partly_cloudy_day else R.drawable.partly_cloudy_night
            2 -> if (isDay) R.drawable.cloudy else R.drawable.cloudy
            3 -> if (isDay) R.drawable.overcast_day else R.drawable.overcast_night
            45, 48 -> if (isDay) R.drawable.fog_day else R.drawable.fog_night
            51, 53, 55 -> if (isDay) R.drawable.drizzle else R.drawable.drizzle
            61, 63, 65 -> if (isDay) R.drawable.rain else R.drawable.rain
            71, 73, 75 -> if (isDay) R.drawable.snow else R.drawable.snow
            80, 81, 82 -> if (isDay) R.drawable.thunderstorms_day_rain else R.drawable.thunderstorms_night_rain
            95, 96, 99 -> if (isDay) R.drawable.thunderstorms_rain else R.drawable.thunderstorms_rain
            else -> R.drawable.hail
        }
    }

    private fun isDayTime(time: String): Boolean {
        val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val parsedTime = hourFormat.parse(time)
        val calendar = Calendar.getInstance()
        calendar.time = parsedTime

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return hour in 6..18
    }

    private fun getCityName(lat: Double, long: Double): String {
        val geoCoder = Geocoder(this, Locale.getDefault())
        return try {
            val addresses = geoCoder.getFromLocation(lat, long, 1)

            if (addresses != null && addresses.isNotEmpty()) {
                val cityName = addresses[0].locality ?: "Unknown City"
                findViewById<TextView>(R.id.textViewCityName).text = cityName
                cityName
            } else {
                "Unknown City"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.message}"
        }
    }


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }
}

interface WeatherApiService {
    @GET("v1/forecast")
    fun getWeatherData(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,weather_code",
        @Query("hourly") hourly: String = "temperature_2m,precipitation_probability,weather_code",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum",
        @Query("timezone") timezone: String = "Asia/Bangkok"
    ): retrofit2.Call<WeatherResponse>
}

interface GeocodingApiService {
    @GET("v1/search")
    fun getCityName(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double
    ): retrofit2.Call<GeocodingResponse>
}

data class GeocodingResponse(
    val results: List<GeocodingResult>
)

data class GeocodingResult(
    val name: String
)

data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val current: CurrentWeather,
    val hourly: HourlyWeather,
    val daily: DailyWeather
)

data class CurrentWeather(
    val time: String,
    val temperature_2m: Double,
    val weather_code: Int
)

data class HourlyWeather(
    val time: List<String>,
    val temperature_2m: List<Double>,
    val precipitation_probability: List<Int>,
    val weather_code: List<Int>
)

data class DailyWeather(
    val time: List<String>,
    val weather_code: List<Int>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>,
    val precipitation_sum: List<Double>
)
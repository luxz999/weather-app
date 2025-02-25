package com.example.projectbutnotmy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PopupHourlyForecastAdapter(
    private val times: List<String>,
    private val temperatures: List<Double>,
    private val weatherDescriptions: List<String>,
    private val weatherCodes: List<Int>,
    private val getWeatherIconResource: (Int, Boolean) -> Int
) : RecyclerView.Adapter<PopupHourlyForecastAdapter.PopupHourlyViewHolder>() {

    class PopupHourlyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeTextView: TextView = itemView.findViewById(R.id.textViewTime)
        val temperatureTextView: TextView = itemView.findViewById(R.id.textViewTemperature)
        val weatherDescriptionTextView: TextView = itemView.findViewById(R.id.textViewWeatherDescription)
        val weatherIconImageView: ImageView = itemView.findViewById(R.id.imageViewWeatherIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopupHourlyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_popup_hourly_forecast, parent, false)
        return PopupHourlyViewHolder(view)
    }

    override fun onBindViewHolder(holder: PopupHourlyViewHolder, position: Int) {
        holder.timeTextView.text = times[position]
        holder.temperatureTextView.text = "${temperatures[position]} °C"
        holder.weatherDescriptionTextView.text = weatherDescriptions[position]

        val isDay = isDayTime(times[position])
        val iconResId = getWeatherIconResource(weatherCodes[position], isDay)
        holder.weatherIconImageView.setImageResource(iconResId)
    }

    override fun getItemCount(): Int = times.size

    private fun isDayTime(time: String): Boolean {
        val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val parsedTime = hourFormat.parse(time)
        val calendar = Calendar.getInstance()
        calendar.time = parsedTime

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return hour in 6..18
    }
}
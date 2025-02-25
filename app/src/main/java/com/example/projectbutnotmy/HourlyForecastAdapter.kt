package com.example.projectbutnotmy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HourlyForecastAdapter(
    private val hourlyData: List<String>,
    private val temperatures: List<Double>,
    private val weatherCodes: List<Int>,
    private val precipitationProbabilities: List<Int>,
    private val getWeatherIconResource: (Int, Boolean) -> Int,
    private val isDayTime: (String) -> Boolean
) : RecyclerView.Adapter<HourlyForecastAdapter.HourlyViewHolder>() {

    class HourlyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeTextView: TextView = itemView.findViewById(R.id.textViewTime)
        val temperatureTextView: TextView = itemView.findViewById(R.id.textViewTemperature)
        val weatherIconImageView: ImageView = itemView.findViewById(R.id.imageViewWeatherIcon)
        val precipitationTextView: TextView = itemView.findViewById(R.id.textViewPrecipitation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourlyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hourly_forecast, parent, false)
        return HourlyViewHolder(view)
    }

    override fun onBindViewHolder(holder: HourlyViewHolder, position: Int) {
        holder.timeTextView.text = hourlyData[position]
        holder.temperatureTextView.text = "${temperatures[position]} °C"

        val isDay = isDayTime(hourlyData[position])

        val weatherIconResource = getWeatherIconResource(weatherCodes[position], isDay)
        holder.weatherIconImageView.setImageResource(weatherIconResource)

        holder.precipitationTextView.text = "💧 ${precipitationProbabilities[position]}%"
    }

    override fun getItemCount(): Int = hourlyData.size
}
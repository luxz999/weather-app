package com.example.projectbutnotmy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class DailyForecastAdapter(
    private val dailyData: List<String>,
    private val maxTemps: List<Double>,
    private val minTemps: List<Double>,
    private val weatherCodes: List<Int>,
    private val getWeatherIconResource: (Int, Boolean) -> Int ,
    private val onItemClicked: (String) -> Unit
) : RecyclerView.Adapter<DailyForecastAdapter.DailyViewHolder>() {

    class DailyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTextView: TextView = itemView.findViewById(R.id.textViewDate)
        val weatherIconImageView: ImageView = itemView.findViewById(R.id.imageViewWeatherIcon)
        val maxTempTextView: TextView = itemView.findViewById(R.id.textViewMaxTemp)
        val minTempTextView: TextView = itemView.findViewById(R.id.textViewMinTemp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_daily_forecast, parent, false)
        return DailyViewHolder(view)
    }

    override fun onBindViewHolder(holder: DailyViewHolder, position: Int) {


        holder.dateTextView.text = dailyData[position]

        holder.maxTempTextView.text = "↑ ${maxTemps[position]} °C"
        holder.minTempTextView.text = "↓ ${minTemps[position]} °C"

        val iconResId = getWeatherIconResource(weatherCodes[position], true)
        holder.weatherIconImageView.setImageResource(iconResId)
        holder.itemView.setOnClickListener {
            val date = dailyData[position]
            onItemClicked(date)
        }
    }



    override fun getItemCount(): Int = dailyData.size
}
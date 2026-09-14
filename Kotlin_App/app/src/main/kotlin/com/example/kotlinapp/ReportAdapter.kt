package com.example.kotlinapp

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportAdapter(private var reports: List<ReportModel>) : RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    class ReportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto: ImageView = view.findViewById(R.id.ivReportPhotoItem)
        val tvDesc: TextView = view.findViewById(R.id.tvReportDescription)
        val tvLocation: TextView = view.findViewById(R.id.tvReportLocation)
        val tvDate: TextView = view.findViewById(R.id.tvReportDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]
        
        holder.tvDesc.text = report.description

        if (report.latitude != null && report.longitude != null) {
            holder.tvLocation.text = "GPS: Lat ${String.format("%.4f", report.latitude)}, Lng ${String.format("%.4f", report.longitude)}"
        } else {
            holder.tvLocation.text = "Ubicación: No disponible"
        }

        if (report.timestamp != null) {
            val date = Date(report.timestamp)
            val format = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            holder.tvDate.text = format.format(date)
        } else {
            holder.tvDate.text = "Fecha: ---"
        }

        try {
            if (!report.localImagePath.isNullOrEmpty()) {
                val file = File(report.localImagePath)
                if (file.exists()) {
                    holder.ivPhoto.setImageURI(Uri.fromFile(file))
                } else {
                    holder.ivPhoto.setImageResource(R.drawable.logo) // Fallback
                }
            } else {
                holder.ivPhoto.setImageResource(R.drawable.logo)
            }
        } catch (e: Exception) {
            holder.ivPhoto.setImageResource(R.drawable.logo)
        }
    }

    override fun getItemCount() = reports.size
    
    fun updateData(newReports: List<ReportModel>) {
        reports = newReports
        notifyDataSetChanged()
    }
}

data class ReportModel(
    val id: String = "",
    val userId: String = "",
    val description: String = "",
    val localImagePath: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long? = null
)

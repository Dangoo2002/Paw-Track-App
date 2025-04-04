package com.example.pawtrack

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pawtrack.databinding.EventItemBinding
import java.text.SimpleDateFormat
import java.util.*

class EventAdapter(
    private var events: MutableList<Event>,
    private val isFuture: Boolean
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    inner class EventViewHolder(val binding: EventItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = EventItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.binding.tvEvent.text = event.type // Use full description directly
        holder.binding.tvTime.text = when {
            event.type.contains("No ") -> "" // No time for default text
            isFuture -> {
                val timeLeft = event.time - System.currentTimeMillis()
                val hours = timeLeft / (1000 * 60 * 60)
                val minutes = (timeLeft % (1000 * 60 * 60)) / (1000 * 60)
                "$hours h $minutes m left"
            }
            else -> "Completed at ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.time))}"
        }
    }

    override fun getItemCount(): Int = events.size

    fun updateEvents(newEvents: List<Event>) {
        events.clear()
        events.addAll(newEvents)
        notifyDataSetChanged()
    }
}
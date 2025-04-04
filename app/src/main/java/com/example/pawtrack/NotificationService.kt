package com.example.pawtrack

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.google.firebase.database.*

class NotificationService : Service() {
    
    private lateinit var database: FirebaseDatabase
    private lateinit var eventsRef: DatabaseReference
    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 60000L

    override fun onCreate() {
        super.onCreate()
        database = FirebaseDatabase.getInstance()
        eventsRef = database.getReference("events")
        createNotificationChannel()
        startTaskChecking()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startTaskChecking() {
        handler.post(object : Runnable {
            override fun run() {
                checkTasks()
                handler.postDelayed(this, checkInterval)
            }
        })
    }

    private fun checkTasks() {
        eventsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentTime = System.currentTimeMillis()
                for (data in snapshot.children) {
                    val event = data.getValue(Event::class.java) ?: continue
                    if (!event.completed && event.time <= currentTime + 60000) {
                        showNotification(event)
                        eventsRef.child(event.id).child("completed").setValue(true)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Task Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun showNotification(event: Event) {
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("PawTrack Reminder")
            .setContentText("${event.type.capitalize()} ${event.dogName} is due now!")
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(event.id.hashCode(), notification)
    }

    companion object {
        private const val CHANNEL_ID = "PawTrackNotifications"
    }
}
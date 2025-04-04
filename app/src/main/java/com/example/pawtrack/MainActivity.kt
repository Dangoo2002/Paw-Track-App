package com.example.pawtrack

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.*
import com.example.pawtrack.databinding.ActivityMainBinding
import java.util.*
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var dogsRef: DatabaseReference
    private lateinit var eventsRef: DatabaseReference
    private lateinit var futureAdapter: EventAdapter
    private lateinit var completedAdapter: EventAdapter
    private val handler = Handler(Looper.getMainLooper())
    private val dogNames = mutableListOf<String>()
    private val allowedTypes = listOf("sleep", "feed", "vet", "walk")
    private val typeDescriptions = mapOf(
        "sleep" to "Sleep %s",
        "feed" to "Feed %s",
        "vet" to "Vet %s",
        "walk" to "Walk %s"
    )
    private val maxEvents = 4

    private val voiceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        results?.firstOrNull()?.let { command -> addEventFromVoice(command) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        database = FirebaseDatabase.getInstance()
        dogsRef = database.getReference("dogs")
        eventsRef = database.getReference("events")

        // Setup RecyclerViews
        futureAdapter = EventAdapter(mutableListOf(), true)
        completedAdapter = EventAdapter(mutableListOf(), false)
        binding.rvFutureActivities.layoutManager = LinearLayoutManager(this)
        binding.rvFutureActivities.adapter = futureAdapter
        binding.rvCompletedActivities.layoutManager = LinearLayoutManager(this)
        binding.rvCompletedActivities.adapter = completedAdapter

        // FAB click listener
        binding.fabAddEvent.setOnClickListener { showAddEventDialog() }

        // Voice button click listener
        binding.btnVoice.setOnClickListener { startVoiceRecognition() }

        // Add dog click listener
        binding.btnAddDog.setOnClickListener { showAddDogDialog() }

        // Remove dog click listener
        binding.btnRemoveDog.setOnClickListener { showRemoveDogDialog() }

        // Start notification service
        startService(Intent(this, NotificationService::class.java))

        // Load initial data
        loadDogs()
        loadEvents()
        startCountdownUpdater()
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say an event (e.g., Feed Max at 6 PM)")
        }
        voiceLauncher.launch(intent)
    }

    private fun addEventFromVoice(command: String) {
        val parts = command.lowercase().split(" at ")
        val eventPart = parts[0].split(" ")
        val eventType = eventPart[0].takeIf { it in allowedTypes } ?: "feed"
        val dogName = eventPart.drop(1).joinToString(" ")
        val timeStr = parts.getOrNull(1) ?: "12:00"
        val time = parseTime(timeStr)

        val description = typeDescriptions[eventType]?.format(dogName) ?: "Feed $dogName"
        val event = Event(
            id = UUID.randomUUID().toString(),
            dogName = dogName,
            type = description,
            time = time,
            completed = false // Explicitly future
        )

        eventsRef.get().addOnSuccessListener { snapshot ->
            val futureCount = snapshot.children.mapNotNull { it.getValue(Event::class.java) }
                .count { it.time > System.currentTimeMillis() && !it.completed }
            if (futureCount < maxEvents) {
                eventsRef.child(event.id).setValue(event)
            } else {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Error")
                    .setMessage("Max number of activities reached, please wait")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun showAddEventDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_event, null)
        val spinnerDogs = dialogView.findViewById<android.widget.Spinner>(R.id.spinner_dogs)
        val spinnerEvents = dialogView.findViewById<android.widget.Spinner>(R.id.spinner_events)
        val timePicker = dialogView.findViewById<android.widget.TimePicker>(R.id.time_picker)

        spinnerDogs.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, dogNames)
        spinnerEvents.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, allowedTypes)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Event")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val dogName = spinnerDogs.selectedItem.toString()
                val eventType = spinnerEvents.selectedItem.toString().lowercase()
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, timePicker.hour)
                    set(Calendar.MINUTE, timePicker.minute)
                    set(Calendar.SECOND, 0)
                    // Ensure future time by adding at least 1 minute if too close
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.MINUTE, 1)
                    }
                }
                val description = typeDescriptions[eventType]?.format(dogName) ?: "Feed $dogName"
                val event = Event(
                    id = UUID.randomUUID().toString(),
                    dogName = dogName,
                    type = description,
                    time = calendar.timeInMillis,
                    completed = false // Explicitly future
                )

                eventsRef.get().addOnSuccessListener { snapshot ->
                    val futureCount = snapshot.children.mapNotNull { it.getValue(Event::class.java) }
                        .count { it.time > System.currentTimeMillis() && !it.completed }
                    if (futureCount < maxEvents) {
                        eventsRef.child(event.id).setValue(event)
                    } else {
                        MaterialAlertDialogBuilder(this)
                            .setTitle("Error")
                            .setMessage("Max number of activities reached, please wait")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddDogDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_dog, null)
        val editTextDogName = dialogView.findViewById<android.widget.EditText>(R.id.et_dog_name)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Dog")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val dogName = editTextDogName.text.toString().trim()
                val dog = Dog(UUID.randomUUID().toString(), dogName)
                dogsRef.child(dog.id).setValue(dog)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRemoveDogDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_remove_dog, null)
        val spinnerDogs = dialogView.findViewById<android.widget.Spinner>(R.id.spinner_dogs)

        spinnerDogs.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, dogNames)

        MaterialAlertDialogBuilder(this)
            .setTitle("Remove Dog")
            .setView(dialogView)
            .setPositiveButton("Remove") { _, _ ->
                val dogName = spinnerDogs.selectedItem.toString()
                dogsRef.orderByChild("name").equalTo(dogName).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        snapshot.children.forEach { it.ref.removeValue() }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadDogs() {
        dogsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                dogNames.clear()
                for (data in snapshot.children) {
                    val dog = data.getValue(Dog::class.java)
                    dog?.name?.let { dogNames.add(it) }
                }
                binding.tvDogList.text = "Dogs: ${dogNames.joinToString(", ")}"
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadEvents() {
        eventsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentTime = System.currentTimeMillis()
                val futureEvents = snapshot.children.mapNotNull { it.getValue(Event::class.java) }
                    .filter { it.time > currentTime && !it.completed }
                    .sortedBy { it.time } // Earliest first
                    .take(maxEvents)
                val completedEvents = snapshot.children.mapNotNull { it.getValue(Event::class.java) }
                    .filter { it.completed || it.time <= currentTime }
                    .sortedByDescending { it.time } // Most recent first
                    .take(maxEvents)

                futureAdapter.updateEvents(futureEvents.takeIf { it.isNotEmpty() } ?: listOf(Event("", "", "No future activities", 0, false)))
                completedAdapter.updateEvents(completedEvents.takeIf { it.isNotEmpty() } ?: listOf(Event("", "", "No completed tasks", 0, true)))

                snapshot.children.forEach { data ->
                    val event = data.getValue(Event::class.java)
                    event?.let {
                        if (it.time <= currentTime && !it.completed) {
                            eventsRef.child(it.id).child("completed").setValue(true)
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun startCountdownUpdater() {
        handler.post(object : Runnable {
            override fun run() {
                futureAdapter.notifyDataSetChanged()
                completedAdapter.notifyDataSetChanged()
                handler.postDelayed(this, 60000)
            }
        })
    }

    private fun parseTime(timeStr: String): Long {
        val calendar = Calendar.getInstance()
        val parts = timeStr.lowercase().split(" ")
        val timeParts = parts[0].split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts.getOrNull(1)?.toInt() ?: 0

        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        // Ensure future time by adding 1 minute if parsed time is in the past
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.MINUTE, 1)
        }
        return calendar.timeInMillis
    }
}
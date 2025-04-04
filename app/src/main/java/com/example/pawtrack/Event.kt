package com.example.pawtrack

data class Event(
    val id: String = "",
    val dogName: String = "",
    val type: String = "",
    val time: Long = 0,
    val completed: Boolean = false
)
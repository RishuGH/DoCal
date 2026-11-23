package com.rishugh.docal.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Immutable
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val durationMinutes: Int = 60,
    val color: Long = 0xFF4285F4,  // Google Blue default
    val isCompleted: Boolean = false
)

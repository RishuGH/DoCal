package com.rishugh.docal.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import com.rishugh.docal.model.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class TaskRepository(context: Context? = null) {
    private val appContext = context?.applicationContext
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val prefs: SharedPreferences? by lazy {
        appContext?.getSharedPreferences("docal_prefs", Context.MODE_PRIVATE)
    }

    private val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(LocalDate::class.java, JsonSerializer<LocalDate> { src, _, _ ->
                JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE))
            })
            .registerTypeAdapter(LocalDate::class.java, JsonDeserializer<LocalDate> { json, _, _ ->
                LocalDate.parse(json.asString, DateTimeFormatter.ISO_LOCAL_DATE)
            })
            .registerTypeAdapter(LocalTime::class.java, JsonSerializer<LocalTime> { src, _, _ ->
                JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_TIME))
            })
            .registerTypeAdapter(LocalTime::class.java, JsonDeserializer<LocalTime> { json, _, _ ->
                LocalTime.parse(json.asString, DateTimeFormatter.ISO_LOCAL_TIME)
            })
            .create()
    }

    private var inMemoryTasks: MutableList<Task>? = null

    fun getTasks(): List<Task> {
        if (appContext == null) {
            if (inMemoryTasks == null) inMemoryTasks = createSampleTasks().toMutableList()
            return inMemoryTasks ?: emptyList()
        }
        val p = prefs ?: return createSampleTasks()
        val json = p.getString(KEY_TASKS, null)
        if (json.isNullOrBlank()) {
            val initialTasks = createSampleTasks()
            saveTasks(initialTasks)
            return initialTasks
        }
        return try {
            val type = object : TypeToken<List<Task>>() {}.type
            gson.fromJson(json, type) ?: createSampleTasks()
        } catch (e: Exception) {
            createSampleTasks()
        }
    }

    fun saveTasks(tasks: List<Task>) {
        if (appContext == null) {
            inMemoryTasks = tasks.toMutableList()
            return
        }
        ioScope.launch {
            prefs?.edit()?.putString(KEY_TASKS, gson.toJson(tasks))?.apply()
        }
    }

    private fun createSampleTasks(): List<Task> {
        val today = LocalDate.now()
        return listOf(
            Task(
                title = "Morning Standup",
                date = today,
                time = LocalTime.of(9, 30),
                durationMinutes = 30,
                color = 0xFF0F9D58,
                description = "Daily team sync"
            ),
            Task(
                title = "App Design Review",
                date = today,
                time = LocalTime.of(14, 0),
                durationMinutes = 60,
                color = 0xFF4285F4,
                description = "Review mockups with the design team"
            ),
            Task(title = "Buy Groceries", description = "Milk, eggs, bread"),
            Task(title = "Read Documentation")
        )
    }

    companion object {
        private const val KEY_TASKS = "tasks_list_v5"
    }
}

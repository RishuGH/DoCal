package com.rishugh.docal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rishugh.docal.data.TaskRepository
import com.rishugh.docal.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class TaskViewModel(application: Application? = null) : AndroidViewModel(application ?: Application()) {
    private val repository = TaskRepository(application)

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    val currentWeekStart = MutableStateFlow(LocalDate.now().with(DayOfWeek.MONDAY))
    val isDrawerOpen = MutableStateFlow(false)
    val selectedTaskId = MutableStateFlow<String?>(null)

    init {
        if (application == null) {
            _tasks.value = repository.getTasks()
        } else {
            viewModelScope.launch {
                val loaded = withContext(Dispatchers.IO) {
                    repository.getTasks()
                }
                _tasks.value = loaded
            }
        }
    }

    fun selectTask(taskId: String?) {
        selectedTaskId.value = taskId
    }

    fun addTask(title: String, description: String = "", color: Long = 0xFF4285F4, durationMinutes: Int = 60) {
        if (title.isBlank()) return
        val updated = _tasks.value + Task(
            title = title,
            description = description,
            color = color,
            durationMinutes = durationMinutes
        )
        _tasks.value = updated
        repository.saveTasks(updated)
    }

    fun updateTask(taskId: String, title: String, description: String, color: Long, durationMinutes: Int) {
        val updated = _tasks.value.map {
            if (it.id == taskId) it.copy(
                title = title,
                description = description,
                color = color,
                durationMinutes = durationMinutes
            ) else it
        }
        _tasks.value = updated
        repository.saveTasks(updated)
    }

    fun updateTaskTime(taskId: String, date: LocalDate?, time: LocalTime?) {
        val updated = _tasks.value.map {
            if (it.id == taskId) it.copy(date = date, time = time) else it
        }
        _tasks.value = updated
        repository.saveTasks(updated)
    }

    fun updateTaskDuration(taskId: String, newDuration: Int) {
        val updated = _tasks.value.map {
            if (it.id == taskId) it.copy(durationMinutes = newDuration.coerceAtLeast(15)) else it
        }
        _tasks.value = updated
        repository.saveTasks(updated)
    }

    fun updateTaskTimeAndDuration(taskId: String, date: LocalDate?, time: LocalTime?, durationMinutes: Int) {
        val updated = _tasks.value.map {
            if (it.id == taskId) it.copy(date = date, time = time, durationMinutes = durationMinutes.coerceAtLeast(15)) else it
        }
        _tasks.value = updated
        repository.saveTasks(updated)
    }

    fun toggleTaskComplete(taskId: String) {
        val updated = _tasks.value.map {
            if (it.id == taskId) it.copy(isCompleted = !it.isCompleted) else it
        }
        _tasks.value = updated
        repository.saveTasks(updated)
    }

    fun reorderTask(taskId: String, toIndex: Int) {
        val currentTasks = _tasks.value.toMutableList()
        val task = currentTasks.find { it.id == taskId } ?: return

        val unscheduled = currentTasks.filter { it.date == null }.toMutableList()
        if (task.date != null) {
            val unscheduledTask = task.copy(date = null, time = null)
            val safeIdx = toIndex.coerceIn(0, unscheduled.size)
            unscheduled.add(safeIdx, unscheduledTask)
            val scheduled = currentTasks.filter { it.date != null && it.id != taskId }
            val updated = scheduled + unscheduled
            _tasks.value = updated
            repository.saveTasks(updated)
            return
        }

        val fromIndex = unscheduled.indexOfFirst { it.id == taskId }
        val safeToIndex = toIndex.coerceIn(0, (unscheduled.size - 1).coerceAtLeast(0))
        if (fromIndex < 0 || fromIndex == safeToIndex) return

        val moved = unscheduled.removeAt(fromIndex)
        unscheduled.add(safeToIndex, moved)

        val scheduled = currentTasks.filter { it.date != null }
        val updated = scheduled + unscheduled
        _tasks.value = updated
        repository.saveTasks(updated)
    }

    fun deleteTask(taskId: String) {
        if (selectedTaskId.value == taskId) selectedTaskId.value = null
        val updated = _tasks.value.filterNot { it.id == taskId }
        _tasks.value = updated
        repository.saveTasks(updated)
    }

    fun toggleDrawer() { isDrawerOpen.value = !isDrawerOpen.value }
    fun closeDrawer() { isDrawerOpen.value = false }
    fun openDrawer() { isDrawerOpen.value = true }

    fun previousWeek() { currentWeekStart.update { it.minusWeeks(1) } }
    fun nextWeek() { currentWeekStart.update { it.plusWeeks(1) } }
    fun goToToday() { currentWeekStart.value = LocalDate.now().with(DayOfWeek.MONDAY) }
    fun setWeekStart(date: LocalDate) { currentWeekStart.value = date.with(DayOfWeek.MONDAY) }
}

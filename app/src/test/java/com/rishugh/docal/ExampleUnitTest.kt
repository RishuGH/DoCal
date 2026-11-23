package com.rishugh.docal

import com.rishugh.docal.viewmodel.TaskViewModel
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ExampleUnitTest {
    @Test
    fun taskViewModel_reorderAndResize_worksCorrectly() {
        val vm = TaskViewModel(null)
        val initialUnscheduled = vm.tasks.value.filter { it.date == null }
        assertTrue(initialUnscheduled.size >= 2)

        val firstId = initialUnscheduled[0].id
        val secondId = initialUnscheduled[1].id

        // Reorder task in right taskbar
        vm.reorderTask(firstId, 1)
        val afterReorder = vm.tasks.value.filter { it.date == null }
        assertEquals(secondId, afterReorder[0].id)
        assertEquals(firstId, afterReorder[1].id)

        // Resize task duration and time (top/bottom handle resize)
        val scheduledTask = vm.tasks.value.first { it.date != null }
        val newTime = LocalTime.of(10, 15)
        vm.updateTaskTimeAndDuration(scheduledTask.id, scheduledTask.date, newTime, 90)

        val updatedScheduled = vm.tasks.value.first { it.id == scheduledTask.id }
        assertEquals(newTime, updatedScheduled.time)
        assertEquals(90, updatedScheduled.durationMinutes)
    }
}

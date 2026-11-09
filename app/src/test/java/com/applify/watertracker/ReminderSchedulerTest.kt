package com.applify.watertracker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReminderSchedulerTest {

    @Test
    fun isWithinReminderWindow_standardDaytimeWindow() {
        val start = 9 * 60 // 09:00
        val end = 21 * 60  // 21:00

        val inside = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
        }
        assertTrue(ReminderScheduler.isWithinReminderWindow(inside, start, end))

        val outsideMorning = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 59)
        }
        assertFalse(ReminderScheduler.isWithinReminderWindow(outsideMorning, start, end))

        val outsideNight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
        }
        assertFalse(ReminderScheduler.isWithinReminderWindow(outsideNight, start, end))
    }

    @Test
    fun isWithinReminderWindow_overnightWindow() {
        val start = 22 * 60 // 22:00
        val end = 6 * 60   // 06:00

        val lateNight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 15)
        }
        assertTrue(ReminderScheduler.isWithinReminderWindow(lateNight, start, end))

        val earlyMorning = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 3)
            set(Calendar.MINUTE, 0)
        }
        assertTrue(ReminderScheduler.isWithinReminderWindow(earlyMorning, start, end))

        val daytime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
        }
        assertFalse(ReminderScheduler.isWithinReminderWindow(daytime, start, end))
    }
}

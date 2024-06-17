package co.iostream.apps.android.io_private.utils

import android.content.Context
import co.iostream.apps.android.io_private.R
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DateUtils private constructor() {
    companion object {
        fun format(milliSeconds: Long, dateFormat: String): String {
            // Create a DateFormatter object for displaying date in specified format.
            val formatter = SimpleDateFormat(dateFormat, Locale.getDefault())

            // Create a calendar object that will convert the date and time value in milliseconds to date.
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = milliSeconds
            return formatter.format(calendar.time)
        }

        fun formatRelative(date: String, format: String): String {
            val convTime: String
            val suffix = "ago"
            try {
                val dateFormat = SimpleDateFormat(format, Locale.getDefault())
                val pasTime = dateFormat.parse(date)
                val nowTime = Date()
                val dateDiff = nowTime.time - (pasTime?.time ?: 0)
                val second: Long = TimeUnit.MILLISECONDS.toSeconds(dateDiff)
                val minute: Long = TimeUnit.MILLISECONDS.toMinutes(dateDiff)
                val hour: Long = TimeUnit.MILLISECONDS.toHours(dateDiff)
                val day: Long = TimeUnit.MILLISECONDS.toDays(dateDiff)
                if (second < 60) {
                    convTime = "$second seconds $suffix"
                } else if (minute < 60) {
                    convTime = "$minute minutes $suffix"
                } else if (hour < 24) {
                    convTime = "$hour hours $suffix"
                } else if (day >= 7) {
                    convTime = if (day > 360) {
                        (day / 360).toString() + " years " + suffix
                    } else if (day > 30) {
                        (day / 30).toString() + " months " + suffix
                    } else {
                        (day / 7).toString() + " week " + suffix
                    }
                } else {
                    convTime = "$day days $suffix"
                }
            } catch (e: ParseException) {
                return ""
            }

            return convTime
        }

        fun formatRelative(context: Context, milliSeconds: Long): String {
            val convTime: String
            val suffix = context.getString(R.string.ago)
            try {
                val nowTime = Date()
                val dateDiff = nowTime.time - milliSeconds
                val second: Long = TimeUnit.MILLISECONDS.toSeconds(dateDiff)
                val minute: Long = TimeUnit.MILLISECONDS.toMinutes(dateDiff)
                val hour: Long = TimeUnit.MILLISECONDS.toHours(dateDiff)
                val day: Long = TimeUnit.MILLISECONDS.toDays(dateDiff)
                if (second < 60) {
                    convTime =
                        "$second ${context.getString(if (second == 1L) R.string.second else R.string.seconds)} $suffix"
                } else if (minute < 60) {
                    convTime =
                        "$minute ${context.getString(if (minute == 1L) R.string.second else R.string.seconds)} $suffix"
                } else if (hour < 24) {
                    convTime =
                        "$hour ${context.getString(if (hour == 1L) R.string.second else R.string.seconds)} $suffix"
                } else if (day >= 7) {
                    convTime = if (day > 360) {
                        (day / 360).toString() + " ${context.getString(if (day / 360 == 1L) R.string.year else R.string.years)} " + suffix
                    } else if (day > 30) {
                        (day / 30).toString() + " ${context.getString(if (day / 30 == 1L) R.string.month else R.string.months)} " + suffix
                    } else {
                        (day / 7).toString() + " ${context.getString(if (day / 7 == 1L) R.string.week else R.string.weeks)} " + suffix
                    }
                } else {
                    convTime =
                        "$day ${context.getString(if (day == 1L) R.string.day else R.string.days)} $suffix"
                }
            } catch (e: ParseException) {
                return ""
            }

            return convTime
        }
    }
}
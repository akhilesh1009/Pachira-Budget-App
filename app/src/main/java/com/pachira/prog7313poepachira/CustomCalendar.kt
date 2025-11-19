package com.pachira.prog7313poepachira

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageButton
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*



 // Custom dialog for selecting a single date using a calendar-style UI.
class CustomCalendarDialog(
    context: Context,
    private val initialDate: Calendar = Calendar.getInstance(),
    private val onDateSelectedListener: (Calendar) -> Unit
) : Dialog(context) {

    private lateinit var monthYearText: TextView
    private lateinit var calendarGrid: GridView
    private lateinit var prevMonthButton: ImageButton
    private lateinit var nextMonthButton: ImageButton

     // Tracks which month is displayed
    private val displayDate = Calendar.getInstance()
    private var selectedDate = Calendar.getInstance()

    init {
        selectedDate.timeInMillis = initialDate.timeInMillis
        displayDate.timeInMillis = initialDate.timeInMillis
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE) // Remove default title
        setContentView(R.layout.custom_calendar_dialog)

        monthYearText = findViewById(R.id.monthYearText)
        calendarGrid = findViewById(R.id.calendarGrid)
        prevMonthButton = findViewById(R.id.prevMonth)
        nextMonthButton = findViewById(R.id.nextMonth)

        // Handle month navigation
        prevMonthButton.setOnClickListener {
            displayDate.add(Calendar.MONTH, -1)
            updateCalendarView()
        }

        nextMonthButton.setOnClickListener {
            displayDate.add(Calendar.MONTH, 1)
            updateCalendarView()
        }

        updateCalendarView()
    }

    // Updates the grid and header text based on the current displayDate
    private fun updateCalendarView() {
        val dateFormat = SimpleDateFormat("MMMM - yyyy", Locale.getDefault())
        monthYearText.text = dateFormat.format(displayDate.time)

        calendarGrid.adapter = CalendarAdapter(context, displayDate, selectedDate) { newDate ->
            selectedDate.timeInMillis = newDate.timeInMillis
            onDateSelectedListener(selectedDate)
            dismiss() // Close dialog when a date is chosen
        }
    }
     // Adapter class to populate the calendar grid with dates.
    private class CalendarAdapter(
        private val context: Context,
        private val displayDate: Calendar,
        private val selectedDate: Calendar,
        private val onDateClick: (Calendar) -> Unit
    ) : BaseAdapter() {

        private val days = mutableListOf<DayItem>()
        private val calendar = Calendar.getInstance()
        private val currentMonth: Int
        private val currentYear: Int

        init {
            calendar.timeInMillis = displayDate.timeInMillis
            calendar.set(Calendar.DAY_OF_MONTH, 1)

            currentMonth = calendar.get(Calendar.MONTH)
            currentYear = calendar.get(Calendar.YEAR)

            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK)

            // Adjust starting index to Monday (0 = Mon, 6 = Sun)
            val adjustedFirstDay = if (firstDayOfMonth == Calendar.SUNDAY) 6 else firstDayOfMonth - 2

            // Fill in previous month's trailing days
            if (adjustedFirstDay > 0) {
                val prevMonth = Calendar.getInstance()
                prevMonth.timeInMillis = calendar.timeInMillis
                prevMonth.add(Calendar.MONTH, -1)
                val daysInPrevMonth = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

                for (i in (daysInPrevMonth - adjustedFirstDay + 1)..daysInPrevMonth) {
                    val day = Calendar.getInstance()
                    day.timeInMillis = prevMonth.timeInMillis
                    day.set(Calendar.DAY_OF_MONTH, i)
                    days.add(DayItem(i, day, false))
                }
            }

            // Fill current month days
            for (i in 1..daysInMonth) {
                val day = Calendar.getInstance()
                day.timeInMillis = calendar.timeInMillis
                day.set(Calendar.DAY_OF_MONTH, i)
                days.add(DayItem(i, day, true))
            }

            // Fill next month's leading days (to make 6 full weeks = 42 days)
            val remainingCells = 42 - days.size
            if (remainingCells > 0) {
                val nextMonth = Calendar.getInstance()
                nextMonth.timeInMillis = calendar.timeInMillis
                nextMonth.add(Calendar.MONTH, 1)

                for (i in 1..remainingCells) {
                    val day = Calendar.getInstance()
                    day.timeInMillis = nextMonth.timeInMillis
                    day.set(Calendar.DAY_OF_MONTH, i)
                    days.add(DayItem(i, day, false))
                }
            }
        }

        override fun getCount(): Int = days.size
        override fun getItem(position: Int): Any = days[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.calendar_day_item, parent, false)

            val dayText = view.findViewById<TextView>(R.id.dayText)
            val dayItem = days[position]

            // Display day number
            dayText.text = dayItem.dayNumber.toString()

            // Set text color based on current/other month
            dayText.setTextColor(
                if (dayItem.isCurrentMonth) Color.BLACK else Color.LTGRAY
            )

            // Highlight if selected
            val isSameDay = dayItem.date.get(Calendar.DAY_OF_MONTH) == selectedDate.get(Calendar.DAY_OF_MONTH)
            val isSameMonth = dayItem.date.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH)
            val isSameYear = dayItem.date.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR)
            val isSelected = isSameDay && isSameMonth && isSameYear

            if (isSelected) {
                dayText.setBackgroundResource(R.drawable.calendar_selected_day)
                dayText.setTextColor(Color.WHITE)
            } else {
                dayText.background = null
            }

            // Handle click
            view.setOnClickListener {
                onDateClick(dayItem.date)
            }

            return view
        }
         //Data class to represent each day in the calendar grid.
        private data class DayItem(
            val dayNumber: Int,
            val date: Calendar,
            val isCurrentMonth: Boolean
        )
    }
}

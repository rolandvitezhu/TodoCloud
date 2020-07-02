package com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment

import android.app.Dialog
import android.app.TimePickerDialog
import android.app.TimePickerDialog.OnTimeSetListener
import android.os.Bundle
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatDialogFragment
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.Constant
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.CreateTodoFragment
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.ModifyTodoFragment
import org.threeten.bp.LocalDateTime

class ReminderTimePickerDialogFragment : AppCompatDialogFragment(), OnTimeSetListener {

    private var hour = 0
    private var minute = 0
    private var date: LocalDateTime? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (arguments != null) {
            date = arguments!![Constant.REMINDER_DATE_TIME] as LocalDateTime?
            if (date != null) {
                hour = date!!.hour
                minute = date!!.minute
            }
        }

        return TimePickerDialog(
                activity, R.style.MyPickerDialogTheme, this, hour, minute, true
        )
    }

    override fun onTimeSet(view: TimePicker, hour: Int, minute: Int) {
        date = LocalDateTime.of(date!!.year, date!!.month, date!!.dayOfMonth, hour, minute)

        if (targetFragment is CreateTodoFragment)
            (targetFragment as CreateTodoFragment?)!!.onSelectReminderDateTime(date)
        else if (targetFragment is ModifyTodoFragment)
            (targetFragment as ModifyTodoFragment?)!!.onSelectReminderDateTime(date)

        dismiss()
    }
}
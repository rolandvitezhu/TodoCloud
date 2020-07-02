package com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.DatePicker
import androidx.appcompat.app.AppCompatDialogFragment
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.Constant
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.CreateTodoFragment
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.ModifyTodoFragment
import org.threeten.bp.LocalDateTime

class ReminderDatePickerDialogFragment : AppCompatDialogFragment(), OnDateSetListener {

    private var year = 0
    private var month = 0
    private var day = 0
    private var date: LocalDateTime? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val thereIsNoDateTime = arguments == null || arguments!![Constant.REMINDER_DATE_TIME] == null
        date = if (thereIsNoDateTime) LocalDateTime.now() else arguments!![Constant.REMINDER_DATE_TIME] as LocalDateTime?
        year = date!!.year
        month = date!!.monthValue
        day = date!!.dayOfMonth
        val datePickerDialog = DatePickerDialog(
                activity!!, R.style.MyPickerDialogTheme, this, year, month - 1, day
        )
        prepareDatePickerDialogButtons(datePickerDialog)

        return datePickerDialog
    }

    private fun prepareDatePickerDialogButtons(datePickerDialog: DatePickerDialog) {
        datePickerDialog.setButton(
                DialogInterface.BUTTON_POSITIVE,
                getString(R.string.reminderdatepicker_positivebuttontext),
                datePickerDialog
        )
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        date = LocalDateTime.of(year, month + 1, day, date!!.hour, date!!.minute)

        if (targetFragment is CreateTodoFragment)
            (targetFragment as CreateTodoFragment?)!!.onSelectReminderDate(date!!)
        else if (targetFragment is ModifyTodoFragment)
            (targetFragment as ModifyTodoFragment?)!!.onSelectReminderDate(date!!)

        dismiss()
    }
}
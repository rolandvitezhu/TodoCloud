package com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.appcompat.app.AppCompatDialogFragment
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.Constant
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.CreateTodoFragment
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.ModifyTodoFragment
import org.threeten.bp.LocalDate

class DatePickerDialogFragment : AppCompatDialogFragment(), OnDateSetListener {

    private var year = 0
    private var month = 0
    private var day = 0
    private var date: LocalDate? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val thereIsNoDate = arguments == null || arguments!![Constant.DUE_DATE] == null
        date = if (thereIsNoDate) LocalDate.now() else arguments!![Constant.DUE_DATE] as LocalDate?
        year = date!!.year
        month = date!!.monthValue
        day = date!!.dayOfMonth

        return DatePickerDialog(
                activity!!, R.style.MyPickerDialogTheme, this, year, month - 1, day
        )
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        date = LocalDate.of(year, month + 1, day)
        if (targetFragment is CreateTodoFragment) (targetFragment as CreateTodoFragment?)!!.onSelectDate(date) else if (targetFragment is ModifyTodoFragment) (targetFragment as ModifyTodoFragment?)!!.onSelectDate(date)
        dismiss()
    }
}
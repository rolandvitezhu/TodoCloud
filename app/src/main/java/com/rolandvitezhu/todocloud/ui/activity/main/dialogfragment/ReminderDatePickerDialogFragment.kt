package com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.DatePicker
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.lifecycle.ViewModelProvider
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.CreateTodoFragment
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.ModifyTodoFragment
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.TodosViewModel
import org.threeten.bp.LocalDateTime

class ReminderDatePickerDialogFragment : AppCompatDialogFragment(), OnDateSetListener {

    private val todosViewModel by lazy {
        ViewModelProvider(requireActivity()).get(TodosViewModel::class.java)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (todosViewModel.ldtReminderDateTime == null)
            todosViewModel.ldtReminderDateTime = LocalDateTime.now()

        val year = todosViewModel.ldtReminderDateTime?.year ?: 0
        val month = todosViewModel.ldtReminderDateTime?.monthValue ?: 0
        val day = todosViewModel.ldtReminderDateTime?.dayOfMonth ?: 0

        val datePickerDialog = DatePickerDialog(
                requireActivity(),
                R.style.MyPickerDialogTheme,
                this,
                year, month - 1, day
        )
        prepareDatePickerDialogButtons(datePickerDialog)

        return datePickerDialog
    }

    private fun prepareDatePickerDialogButtons(datePickerDialog: DatePickerDialog) {
        datePickerDialog.setButton(
                DialogInterface.BUTTON_POSITIVE,
                getString(R.string.reminderdatepicker_positivebuttontext),
                datePickerDialog/*Message.obtain()*/
        )
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        todosViewModel.ldtReminderDateTime =
                LocalDateTime.of(
                        year,
                        month + 1,
                        day,
                        todosViewModel.ldtReminderDateTime?.hour ?: 0,
                        todosViewModel.ldtReminderDateTime?.minute ?: 0)

        if (targetFragment is CreateTodoFragment?)
            (targetFragment as CreateTodoFragment?)?.onSelectReminderDate()
        else if (targetFragment is ModifyTodoFragment?)
            (targetFragment as ModifyTodoFragment?)?.onSelectReminderDate()

        dismiss()
    }
}
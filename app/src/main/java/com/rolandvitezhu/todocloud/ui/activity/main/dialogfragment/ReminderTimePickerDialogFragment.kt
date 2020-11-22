package com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment

import android.app.Dialog
import android.app.TimePickerDialog
import android.app.TimePickerDialog.OnTimeSetListener
import android.os.Bundle
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.lifecycle.ViewModelProvider
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.TodosViewModel
import org.threeten.bp.LocalDateTime
import org.threeten.bp.Month

class ReminderTimePickerDialogFragment : AppCompatDialogFragment(), OnTimeSetListener {

    private val todosViewModel by lazy {
        ViewModelProvider(requireActivity()).get(TodosViewModel::class.java)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return TimePickerDialog(
                requireActivity(),
                R.style.MyPickerDialogTheme,
                this,
                todosViewModel.ldtReminderDateTime?.hour ?: 0,
                todosViewModel.ldtReminderDateTime?.minute ?: 0,
                true)
    }

    override fun onTimeSet(view: TimePicker, hour: Int, minute: Int) {
        todosViewModel.ldtReminderDateTime =
                LocalDateTime.of(
                        todosViewModel.ldtReminderDateTime?.year ?: 0,
                        todosViewModel.ldtReminderDateTime?.month ?: Month.of(1),
                        todosViewModel.ldtReminderDateTime?.dayOfMonth ?: 0,
                        hour,
                        minute)

        todosViewModel.setTodoReminderDateTime(todosViewModel.ldtReminderDateTime)

        dismiss()
    }
}
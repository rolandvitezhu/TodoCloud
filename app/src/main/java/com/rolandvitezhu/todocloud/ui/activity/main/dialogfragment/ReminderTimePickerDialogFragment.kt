package com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment

import android.app.Dialog
import android.app.TimePickerDialog
import android.app.TimePickerDialog.OnTimeSetListener
import android.os.Bundle
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.lifecycle.ViewModelProvider
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.CreateTodoFragment
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.ModifyTodoFragment
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.TodosViewModel
import org.threeten.bp.LocalDateTime
import org.threeten.bp.Month

class ReminderTimePickerDialogFragment : AppCompatDialogFragment(), OnTimeSetListener {

    /*private var hour = 0
    private var minute = 0
    private var date: LocalDateTime? = null*/
    private val todosViewModel by lazy {
        ViewModelProvider(requireActivity()).get(TodosViewModel::class.java)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        /*date = requireArguments()[Constant.REMINDER_DATE_TIME] as LocalDateTime?
        if (date != null) {
            hour = date!!.hour
            minute = date!!.minute
        }*/

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

        if (targetFragment is CreateTodoFragment?)
            /*(targetFragment as CreateTodoFragment?)?.onSelectReminderDateTime(date)*/
        else if (targetFragment is ModifyTodoFragment?)
            /*(targetFragment as ModifyTodoFragment?)?.onSelectReminderDateTime(todosViewModel.ldtReminderDateTime)*/

        dismiss()
    }
}
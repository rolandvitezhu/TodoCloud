package com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.lifecycle.ViewModelProvider
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.TodosViewModel
import org.threeten.bp.LocalDate

class DatePickerDialogFragment : AppCompatDialogFragment(), OnDateSetListener {

    private val todosViewModel by lazy {
        ViewModelProvider(requireActivity()).get(TodosViewModel::class.java)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val year = todosViewModel.ldDueDate?.year ?: 0
        val month = todosViewModel.ldDueDate?.monthValue ?: 0
        val day = todosViewModel.ldDueDate?.dayOfMonth ?: 0

        return DatePickerDialog(
                requireActivity(),
                R.style.MyPickerDialogTheme,
                this,
                year, month - 1, day
        )
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        todosViewModel.setTodoDueDate(LocalDate.of(year, month + 1, day))
        dismiss()
    }
}
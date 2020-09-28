package com.rolandvitezhu.todocloud.ui.activity.main.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.databinding.FragmentModifytodoBinding
import com.rolandvitezhu.todocloud.helper.hideSoftInput
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.DatePickerDialogFragment
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.ReminderDatePickerDialogFragment
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.ReminderTimePickerDialogFragment
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.TodosViewModel

class ModifyTodoFragment : Fragment() {

    private val todosViewModel by lazy {
        ViewModelProvider(requireActivity()).get(TodosViewModel::class.java)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val fragmentModifytodoBinding: FragmentModifytodoBinding =
                FragmentModifytodoBinding.inflate(inflater, container, false)
        val view: View = fragmentModifytodoBinding.root

        fragmentModifytodoBinding.lifecycleOwner = this
        fragmentModifytodoBinding.modifyTodoFragment = this
        fragmentModifytodoBinding.todosViewModel = todosViewModel
        fragmentModifytodoBinding.executePendingBindings()

        return view
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)!!.onSetActionBarTitle(getString(R.string.modifytodo_title))
    }

    /**
     * Handle the modify todo action. If the title field is empty, we set the default title because
     * it is a required field. If the title field is not empty, we apply the modification which
     * the user have made.
     */
    fun handleModifyTodo() {
        hideSoftInput()
        if (todosViewModel.todoTitle.isNullOrBlank()) {
            // Set the original title of the todo on the UI. That was the title of the todo
            // as we have opened it.
            todosViewModel.todoTitle = todosViewModel.originalTitle
        } else {
            // Apply the modifications which the user did for the todo item. Persist the
            // modifications and navigate back to the previous screen.
            todosViewModel.onModifyTodo()
            (activity as MainActivity?)?.onBackPressed()
        }
    }

    private fun openDatePickerDialogFragment() {
        val datePickerDialogFragment = DatePickerDialogFragment()

        datePickerDialogFragment.setTargetFragment(this, 0)
        datePickerDialogFragment.show(parentFragmentManager, "DatePickerDialogFragment")
    }

    private fun openReminderDatePickerDialogFragment() {
        val reminderDatePickerDialogFragment = ReminderDatePickerDialogFragment()

        reminderDatePickerDialogFragment.setTargetFragment(this@ModifyTodoFragment, 0)
        reminderDatePickerDialogFragment.show(
                parentFragmentManager,
                "ReminderDatePickerDialogFragment"
        )
    }

    fun onSelectReminderDate() {
        openReminderTimePickerDialogFragment()
    }

    private fun openReminderTimePickerDialogFragment() {
        val reminderTimePickerDialogFragment = ReminderTimePickerDialogFragment()

        reminderTimePickerDialogFragment.setTargetFragment(this, 0)
        reminderTimePickerDialogFragment.show(
                parentFragmentManager,
                "ReminderTimePickerDialogFragment"
        )
    }

    fun onDueDateClick() {
        openDatePickerDialogFragment()
    }

    fun onReminderDateTimeClick() {
        openReminderDatePickerDialogFragment()
    }
}
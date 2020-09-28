package com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.lifecycle.ViewModelProvider
import com.rolandvitezhu.todocloud.databinding.DialogSorttodolistBinding
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.TodosViewModel

class SortTodoListDialogFragment : AppCompatDialogFragment() {

    private val todosViewModel by lazy {
        ViewModelProvider(requireActivity()).get(TodosViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val dialogSorttodolistBinding: DialogSorttodolistBinding =
                DialogSorttodolistBinding.inflate(layoutInflater, null, false)
        val view: View = dialogSorttodolistBinding.root

        dialogSorttodolistBinding.sortTodoListDialog = this
        dialogSorttodolistBinding.executePendingBindings()

        isCancelable = true

        return view
    }

    fun onSortByDueDateClicked() {
        todosViewModel.sortByDueDate()
        dismiss()
    }

    fun onSortByPriorityClicked() {
        todosViewModel.sortByPriority()
        dismiss()
    }
}
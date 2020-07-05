package com.rolandvitezhu.todocloud.ui.activity.main.dialog

import android.app.Dialog
import android.content.Context
import android.view.Window
import androidx.databinding.DataBindingUtil
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.databinding.DialogSorttodolistBinding

class SortTodoListDialog(context: Context,
                         private val presenter: Presenter) : Dialog(context) {
    interface Presenter {
        fun onSortByDueDatePushed()
        fun onSortByPriorityPushed()
    }

    fun onSortByDueDateClicked() {
        presenter.onSortByDueDatePushed()
        dismiss()
    }

    fun onSortByPriorityClicked() {
        dismiss()
        presenter.onSortByPriorityPushed()
    }

    init {
        //    BaseInjector.getComponent().inject(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogSorttodolistBinding: DialogSorttodolistBinding =
                DataBindingUtil.inflate(
                        layoutInflater,
                        R.layout.dialog_sorttodolist,
                        null,
                        false)
        setContentView(dialogSorttodolistBinding.root)
        dialogSorttodolistBinding.sortTodoListDialog = this

        setCancelable(true)
        show()
    }
}
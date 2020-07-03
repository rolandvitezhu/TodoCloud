package com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment

import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.data.Todo
import com.rolandvitezhu.todocloud.databinding.DialogConfirmdeleteBinding
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.MainListFragment
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.SearchFragment
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.TodoListFragment
import kotlinx.android.synthetic.main.dialog_confirmdelete.view.*
import java.util.*

class ConfirmDeleteDialogFragment : AppCompatDialogFragment() {

    private var itemType: String? = null
    private var itemsToDelete: ArrayList<*>? = null
    private var isManyItems = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.MyDialogTheme)
        prepareItemVariables()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val dialogConfirmdeleteBinding: DialogConfirmdeleteBinding =
                DataBindingUtil.inflate(inflater, R.layout.dialog_confirmdelete, container, false)
        val view: View = dialogConfirmdeleteBinding.root
        dialogConfirmdeleteBinding.confirmDeleteDialogFragment = this

        prepareDialogTexts(view)

        return view
    }

    private fun prepareItemVariables() {
        val arguments = arguments
        itemType = arguments!!.getString("itemType")
        itemsToDelete = arguments.getParcelableArrayList<Parcelable>("itemsToDelete")
        prepareIsManyItems()
    }

    private fun prepareIsManyItems() {
        if (itemsToDelete != null && itemsToDelete!!.size > 1) {
            isManyItems = true
        }
    }

    private fun prepareDialogTexts(view: View) {
        val itemTitle = arguments!!.getString("itemTitle")
        when (itemType) {
            "todo" -> if (isManyItems) {
                prepareConfirmDeleteTodosDialogTexts(view)
            } else {
                prepareConfirmDeleteTodoDialogTexts(view)
            }
            "list" -> if (isManyItems) {
                prepareConfirmDeleteListsDialogTexts(view)
            } else {
                prepareConfirmDeleteListDialogTexts(itemTitle, view)
            }
            "listInCategory" -> if (isManyItems) {
                prepareConfirmDeleteListsDialogTexts(view)
            } else {
                prepareConfirmDeleteListDialogTexts(itemTitle, view)
            }
            "category" -> if (isManyItems) {
                prepareConfirmDeleteCategoriesDialogTexts(view)
            } else {
                prepareConfirmDeleteCategoryDialogTexts(itemTitle, view)
            }
        }
    }

    private fun prepareConfirmDeleteCategoryDialogTexts(itemTitle: String?, view: View) {
        val dialogTitle = getString(R.string.confirmdelete_deletecategorytitle)
        val actionTextPrefix = getString(R.string.confirmdelete_deletecategoryactiontext)
        val actionText = prepareActionText(actionTextPrefix, itemTitle)
        setDialogTitle(dialogTitle)
        setActionText(actionText, view)
    }

    private fun prepareConfirmDeleteCategoriesDialogTexts(view: View) {
        val dialogTitle = getString(R.string.confirmdelete_categoriestitle)
        val actionText = getString(R.string.confirmdelete_categoriesactiontext)
        setDialogTitle(dialogTitle)
        setActionText(actionText, view)
    }

    private fun prepareConfirmDeleteListDialogTexts(itemTitle: String?, view: View) {
        val dialogTitle = getString(R.string.confirmdelete_deletelisttitle)
        val actionTextPrefix = getString(R.string.confirmdelete_deletelistactiontext)
        val actionText = prepareActionText(actionTextPrefix, itemTitle)
        setDialogTitle(dialogTitle)
        setActionText(actionText, view)
    }

    private fun prepareConfirmDeleteListsDialogTexts(view: View) {
        val dialogTitle = getString(R.string.confirmdelete_liststitle)
        val actionText = getString(R.string.confirmdelete_listsactiontext)
        setDialogTitle(dialogTitle)
        setActionText(actionText, view)
    }

    private fun prepareConfirmDeleteTodoDialogTexts(view: View) {
        val dialogTitle = getString(R.string.confirmdelete_deletetodotitle)
        val itemTitle = prepareTodoItemTitle()
        val actionTextPrefix = getString(R.string.confirmdelete_deletetodoactiontext)
        val actionText = prepareActionText(actionTextPrefix, itemTitle)
        setDialogTitle(dialogTitle)
        setActionText(actionText, view)
    }

    private fun prepareConfirmDeleteTodosDialogTexts(view: View) {
        val dialogTitle = getString(R.string.confirmdelete_todostitle)
        val actionText = getString(R.string.confirmdelete_todosactiontext)
        setDialogTitle(dialogTitle)
        setActionText(actionText, view)
    }

    private fun prepareTodoItemTitle(): String? {
        val todos: ArrayList<Todo>? = itemsToDelete as ArrayList<Todo>?
        return todos!![0].title
    }

    private fun prepareActionText(actionTextPrefix: String, itemTitle: String?): String {
        return "$actionTextPrefix\"$itemTitle\"?"
    }

    private fun setDialogTitle(dialogTitle: String) {
        val dialog = dialog
        dialog!!.setTitle(dialogTitle)
    }

    private fun setActionText(actionText: String, view: View) {
        view.textview_confirmdelete_actiontext!!.text = actionText
    }

    fun onBtnOkClick(view: View) {
        if (targetFragment is MainListFragment) {
            val mainListFragment = targetFragment as MainListFragment?
            if (itemType == "todo") {
                mainListFragment!!.onSoftDelete(itemsToDelete!!, itemType)
            } else if (!isManyItems) {
                val onlineId = arguments!!.getString("onlineId")
                mainListFragment!!.onSoftDelete(onlineId, itemType)
            } else {
                mainListFragment!!.onSoftDelete(itemsToDelete!!, itemType)
            }
        } else if (targetFragment is TodoListFragment) {
            val todoListFragment = targetFragment as TodoListFragment?
            if (itemType == "todo") {
                todoListFragment!!.onSoftDelete(itemsToDelete!!, itemType)
            } else if (!isManyItems) {
                val onlineId = arguments!!.getString("onlineId")
                todoListFragment!!.onSoftDelete(onlineId, itemType)
            } else {
                todoListFragment!!.onSoftDelete(itemsToDelete!!, itemType)
            }
        } else if (targetFragment is SearchFragment) {
            val searchFragment = targetFragment as SearchFragment?
            if (itemType == "todo") {
                searchFragment!!.onSoftDelete(itemsToDelete!!, itemType)
            } else if (!isManyItems) {
                val onlineId = arguments!!.getString("onlineId")
                searchFragment!!.onSoftDelete(onlineId, itemType)
            } else {
                searchFragment!!.onSoftDelete(itemsToDelete!!, itemType)
            }
        }
        dismiss()
    }

    fun onBtnCancelClick(view: View) {
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        val targetFragment = targetFragment
        if (targetFragment is DialogInterface.OnDismissListener) {
            (targetFragment as DialogInterface.OnDismissListener).onDismiss(dialog)
        }
    }
}
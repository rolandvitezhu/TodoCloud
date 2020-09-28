package com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment

import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.data.Todo
import com.rolandvitezhu.todocloud.databinding.DialogConfirmdeleteBinding
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.CategoriesViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.ListsViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.TodosViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.viewmodelfactory.ListsViewModelFactory
import kotlinx.android.synthetic.main.dialog_confirmdelete.view.*
import java.util.*

class ConfirmDeleteDialogFragment : AppCompatDialogFragment() {

    private var itemType: String? = null
    private var itemsToDelete: ArrayList<*>? = null
    private var isMultipleItems = false

    private val todosViewModel by lazy {
        ViewModelProvider(requireActivity()).get(TodosViewModel::class.java)
    }
    private val categoriesViewModel by lazy {
        ViewModelProvider(requireActivity()).get(CategoriesViewModel::class.java)
    }
    private val listsViewModel by lazy {
        ViewModelProvider(requireActivity(), ListsViewModelFactory(categoriesViewModel)).
                get(ListsViewModel::class.java)
    }

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
                DialogConfirmdeleteBinding.inflate(inflater, container, false)
        val view: View = dialogConfirmdeleteBinding.root

        dialogConfirmdeleteBinding.confirmDeleteDialogFragment = this
        dialogConfirmdeleteBinding.executePendingBindings()

        prepareDialogTexts(view)

        return view
    }

    private fun prepareItemVariables() {
        itemType = requireArguments().getString("itemType")
        itemsToDelete = requireArguments().getParcelableArrayList<Parcelable>("itemsToDelete")
        prepareIsMultipleItems()
    }

    private fun prepareIsMultipleItems() {
        isMultipleItems = !itemsToDelete.isNullOrEmpty()
    }

    /**
     * Prepare and set the dialog title and the action text by the item type.
     */
    private fun prepareDialogTexts(view: View) {
        val itemTitle = requireArguments().getString("itemTitle")
        when (itemType) {
            "todo" -> if (isMultipleItems) {
                prepareConfirmDeleteTodosDialogTexts(view)
            } else {
                prepareConfirmDeleteTodoDialogTexts(view)
            }
            "list" -> if (isMultipleItems) {
                prepareConfirmDeleteListsDialogTexts(view)
            } else {
                prepareConfirmDeleteListDialogTexts(itemTitle, view)
            }
            "listInCategory" -> if (isMultipleItems) {
                prepareConfirmDeleteListsDialogTexts(view)
            } else {
                prepareConfirmDeleteListDialogTexts(itemTitle, view)
            }
            "category" -> if (isMultipleItems) {
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
        requireDialog().setTitle(dialogTitle)
    }

    private fun setActionText(actionText: String, view: View) {
        view.textview_confirmdelete_actiontext.text = actionText
    }

    /**
     * Mark categories as deleted, update them in the local database and update the categories.
     */
    private fun softDeleteCategories(onlineId: String?) {
        if (isMultipleItems)
            categoriesViewModel.onSoftDelete(itemsToDelete, targetFragment)
        else
            categoriesViewModel.onSoftDelete(onlineId, targetFragment)
    }

    /**
     * Mark lists as deleted, update them in the local database and update the lists.
     */
    private fun softDeleteLists(onlineId: String?) {
        if (isMultipleItems)
            listsViewModel.onSoftDelete(itemsToDelete, targetFragment)
        else
            listsViewModel.onSoftDelete(onlineId, targetFragment)
    }

    /**
     * Mark todos as deleted, update them in the local database and update the todos.
     */
    private fun softDeleteTodos(onlineId: String?) {
        if (isMultipleItems)
            todosViewModel.onSoftDelete(itemsToDelete, targetFragment)
        else
            todosViewModel.onSoftDelete(onlineId, targetFragment)
    }

    fun onButtonOkClick(view: View) {
        val onlineId = requireArguments().getString("onlineId")
        when (itemType) {
            "todo" -> {
                softDeleteTodos(onlineId)
            }
            "list" -> {
                softDeleteLists(onlineId)
            }
            "listInCategory" -> {
                softDeleteLists(onlineId)
            }
            "category" -> {
                softDeleteCategories(onlineId)
            }
        }
        dismiss()
    }

    fun onButtonCancelClick(view: View) {
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (targetFragment is DialogInterface.OnDismissListener?) {
            (targetFragment as DialogInterface.OnDismissListener?)?.onDismiss(dialog)
        }
    }
}
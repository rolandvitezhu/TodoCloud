package com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController
import com.rolandvitezhu.todocloud.data.Category
import com.rolandvitezhu.todocloud.database.TodoCloudDatabaseDao
import com.rolandvitezhu.todocloud.databinding.DialogMovelistBinding
import com.rolandvitezhu.todocloud.helper.setSoftInputMode
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.MainListFragment
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.ListsViewModel
import kotlinx.android.synthetic.main.dialog_movelist.*
import javax.inject.Inject

class MoveListDialogFragment : AppCompatDialogFragment() {

    @Inject
    lateinit var todoCloudDatabaseDao: TodoCloudDatabaseDao

    private val listsViewModel by lazy {
        ViewModelProvider(requireActivity()).get(ListsViewModel::class.java)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as AppController).appComponent.
        fragmentComponent().create().inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.MyDialogTheme)

        listsViewModel.categoriesViewModel?.initializeCategoriesForSpinner()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val dialogMovelistBinding: DialogMovelistBinding =
                DialogMovelistBinding.inflate(inflater, container, false)
        val view: View = dialogMovelistBinding.root

        dialogMovelistBinding.moveListDialogFragment = this
        dialogMovelistBinding.listsViewModel = listsViewModel
        dialogMovelistBinding.executePendingBindings()

        requireDialog().setTitle(R.string.movelist_title)
        setSoftInputMode()

        return view
    }

    fun onButtonOkClick(view: View) {
        listsViewModel.isListNotInCategoryBeforeMove = listsViewModel.isListNotInCategory()
        listsViewModel.categoriesViewModel?.category?.categoryOnlineId =
                (this.spinner_movelist_category.selectedItem as Category).categoryOnlineId
        (targetFragment as MainListFragment?)?. let { listsViewModel.onMoveList(it) }
        dismiss()
    }

    fun onButtonCancelClick(view: View) {
        dismiss()
    }
}
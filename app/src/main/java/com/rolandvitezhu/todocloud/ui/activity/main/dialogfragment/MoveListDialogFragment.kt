package com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController
import com.rolandvitezhu.todocloud.data.Category
import com.rolandvitezhu.todocloud.databinding.DialogMovelistBinding
import com.rolandvitezhu.todocloud.datastorage.DbLoader
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.MainListFragment
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.CategoriesViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.ListsViewModel
import kotlinx.android.synthetic.main.dialog_movelist.*
import kotlinx.android.synthetic.main.dialog_movelist.view.*
import java.util.*
import javax.inject.Inject

class MoveListDialogFragment : AppCompatDialogFragment() {

    @Inject
    lateinit var dbLoader: DbLoader

    private var categoriesViewModel: CategoriesViewModel? = null
    private var listsViewModel: ListsViewModel? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (Objects.requireNonNull(activity)?.application as AppController).appComponent.
        fragmentComponent().create().inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.MyDialogTheme)
        categoriesViewModel = ViewModelProviders.of(activity!!).get(CategoriesViewModel::class.java)
        listsViewModel = ViewModelProviders.of(activity!!).get(ListsViewModel::class.java)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val dialogMovelistBinding: DialogMovelistBinding =
                DataBindingUtil.inflate(inflater, R.layout.dialog_movelist, container, false)
        val view: View = dialogMovelistBinding.root
        dialogMovelistBinding.moveListDialogFragment = this

        val dialog = dialog
        dialog!!.setTitle(R.string.movelist_title)
        setSoftInputMode()
        prepareSpinner(view)

        return view
    }

    private fun prepareSpinner(view: View) {
        val categoryForListWithoutCategory = Category(
                getString(R.string.movelist_spinneritemlistnotincategory)
        )
        val categoryOriginallyRelatedToList = categoriesViewModel!!.category
        val realCategoriesFromDatabase = dbLoader!!.categories
        val categoriesForSpinner = ArrayList<Category>()
        categoriesForSpinner.add(categoryForListWithoutCategory)
        categoriesForSpinner.addAll(realCategoriesFromDatabase)
        view.spinner_movelist_category!!.adapter = ArrayAdapter(
                activity!!,
                android.R.layout.simple_spinner_item,
                categoriesForSpinner
        )
        val categoryOriginallyRelatedToListPosition = categoriesForSpinner.indexOf(categoryOriginallyRelatedToList)
        view.spinner_movelist_category!!.setSelection(categoryOriginallyRelatedToListPosition)
    }

    private fun setSoftInputMode() {
        val dialog = dialog
        val window = dialog!!.window
        if (window != null) {
            val hiddenSoftInputAtOpenDialog = WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
            val softInputNotCoverFooterButtons = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            window.setSoftInputMode(softInputNotCoverFooterButtons or hiddenSoftInputAtOpenDialog)
        }
    }

    fun onBtnOkClick(view: View) {
        val category = categoriesViewModel!!.category
        val isListNotInCategoryBeforeMove = category.categoryOnlineId == null
        val (_, categoryOnlineId) = this.spinner_movelist_category!!.selectedItem as Category
        category.categoryOnlineId = categoryOnlineId
        categoriesViewModel!!.category = category
        (targetFragment as MainListFragment?)!!.onMoveList(isListNotInCategoryBeforeMove)
        dismiss()
    }

    fun onBtnCancelClick(view: View) {
        dismiss()
    }
}
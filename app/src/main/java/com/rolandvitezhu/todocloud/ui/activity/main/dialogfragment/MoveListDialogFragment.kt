package com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.Unbinder
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController
import com.rolandvitezhu.todocloud.data.Category
import com.rolandvitezhu.todocloud.datastorage.DbLoader
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.MainListFragment
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.CategoriesViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.ListsViewModel
import java.util.*
import javax.inject.Inject

class MoveListDialogFragment : AppCompatDialogFragment() {

    @Inject
    lateinit var dbLoader: DbLoader

    @BindView(R.id.spinner_movelist_category)
    lateinit var spinnerCategory: Spinner

    private var categoriesViewModel: CategoriesViewModel? = null
    private var listsViewModel: ListsViewModel? = null

    lateinit var unbinder: Unbinder

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
        val view = inflater.inflate(R.layout.dialog_movelist, container)
        unbinder = ButterKnife.bind(this, view)

        val dialog = dialog
        dialog!!.setTitle(R.string.movelist_title)
        setSoftInputMode()
        prepareSpinner()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder!!.unbind()
    }

    private fun prepareSpinner() {
        val categoryForListWithoutCategory = Category(
                getString(R.string.movelist_spinneritemlistnotincategory)
        )
        val categoryOriginallyRelatedToList = categoriesViewModel!!.category
        val realCategoriesFromDatabase = dbLoader!!.categories
        val categoriesForSpinner = ArrayList<Category>()
        categoriesForSpinner.add(categoryForListWithoutCategory)
        categoriesForSpinner.addAll(realCategoriesFromDatabase)
        spinnerCategory!!.adapter = ArrayAdapter(
                activity!!,
                android.R.layout.simple_spinner_item,
                categoriesForSpinner
        )
        val categoryOriginallyRelatedToListPosition = categoriesForSpinner.indexOf(categoryOriginallyRelatedToList)
        spinnerCategory!!.setSelection(categoryOriginallyRelatedToListPosition)
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

    @OnClick(R.id.button_movelist_ok)
    fun onBtnOkClick(view: View?) {
        val category = categoriesViewModel!!.category
        val isListNotInCategoryBeforeMove = category.categoryOnlineId == null
        val (_, categoryOnlineId) = spinnerCategory!!.selectedItem as Category
        category.categoryOnlineId = categoryOnlineId
        categoriesViewModel!!.category = category
        (targetFragment as MainListFragment?)!!.onMoveList(isListNotInCategoryBeforeMove)
        dismiss()
    }

    @OnClick(R.id.button_movelist_cancel)
    fun onBtnCancelClick(view: View?) {
        dismiss()
    }
}
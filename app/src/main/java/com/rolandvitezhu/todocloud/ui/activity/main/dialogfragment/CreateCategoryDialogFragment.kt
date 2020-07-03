package com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.data.Category
import com.rolandvitezhu.todocloud.databinding.DialogCreatecategoryBinding
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.MainListFragment
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.CategoriesViewModel
import kotlinx.android.synthetic.main.dialog_createcategory.*
import kotlinx.android.synthetic.main.dialog_createcategory.view.*

class CreateCategoryDialogFragment : AppCompatDialogFragment() {

    private var categoriesViewModel: CategoriesViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.MyDialogTheme)
        categoriesViewModel = ViewModelProviders.of(activity!!).get(CategoriesViewModel::class.java)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val dialogCreatecategoryBinding: DialogCreatecategoryBinding =
                DataBindingUtil.inflate(inflater, R.layout.dialog_createcategory, container, false)
        val view: View = dialogCreatecategoryBinding.root
        dialogCreatecategoryBinding.createCategoryDialogFragment = this

        val dialog = dialog
        dialog!!.setTitle(R.string.all_createcategory)
        setSoftInputMode()
        applyTextChangeEvent(view)
        applyEditorActionEvents(view)

        return view
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

    private fun applyTextChangeEvent(view: View) {
        view.textinputedittext_createcategory_title!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateTitle()
            }
        })
    }

    private fun applyEditorActionEvents(view: View) {
        view.textinputedittext_createcategory_title!!.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            val pressDone = actionId == EditorInfo.IME_ACTION_DONE
            var pressEnter = false
            if (event != null) {
                val keyCode = event.keyCode
                pressEnter = keyCode == KeyEvent.KEYCODE_ENTER
            }
            if (pressEnter || pressDone) {
                view.button_createcategory_ok!!.performClick()
                return@OnEditorActionListener true
            }
            false
        })
    }

    private fun validateTitle(): Boolean {
        val givenTitle = this.textinputedittext_createcategory_title!!.text.toString().trim { it <= ' ' }
        return if (givenTitle.isEmpty()) {
            this.textinputlayout_createcategory_title!!.error = getString(R.string.all_entertitle)
            false
        } else {
            this.textinputlayout_createcategory_title!!.isErrorEnabled = false
            true
        }
    }

    private fun prepareCategoryToCreate(givenTitle: String): Category {
        val categoryToCreate = Category()
        categoryToCreate.title = givenTitle
        categoryToCreate.rowVersion = 0
        categoryToCreate.deleted = false
        categoryToCreate.dirty = true
        categoryToCreate.position = 5.0
        return categoryToCreate
    }

    fun onBtnOkClick(view: View) {
        val givenTitle = this.textinputedittext_createcategory_title!!.text.toString().trim { it <= ' ' }
        if (validateTitle()) {
            val categoryToCreate = prepareCategoryToCreate(givenTitle)
            categoriesViewModel!!.category = categoryToCreate
            (targetFragment as MainListFragment?)!!.onCreateCategory()
            dismiss()
        }
    }

    fun onBtnCancelClick(view: View) {
        dismiss()
    }
}
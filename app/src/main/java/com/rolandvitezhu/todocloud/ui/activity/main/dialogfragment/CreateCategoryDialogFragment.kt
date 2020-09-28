package com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.databinding.DialogCreatecategoryBinding
import com.rolandvitezhu.todocloud.helper.setSoftInputMode
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.CategoriesViewModel
import kotlinx.android.synthetic.main.dialog_createcategory.*
import kotlinx.android.synthetic.main.dialog_createcategory.view.*

class CreateCategoryDialogFragment : AppCompatDialogFragment() {

    private val categoriesViewModel by lazy {
        ViewModelProvider(requireActivity()).get(CategoriesViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.MyDialogTheme)

        categoriesViewModel.initializeCategory()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val dialogCreatecategoryBinding: DialogCreatecategoryBinding =
                DialogCreatecategoryBinding.inflate(inflater, container, false)
        val view: View = dialogCreatecategoryBinding.root

        dialogCreatecategoryBinding.createCategoryDialogFragment = this
        dialogCreatecategoryBinding.categoriesViewModel = categoriesViewModel
        dialogCreatecategoryBinding.executePendingBindings()

        dialog?.setTitle(R.string.all_createcategory)
        setSoftInputMode()
        applyTextChangeEvent(view)
        applyEditorActionEvents(view)

        return view
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
        view.textinputedittext_createcategory_title?.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
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
        return if (categoriesViewModel.category.title.isBlank()) {
            this.textinputlayout_createcategory_title!!.error = getString(R.string.all_entertitle)
            false
        } else {
            this.textinputlayout_createcategory_title!!.isErrorEnabled = false
            true
        }
    }

    fun onButtonOkClick(view: View) {
        if (validateTitle()) {
            categoriesViewModel.onCreateCategory()
            dismiss()
        }
    }

    fun onButtonCancelClick(view: View) {
        dismiss()
    }
}
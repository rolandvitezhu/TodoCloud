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
import com.rolandvitezhu.todocloud.databinding.DialogModifycategoryBinding
import com.rolandvitezhu.todocloud.helper.setSoftInputMode
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.MainListFragment
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.CategoriesViewModel
import kotlinx.android.synthetic.main.dialog_modifycategory.*
import kotlinx.android.synthetic.main.dialog_modifycategory.view.*

class ModifyCategoryDialogFragment : AppCompatDialogFragment() {

    private val categoriesViewModel by lazy {
        ViewModelProvider(requireActivity()).get(CategoriesViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.MyDialogTheme)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val dialogModifycategoryBinding: DialogModifycategoryBinding =
                DialogModifycategoryBinding.inflate(inflater, container, false)
        val view: View = dialogModifycategoryBinding.root

        dialogModifycategoryBinding.modifyCategoryDialogFragment = this
        dialogModifycategoryBinding.categoriesViewModel = categoriesViewModel
        dialogModifycategoryBinding.executePendingBindings()

        requireDialog().setTitle(R.string.modifycategory_title)
        setSoftInputMode()
        applyTextChangedEvents(view)
        applyEditorEvents(view)

        return view
    }

    private fun applyTextChangedEvents(view: View) {
        view.textinputedittext_modifycategory_title.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateTitle()
            }
        })
    }

    private fun applyEditorEvents(view: View) {
        view.textinputedittext_modifycategory_title.setOnEditorActionListener(
                OnEditorActionListener { v, actionId, event ->
            val pressDone = actionId == EditorInfo.IME_ACTION_DONE
            var pressEnter = false
            if (event != null) {
                val keyCode = event.keyCode
                pressEnter = keyCode == KeyEvent.KEYCODE_ENTER
            }
            if (pressEnter || pressDone) {
                view.button_modifycategory_ok.performClick()
                return@OnEditorActionListener true
            }
            false
        })
    }

    private fun validateTitle(): Boolean {
        return if (categoriesViewModel.category.title.isBlank()) {
            this.textinputlayout_modifycategory_title!!.error = getString(R.string.all_entertitle)
            false
        } else {
            this.textinputlayout_modifycategory_title!!.isErrorEnabled = false
            true
        }
    }

    fun onButtonOkClick(view: View) {
        if (validateTitle()) {
            categoriesViewModel.onModifyCategory()
            (targetFragment as MainListFragment?)?.finishActionMode()
            dismiss()
        }
    }

    fun onButtonCancelClick(view: View) {
        dismiss()
    }
}
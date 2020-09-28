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
import com.rolandvitezhu.todocloud.databinding.DialogCreatelistincategoryBinding
import com.rolandvitezhu.todocloud.helper.setSoftInputMode
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.MainListFragment
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.ListsViewModel
import kotlinx.android.synthetic.main.dialog_createlistincategory.*
import kotlinx.android.synthetic.main.dialog_createlistincategory.view.*

class CreateListInCategoryDialogFragment : AppCompatDialogFragment() {

    private val listsViewModel by lazy {
        ViewModelProvider(requireActivity()).get(ListsViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.MyDialogTheme)

        listsViewModel.initializeList()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val dialogCreatelistincategoryBinding: DialogCreatelistincategoryBinding =
                DialogCreatelistincategoryBinding.inflate(inflater, container, false)
        val view: View = dialogCreatelistincategoryBinding.root

        dialogCreatelistincategoryBinding.createListInCategoryDialogFragment = this
        dialogCreatelistincategoryBinding.listsViewModel = listsViewModel
        dialogCreatelistincategoryBinding.executePendingBindings()

        requireDialog().setTitle(R.string.all_createlist)
        setSoftInputMode()
        applyTextChangedEvents(view)
        applyEditorActionEvents(view)

        return view
    }

    private fun applyTextChangedEvents(view: View) {
        view.textinputedittext_createlistincategory_title.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateTitle()
            }
        })
    }

    private fun applyEditorActionEvents(view: View) {
        view.textinputedittext_createlistincategory_title.setOnEditorActionListener(
                OnEditorActionListener { v, actionId, event ->
            val pressDone = actionId == EditorInfo.IME_ACTION_DONE
            var pressEnter = false
            if (event != null) {
                val keyCode = event.keyCode
                pressEnter = keyCode == KeyEvent.KEYCODE_ENTER
            }
            if (pressEnter || pressDone) {
                view.button_createlistincategory_ok!!.performClick()
                return@OnEditorActionListener true
            }
            false
        })
    }

    private fun validateTitle(): Boolean {
        return if (listsViewModel.list.title.isBlank()) {
            this.textinputlayout_createlistincategory_title.error = getString(R.string.all_entertitle)
            false
        } else {
            this.textinputlayout_createlistincategory_title.isErrorEnabled = false
            true
        }
    }

    fun onButtonOkClick(view: View) {
        if (validateTitle()) {
            listsViewModel.onCreateListInCategory()
            (targetFragment as MainListFragment?)?.finishActionMode()
            dismiss()
        }
    }

    fun onButtonCancelClick(view: View) {
        dismiss()
    }
}
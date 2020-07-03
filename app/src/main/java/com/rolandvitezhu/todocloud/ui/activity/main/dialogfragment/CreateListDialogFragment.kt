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
import com.rolandvitezhu.todocloud.data.List
import com.rolandvitezhu.todocloud.databinding.DialogCreatelistBinding
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.MainListFragment
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.ListsViewModel
import kotlinx.android.synthetic.main.dialog_createlist.*
import kotlinx.android.synthetic.main.dialog_createlist.view.*

class CreateListDialogFragment : AppCompatDialogFragment() {

    private var listsViewModel: ListsViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.MyDialogTheme)
        listsViewModel = ViewModelProviders.of(activity!!).get(ListsViewModel::class.java)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val dialogCreatelistBinding: DialogCreatelistBinding =
                DataBindingUtil.inflate(inflater, R.layout.dialog_createlist, container, false)
        val view: View = dialogCreatelistBinding.root
        dialogCreatelistBinding.createListDialogFragment = this

        val dialog = dialog
        dialog!!.setTitle(R.string.all_createlist)
        setSoftInputMode()
        applyTextChangedEvents(view)
        applyEditorEvents(view)

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

    private fun applyTextChangedEvents(view: View) {
        view.textinputedittext_createlist_title!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateTitle()
            }
        })
    }

    private fun applyEditorEvents(view: View) {
        view.textinputedittext_createlist_title!!.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            val pressDone = actionId == EditorInfo.IME_ACTION_DONE
            var pressEnter = false
            if (event != null) {
                val keyCode = event.keyCode
                pressEnter = keyCode == KeyEvent.KEYCODE_ENTER
            }
            if (pressEnter || pressDone) {
                view.button_createlist_ok!!.performClick()
                return@OnEditorActionListener true
            }
            false
        })
    }

    private fun validateTitle(): Boolean {
        val givenTitle = this.textinputedittext_createlist_title!!.text.toString().trim { it <= ' ' }
        return if (givenTitle.isEmpty()) {
            this.textinputlayout_createlist_title!!.error = getString(R.string.all_entertitle)
            false
        } else {
            this.textinputlayout_createlist_title!!.isErrorEnabled = false
            true
        }
    }

    private fun prepareListToCreate(givenTitle: String): List {
        val listToCreate = List()

        listToCreate.title = givenTitle
        listToCreate.rowVersion = 0
        listToCreate.deleted = false
        listToCreate.dirty = true
        listToCreate.position = 5.0

        return listToCreate
    }

    fun onBtnOkClick(view: View) {
        val givenTitle = this.textinputedittext_createlist_title!!.text.toString().trim { it <= ' ' }
        if (validateTitle()) {
            val listToCreate = prepareListToCreate(givenTitle)
            listsViewModel!!.list = listToCreate
            (targetFragment as MainListFragment?)!!.onCreateList()
            dismiss()
        }
    }

    fun onBtnCancelClick(view: View) {
        dismiss()
    }
}
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
import com.rolandvitezhu.todocloud.app.AppController.Companion.setText
import com.rolandvitezhu.todocloud.data.List
import com.rolandvitezhu.todocloud.databinding.DialogModifylistBinding
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.MainListFragment
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.ListsViewModel
import kotlinx.android.synthetic.main.dialog_modifylist.*
import kotlinx.android.synthetic.main.dialog_modifylist.view.*

class ModifyListDialogFragment : AppCompatDialogFragment() {

    private var listsViewModel: ListsViewModel? = null
    private var list: List? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.MyDialogTheme)
        listsViewModel = ViewModelProviders.of(activity!!).get(ListsViewModel::class.java)
        list = listsViewModel!!.list
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val dialogModifylistBinding: DialogModifylistBinding =
                DataBindingUtil.inflate(inflater, R.layout.dialog_modifylist, container, false)
        val view: View = dialogModifylistBinding.root
        dialogModifylistBinding.modifyListDialogFragment = this

        val dialog = dialog
        dialog?.setTitle(R.string.modifylist_title)
        setSoftInputMode()
        setText(list!!.title,
                view.textinputedittext_modifylist_title!!,
                view.textinputlayout_modifylist_title!!)
        applyTextChangedEvents(view)
        applyEditorActionEvents(view)

        return view
    }

    private fun setSoftInputMode() {
        val dialog = dialog
        var window: Window? = null
        if (dialog != null) {
            window = dialog.window
        }
        if (window != null) {
            val hiddenSoftInputAtOpenDialog = WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
            val softInputNotCoverFooterButtons = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            window.setSoftInputMode(softInputNotCoverFooterButtons or hiddenSoftInputAtOpenDialog)
        }
    }

    private fun applyTextChangedEvents(view: View) {
        view.textinputedittext_modifylist_title!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateTitle()
            }
        })
    }

    private fun applyEditorActionEvents(view: View) {
        view.textinputedittext_modifylist_title!!.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            val pressDone = actionId == EditorInfo.IME_ACTION_DONE
            var pressEnter = false
            if (event != null) {
                val keyCode = event.keyCode
                pressEnter = keyCode == KeyEvent.KEYCODE_ENTER
            }
            if (pressEnter || pressDone) {
                view.button_modifylist_ok!!.performClick()
                return@OnEditorActionListener true
            }
            false
        })
    }

    private fun validateTitle(): Boolean {
        val givenTitle = this.textinputedittext_modifylist_title!!.text.toString().trim { it <= ' ' }
        return if (givenTitle.isEmpty()) {
            this.textinputlayout_modifylist_title!!.error = getString(R.string.all_entertitle)
            false
        } else {
            this.textinputlayout_modifylist_title!!.isErrorEnabled = false
            true
        }
    }

    fun onBtnOkClick(view: View) {
        val givenTitle = this.textinputedittext_modifylist_title!!.text.toString().trim { it <= ' ' }
        if (validateTitle()) {
            list!!.title = givenTitle
            listsViewModel!!.list = list
            (targetFragment as MainListFragment?)!!.onModifyList()
            dismiss()
        }
    }

    fun onBtnCancelClick(view: View) {
        dismiss()
    }
}
package com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.Unbinder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.data.List
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.MainListFragment
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.ListsViewModel

class CreateListInCategoryDialogFragment : AppCompatDialogFragment() {

    @BindView(R.id.textinputlayout_createlistincategory_title)
    lateinit var tilTitle: TextInputLayout

    @BindView(R.id.textinputedittext_createlistincategory_title)
    lateinit var tietTitle: TextInputEditText

    @BindView(R.id.button_createlistincategory_ok)
    lateinit var btnOK: Button

    private var listsViewModel: ListsViewModel? = null

    lateinit var unbinder: Unbinder

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
        val view = inflater.inflate(R.layout.dialog_createlistincategory, container)
        unbinder = ButterKnife.bind(this, view)

        val dialog = dialog
        dialog!!.setTitle(R.string.all_createlist)
        setSoftInputMode()
        applyTextChangedEvents()
        applyEditorActionEvents()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder!!.unbind()
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

    private fun applyTextChangedEvents() {
        tietTitle!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateTitle()
            }
        })
    }

    private fun applyEditorActionEvents() {
        tietTitle!!.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            val pressDone = actionId == EditorInfo.IME_ACTION_DONE
            var pressEnter = false
            if (event != null) {
                val keyCode = event.keyCode
                pressEnter = keyCode == KeyEvent.KEYCODE_ENTER
            }
            if (pressEnter || pressDone) {
                btnOK!!.performClick()
                return@OnEditorActionListener true
            }
            false
        })
    }

    private fun validateTitle(): Boolean {
        val givenTitle = tietTitle!!.text.toString().trim { it <= ' ' }
        return if (givenTitle.isEmpty()) {
            tilTitle!!.error = getString(R.string.all_entertitle)
            false
        } else {
            tilTitle!!.isErrorEnabled = false
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

    @OnClick(R.id.button_createlistincategory_ok)
    fun onBtnOkClick(view: View?) {
        val givenTitle = tietTitle!!.text.toString().trim { it <= ' ' }
        if (validateTitle()) {
            val listToCreate = prepareListToCreate(givenTitle)
            listsViewModel!!.list = listToCreate
            (targetFragment as MainListFragment?)!!.onCreateListInCategory()
            dismiss()
        }
    }

    @OnClick(R.id.button_createlistincategory_cancel)
    fun onBtnCancelClick(view: View?) {
        dismiss()
    }
}
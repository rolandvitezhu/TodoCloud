package com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController.Companion.setText
import com.rolandvitezhu.todocloud.data.Category
import com.rolandvitezhu.todocloud.databinding.DialogModifycategoryBinding
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.MainListFragment
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.CategoriesViewModel
import kotlinx.android.synthetic.main.dialog_modifycategory.*
import kotlinx.android.synthetic.main.dialog_modifycategory.view.*

class ModifyCategoryDialogFragment : AppCompatDialogFragment() {

    private var categoriesViewModel: CategoriesViewModel? = null
    private var category: Category? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.MyDialogTheme)
        categoriesViewModel = ViewModelProviders.of(activity!!).get(CategoriesViewModel::class.java)
        category = categoriesViewModel!!.category
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val dialogModifycategoryBinding: DialogModifycategoryBinding =
                DataBindingUtil.inflate(inflater, R.layout.dialog_modifycategory, container, false)
        val view: View = dialogModifycategoryBinding.root
        dialogModifycategoryBinding.modifyCategoryDialogFragment = this

        val dialog = dialog
        dialog!!.setTitle(R.string.modifycategory_title)
        setSoftInputMode()
        setText(category!!.title,
                view.textinputedittext_modifycategory_title!!,
                view.textinputlayout_modifycategory_title!!)
        applyTextChangedEvents(view)
        applyEditorEvents(view.button_modifycategory_ok, view)

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
        view.textinputedittext_modifycategory_title!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateTitle()
            }
        })
    }

    private fun applyEditorEvents(btnOK: Button?, view: View) {
        view.textinputedittext_modifycategory_title!!.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
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
        val givenTitle = this.textinputedittext_modifycategory_title!!.text.toString().trim { it <= ' ' }
        return if (givenTitle.isEmpty()) {
            this.textinputlayout_modifycategory_title!!.error = getString(R.string.all_entertitle)
            false
        } else {
            this.textinputlayout_modifycategory_title!!.isErrorEnabled = false
            true
        }
    }

    fun onBtnOkClick(view: View) {
        val givenTitle = this.textinputedittext_modifycategory_title!!.text.toString().trim { it <= ' ' }
        if (validateTitle()) {
            category!!.title = givenTitle
            categoriesViewModel!!.category = category
            (targetFragment as MainListFragment?)!!.onModifyCategory()
            dismiss()
        }
    }

    fun onBtnCancelClick(view: View) {
        dismiss()
    }
}
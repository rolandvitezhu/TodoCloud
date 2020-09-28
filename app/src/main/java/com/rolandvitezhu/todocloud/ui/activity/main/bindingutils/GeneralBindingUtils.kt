package com.rolandvitezhu.todocloud.ui.activity.main.bindingutils

import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.fragment.app.FragmentActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.rolandvitezhu.todocloud.R

@BindingAdapter("textChangedListener")
fun TextInputEditText.addTextChangedListener(fragmentActivity: FragmentActivity) {
    var textInputLayout: TextInputLayout? = null
    if (parent.parent is TextInputLayout)
        textInputLayout = parent.parent as TextInputLayout

    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            validateTitle(textInputLayout, text, fragmentActivity)
        }
    })
}

private fun validateTitle(textInputLayout: TextInputLayout?,
                          text: Editable?,
                          fragmentActivity: FragmentActivity) {
    fragmentActivity.invalidateOptionsMenu()
    if (text.isNullOrBlank())
        textInputLayout?.error = textInputLayout?.context?.getString(R.string.all_entertitle)
    else
        textInputLayout?.isErrorEnabled = false
}

@BindingAdapter("emptyFieldValidator")
fun TextInputEditText.addEmptyFieldValidator(error: String) {
    var textInputLayout: TextInputLayout? = null
    if (parent.parent is TextInputLayout)
        textInputLayout = parent.parent as TextInputLayout

    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            validateField(text, textInputLayout, error)
        }
    })
}

private fun validateField(text: Editable?, textInputLayout: TextInputLayout?, error: String) {
    if (text.isNullOrBlank())
        textInputLayout?.error = error
    else
        textInputLayout?.isErrorEnabled = false
}

@BindingAdapter("doneButtonListener")
fun TextInputEditText.addDoneButtonListener(button: Button) {
    setOnEditorActionListener(
            TextView.OnEditorActionListener { v, actionId, event ->
                val pressDone = actionId == EditorInfo.IME_ACTION_DONE
                var pressEnter = false
                if (event != null) {
                    val keyCode = event.keyCode
                    pressEnter = keyCode == KeyEvent.KEYCODE_ENTER
                }
                if (pressEnter || pressDone) {
                    button.performClick()
                    return@OnEditorActionListener true
                }
                false
            })
}
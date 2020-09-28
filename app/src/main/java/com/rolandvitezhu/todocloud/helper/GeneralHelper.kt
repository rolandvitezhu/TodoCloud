package com.rolandvitezhu.todocloud.helper

import com.google.android.material.textfield.TextInputLayout

object GeneralHelper {

    /**
     * Show the specified error on the specified TextInputLayout or hide the error.
     */
    fun validateField(isFieldValid: Boolean, textInputLayout: TextInputLayout,
                              error: String): Boolean {
        if (isFieldValid)
            textInputLayout.isErrorEnabled = false
        else
            textInputLayout.error = error

        return isFieldValid
    }
}
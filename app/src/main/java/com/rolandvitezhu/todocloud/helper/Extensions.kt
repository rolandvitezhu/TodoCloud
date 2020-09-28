package com.rolandvitezhu.todocloud.helper

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.Fragment

fun AppCompatDialogFragment.setSoftInputMode() {
    val hiddenSoftInputAtOpenDialog = WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
    val softInputNotCoverFooterButtons = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
    requireDialog().window?.setSoftInputMode(softInputNotCoverFooterButtons or hiddenSoftInputAtOpenDialog)
}

fun Fragment.hideSoftInput() {
    val inputMethodManager = requireActivity().getSystemService(
            Context.INPUT_METHOD_SERVICE
    ) as InputMethodManager
    val currentlyFocusedView = requireActivity().currentFocus
    val windowToken = currentlyFocusedView?.windowToken
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}

fun Activity.hideSoftInput() {
    val inputMethodManager = getSystemService(
            Context.INPUT_METHOD_SERVICE
    ) as InputMethodManager
    val currentlyFocusedView = currentFocus
    if (currentlyFocusedView != null) {
        val windowToken = currentlyFocusedView.windowToken
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    }
}

fun Fragment.applyOrientationPortrait() {
    requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
}

fun Fragment.applyOrientationFullSensor() {
    requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
}
package com.rolandvitezhu.todocloud.ui.activity.main.bindingutils

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController
import com.rolandvitezhu.todocloud.data.List

@BindingAdapter("listSelected")
fun ConstraintLayout.setListSelected(list: List) {
    if (list.isSelected)
        AppController.appContext?.let {
            ContextCompat.getColor(it, R.color.colorAccent) }?.let {
            setBackgroundColor(it)
        }
    else
        AppController.appContext?.let {
            ContextCompat.getColor(it, android.R.color.transparent) }?.let {
            setBackgroundColor(it)
        }
}
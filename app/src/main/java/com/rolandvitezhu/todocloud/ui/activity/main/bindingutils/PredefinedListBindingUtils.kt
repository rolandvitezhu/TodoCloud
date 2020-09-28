package com.rolandvitezhu.todocloud.ui.activity.main.bindingutils

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.data.PredefinedList
import com.rolandvitezhu.todocloud.ui.activity.main.adapter.PredefinedListAdapter

@BindingAdapter(value = ["predefinedListImageAdapter", "predefinedListImagePredefinedList"])
fun ImageView.setPredefinedListImage(
        predefinedListAdapter: PredefinedListAdapter, predefinedList: PredefinedList) {
    when (predefinedListAdapter.predefinedLists.indexOf(predefinedList)) {
        0 -> setImageResource(R.drawable.baseline_today_24)
        1 -> setImageResource(R.drawable.baseline_view_week_24)
        2 -> setImageResource(R.drawable.baseline_all_inclusive_24)
        3 -> setImageResource(R.drawable.baseline_done_24)
    }
}
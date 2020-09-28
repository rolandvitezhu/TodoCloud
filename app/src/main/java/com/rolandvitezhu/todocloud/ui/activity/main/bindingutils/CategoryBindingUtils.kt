package com.rolandvitezhu.todocloud.ui.activity.main.bindingutils

import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController
import com.rolandvitezhu.todocloud.data.Category
import com.rolandvitezhu.todocloud.ui.activity.main.adapter.CategoryAdapter
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.CategoriesViewModel

@BindingAdapter(value = ["categoryIndicatorCategoryAdapter", "categoryIndicatorGroupPosition",
    "categoryIndicatorIsExpanded", "categoryIndicatorConvertView"])
fun ImageView.setCategoryIndicator(categoryAdapter: CategoryAdapter, groupPosition : Int,
isExpanded: Boolean, convertView: View) {
    categoryAdapter.handleCategoryIndicator(groupPosition, isExpanded, convertView)
}

@BindingAdapter("categorySelected")
fun ConstraintLayout.setCategorySelected(category: Category) {
    if (category.isSelected)
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

@BindingAdapter("selectedCategory")
fun Spinner.setSelectedCategory(categoriesViewModel: CategoriesViewModel) {
    if (selectedItem is Category)
        categoriesViewModel.category = selectedItem as Category
}

@BindingAdapter("categorySpinnerAdapter")
fun Spinner.setCategorySpinnerAdapter(categoriesForSpinner: ArrayList<Category>) {
    adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            categoriesForSpinner
    )
}
package com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.viewmodelfactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.CategoriesViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.ListsViewModel

class ListsViewModelFactory(
        private val categoriesViewModel: CategoriesViewModel?): ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ListsViewModel::class.java))
            return ListsViewModel(categoriesViewModel) as T
        throw Throwable("Unknown ViewModel class")
    }
}
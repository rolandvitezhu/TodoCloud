package com.rolandvitezhu.todocloud.helper

import com.google.android.material.textfield.TextInputLayout
import com.rolandvitezhu.todocloud.data.Category
import com.rolandvitezhu.todocloud.data.Todo

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

    fun getTodosArrayList(todos: List<Todo>): ArrayList<Todo> {
        val todosArrayList: ArrayList<Todo> = arrayListOf()
        todosArrayList.addAll(todos)

        return todosArrayList
    }

    fun getListsArrayList(lists: List<com.rolandvitezhu.todocloud.data.List>):
            ArrayList<com.rolandvitezhu.todocloud.data.List> {
        val listsArrayList: ArrayList<com.rolandvitezhu.todocloud.data.List> = arrayListOf()
        listsArrayList.addAll(lists)

        return listsArrayList
    }

    fun getCategoriesArrayList(categories: List<Category>): ArrayList<Category> {
        val categoriesArrayList: ArrayList<Category> = arrayListOf()
        categoriesArrayList.addAll(categories)

        return categoriesArrayList
    }
}
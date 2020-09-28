package com.rolandvitezhu.todocloud.repository

import com.rolandvitezhu.todocloud.app.AppController.Companion.instance
import com.rolandvitezhu.todocloud.data.Todo
import com.rolandvitezhu.todocloud.datastorage.DbConstants
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodoRepository @Inject constructor() : BaseRepository() {

    private var todosToUpdate: ArrayList<Todo>? = null
    private var todosToInsert: ArrayList<Todo>? = null

    suspend fun syncTodoData() {
        val response = apiService.getTodos(dbLoader.lastTodoRowVersion)
        if (response.error.toUpperCase(Locale.getDefault()).equals("FALSE")) {
            // Process the response
            updateTodosInLocalDatabase(response.todos)

            // Update and/or insert todos if there are
            updateAndOrInsertTodos()
        } else {
            // Process the error response
            throw Throwable(response.message)
        }
    }

    /**
     * Set the row version of each todo items in the list one by one. First it sets the row version
     * of a todo item and then it steps the value of the next row version variable, so as it sets
     * the row version of the next todo, it will be bigger by 1.
     */
    private fun setRowVersionsForTodos(todos: ArrayList<Todo>?) {
        if (!todos.isNullOrEmpty()) {
            for (todo in todos) {
                todo.rowVersion = nextRowVersion++
            }
        }
    }

    private suspend fun updateTodos() {
        if (!todosToUpdate.isNullOrEmpty()) {
            // Process the list
            for (todoToUpdate in todosToUpdate!!) {
                // Process the list item
                val response = apiService.updateTodo(todoToUpdate)
                if (response.error.toUpperCase(Locale.getDefault()).equals("FALSE")) {
                    // Process the response
                    makeTodoUpToDate(todoToUpdate)
                } else {
                    // Process the error response
                    throw Throwable(response.message)
                }
            }
        }
    }

    private fun makeTodoUpToDate(todoToUpdate: Todo) {
        todoToUpdate.dirty = false
        dbLoader.updateTodo(todoToUpdate)
        dbLoader.fixTodoPositions(null)
    }

    private suspend fun insertTodos() {
        if (!todosToInsert.isNullOrEmpty()) {
            // Process the list
            for (todoToInsert in todosToInsert!!) {
                // Process the list item
                val response = apiService.insertTodo(todoToInsert)
                if (response.error.toUpperCase(Locale.getDefault()).equals("FALSE")) {
                    // Process the response
                    makeTodoUpToDate(todoToInsert)
                } else {
                    // Process the error response
                    throw Throwable(response.message)
                }
            }
        }
    }

    private fun updateTodosInLocalDatabase(todos: ArrayList<Todo?>?) {
        if (!todos.isNullOrEmpty()) {
            for (todo in todos) {
                val exists = dbLoader.isTodoExists(todo!!.todoOnlineId)
                if (!exists) {
                    dbLoader.createTodo(todo)
                } else {
                    dbLoader.updateTodo(todo)
                    dbLoader.fixTodoPositions(null)
                }
            }
        }
    }

    private suspend fun updateAndOrInsertTodos() {
        val response = apiService.getNextRowVersion(DbConstants.Todo.DATABASE_TABLE, dbLoader.apiKey)
        if (response.error.toUpperCase(Locale.getDefault()).equals("FALSE")) {
            // Process the response
            nextRowVersion = response.nextRowVersion ?: 0
            todosToUpdate = dbLoader.todosToUpdate
            todosToInsert = dbLoader.todosToInsert

            setRowVersionsForTodos(todosToUpdate)
            setRowVersionsForTodos(todosToInsert)

            updateTodos()
            insertTodos()
        } else {
            // Process the error response
            throw Throwable(response.message)
        }
    }

    companion object {
        private val TAG = TodoRepository::class.java.simpleName
    }

    init {
        Objects.requireNonNull(instance)?.appComponent?.inject(this)
    }
}
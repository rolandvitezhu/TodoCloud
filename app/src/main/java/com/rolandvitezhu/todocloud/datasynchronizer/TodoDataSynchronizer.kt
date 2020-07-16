package com.rolandvitezhu.todocloud.datasynchronizer

import android.util.Log
import com.rolandvitezhu.todocloud.app.AppController.Companion.instance
import com.rolandvitezhu.todocloud.data.Todo
import com.rolandvitezhu.todocloud.datastorage.DbConstants
import com.rolandvitezhu.todocloud.network.ApiService
import com.rolandvitezhu.todocloud.network.api.rowversion.dto.GetNextRowVersionResponse
import com.rolandvitezhu.todocloud.network.api.todo.dto.*
import com.rolandvitezhu.todocloud.network.helper.RetrofitResponseHelper.Companion.IsNoError
import com.rolandvitezhu.todocloud.network.helper.RetrofitResponseHelper.Companion.ResponseToJson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodoDataSynchronizer @Inject constructor() : BaseDataSynchronizer() {

    private val apiService: ApiService

    private var disposable: CompositeDisposable? = null

    private var onSyncTodoDataListener: OnSyncTodoDataListener? = null
    private var isUpdateTodoRequestsFinished = false
    private var updateTodoRequestCount = 0
    private var currentUpdateTodoRequest = 0
    private var isInsertTodoRequestsFinished = false
    private var insertTodoRequestCount = 0
    private var currentInsertTodoRequest = 0

    private var todosToUpdate: ArrayList<Todo>? = null
    private var todosToInsert: ArrayList<Todo>? = null

    fun setOnSyncTodoDataListener(onSyncTodoDataListener: OnSyncTodoDataListener?) {
        this.onSyncTodoDataListener = onSyncTodoDataListener
    }

    fun syncTodoData(disposable: CompositeDisposable?) {
        this.disposable = disposable
        initTodoRequestsStates()

        apiService
                .getTodos(dbLoader.lastTodoRowVersion)
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribeWith(createGetTodosDisposableSingleObserver())?.let {
                    this.disposable!!.add(
                            it
                    )
                }
    }

    private fun createGetTodosDisposableSingleObserver(): DisposableSingleObserver<GetTodosResponse?> {
        return object : DisposableSingleObserver<GetTodosResponse?>() {

            override fun onSuccess(getTodosResponse: GetTodosResponse) {
                Log.d(TAG, "Get Todos Response: $getTodosResponse")

                if (getTodosResponse != null && getTodosResponse.error == "false") {
                    val todos = getTodosResponse.todos

                    if (!todos!!.isEmpty())
                        updateTodosInLocalDatabase(todos)

                    // Insert nor update todos if any
                    val shouldUpdateOrInsertTodos = !todosToUpdate!!.isEmpty() || !todosToInsert!!.isEmpty()

                    if (shouldUpdateOrInsertTodos)
                        updateOrInsertTodos()
                    else
                        onSyncTodoDataListener!!.onFinishSyncTodoData()
                } else if (getTodosResponse != null) {
                    // Handle error, if any
                    var message = getTodosResponse.getMessage()

                    if (message == null) message = "Unknown error"
                    onSyncTodoDataListener!!.onSyncError(message)
                }
            }

            override fun onError(throwable: Throwable) {
                Log.d(TAG, "Get Todos Response - onFailure: $throwable")
                onSyncTodoDataListener!!.onSyncError(throwable.toString())
            }
        }
    }

    private fun initTodoRequestsStates() {
        isUpdateTodoRequestsFinished = false
        isInsertTodoRequestsFinished = false
        todosToUpdate = dbLoader.todosToUpdate
        todosToInsert = dbLoader.todosToInsert
        nextRowVersion = 0
    }

    private fun setRowVersionsForTodos(todos: ArrayList<Todo>?) {
        for (todo in todos!!) {
            todo.rowVersion = nextRowVersion++
        }
    }

    private fun updateTodos() {
        // Process list
        if (!todosToUpdate!!.isEmpty()) {
            updateTodoRequestCount = todosToUpdate!!.size
            currentUpdateTodoRequest = 1

            // Process list item
            for (todoToUpdate in todosToUpdate!!) {
                val updateTodoRequest = UpdateTodoRequest()

                updateTodoRequest.todoOnlineId = todoToUpdate.todoOnlineId
                updateTodoRequest.listOnlineId = todoToUpdate.listOnlineId
                updateTodoRequest.title = todoToUpdate.title
                updateTodoRequest.priority = todoToUpdate.priority
                updateTodoRequest.dueDate = todoToUpdate.dueDate
                updateTodoRequest.reminderDateTime = todoToUpdate.reminderDateTime
                updateTodoRequest.description = todoToUpdate.description
                updateTodoRequest.completed = todoToUpdate.completed
                updateTodoRequest.rowVersion = todoToUpdate.rowVersion
                updateTodoRequest.deleted = todoToUpdate.deleted
                updateTodoRequest.position = todoToUpdate.position

                apiService
                        .updateTodo(updateTodoRequest)
                        ?.subscribeOn(Schedulers.io())
                        ?.observeOn(AndroidSchedulers.mainThread())
                        ?.subscribeWith(createUpdateTodoDisposableSingleObserver(todoToUpdate))?.let {
                            disposable!!.add(
                                    it
                        )
                        }
            }
            // Sync finished - there are no list items
        } else {
            isUpdateTodoRequestsFinished = true
            if (isAllTodoRequestsFinished) {
                onSyncTodoDataListener!!.onFinishSyncTodoData()
            }
        }
    }

    private fun createUpdateTodoDisposableSingleObserver(todoToUpdate: Todo):
            DisposableSingleObserver<UpdateTodoResponse?> {
        return object : DisposableSingleObserver<UpdateTodoResponse?>() {

            override fun onSuccess(updateTodoResponse: UpdateTodoResponse) {
                Log.d(TAG, "Update Todo Response: $updateTodoResponse")

                if (updateTodoResponse != null && updateTodoResponse.error == "false") {
                    makeTodoUpToDate(todoToUpdate)
                    if (isLastUpdateTodoRequest) {
                        isUpdateTodoRequestsFinished = true
                        if (isAllTodoRequestsFinished) {
                            onSyncTodoDataListener!!.onFinishSyncTodoData()
                        }
                    }
                } else if (updateTodoResponse != null) {
                    var message = updateTodoResponse.getMessage()

                    if (message == null) message = "Unknown error"
                    onSyncTodoDataListener!!.onSyncError(message)

                    if (isLastUpdateTodoRequest) {
                        isUpdateTodoRequestsFinished = true
                        if (isAllTodoRequestsFinished) {
                            onSyncTodoDataListener!!.onFinishSyncTodoData()
                        }
                    }
                }
            }

            override fun onError(throwable: Throwable) {
                Log.d(TAG, "Update Todo Response - onFailure: $throwable")

                onSyncTodoDataListener!!.onSyncError(throwable.toString())

                if (isLastUpdateTodoRequest) {
                    isUpdateTodoRequestsFinished = true
                    if (isAllTodoRequestsFinished) {
                        onSyncTodoDataListener!!.onFinishSyncTodoData()
                    }
                }
            }
        }
    }

    private fun makeTodoUpToDate(todoToUpdate: Todo) {
        todoToUpdate.dirty = false
        dbLoader.updateTodo(todoToUpdate)
        dbLoader.fixTodoPositions(null)
    }

    private fun insertTodos() {
        // Process list
        if (!todosToInsert!!.isEmpty()) {
            insertTodoRequestCount = todosToInsert!!.size
            currentInsertTodoRequest = 1

            // Process list item
            for (todoToInsert in todosToInsert!!) {
                val insertTodoRequest = InsertTodoRequest()

                insertTodoRequest.todoOnlineId = todoToInsert.todoOnlineId
                insertTodoRequest.listOnlineId = todoToInsert.listOnlineId
                insertTodoRequest.title = todoToInsert.title
                insertTodoRequest.priority = todoToInsert.priority
                insertTodoRequest.dueDate = todoToInsert.dueDate
                insertTodoRequest.reminderDateTime = todoToInsert.reminderDateTime
                insertTodoRequest.description = todoToInsert.description
                insertTodoRequest.completed = todoToInsert.completed
                insertTodoRequest.rowVersion = todoToInsert.rowVersion
                insertTodoRequest.deleted = todoToInsert.deleted
                insertTodoRequest.position = todoToInsert.position

                apiService
                        .insertTodo(insertTodoRequest)
                        ?.subscribeOn(Schedulers.io())
                        ?.observeOn(AndroidSchedulers.mainThread())
                        ?.subscribeWith(createInsertTodoDisposableSingleObserver(todoToInsert))?.let {
                            disposable!!.add(
                                    it
                        )
                        }
            }
            // Sync finished - there are no list items
        } else {
            isInsertTodoRequestsFinished = true
            if (isAllTodoRequestsFinished) {
                onSyncTodoDataListener!!.onFinishSyncTodoData()
            }
        }
    }

    private fun createInsertTodoDisposableSingleObserver(todoToInsert: Todo):
            DisposableSingleObserver<InsertTodoResponse?> {
        return object : DisposableSingleObserver<InsertTodoResponse?>() {

            override fun onSuccess(insertTodoResponse: InsertTodoResponse) {
                Log.d(TAG, "Insert Todo Response: $insertTodoResponse")

                if (insertTodoResponse != null && insertTodoResponse.error == "false") {
                    makeTodoUpToDate(todoToInsert)
                    if (isLastInsertTodoRequest) {
                        isInsertTodoRequestsFinished = true
                        if (isAllTodoRequestsFinished) {
                            onSyncTodoDataListener!!.onFinishSyncTodoData()
                        }
                    }
                } else if (insertTodoResponse != null) {
                    var message = insertTodoResponse.getMessage()

                    if (message == null) message = "Unknown error"
                    onSyncTodoDataListener!!.onSyncError(message)

                    if (isLastInsertTodoRequest) {
                        isInsertTodoRequestsFinished = true
                        if (isAllTodoRequestsFinished) {
                            onSyncTodoDataListener!!.onFinishSyncTodoData()
                        }
                    }
                }
            }

            override fun onError(throwable: Throwable) {
                Log.d(TAG, "Insert Todo Response - onFailure: $throwable")

                onSyncTodoDataListener!!.onSyncError(throwable.toString())

                if (isLastInsertTodoRequest) {
                    isInsertTodoRequestsFinished = true
                    if (isAllTodoRequestsFinished) {
                        onSyncTodoDataListener!!.onFinishSyncTodoData()
                    }
                }
            }
        }
    }

    private fun updateTodosInLocalDatabase(todos: ArrayList<Todo?>?) {
        for (todo in todos!!) {
            val exists = dbLoader.isTodoExists(todo!!.todoOnlineId)
            if (!exists) {
                dbLoader.createTodo(todo)
            } else {
                dbLoader.updateTodo(todo)
                dbLoader.fixTodoPositions(null)
            }
        }
    }

    private fun updateOrInsertTodos() {
        val call = apiService.getNextRowVersion(
                DbConstants.Todo.DATABASE_TABLE, dbLoader.apiKey
        )

        call!!.enqueue(object : Callback<GetNextRowVersionResponse?> {
            override fun onResponse(
                    call: Call<GetNextRowVersionResponse?>,
                    response: Response<GetNextRowVersionResponse?>
            ) {
                Log.d(TAG, "Get Next Row Version Response: " + ResponseToJson(response))

                if (IsNoError(response)) {
                    nextRowVersion = if (response.body() != null) response.body()!!.nextRowVersion!! else 0

                    setRowVersionsForTodos(todosToUpdate)
                    setRowVersionsForTodos(todosToInsert)

                    updateTodos()
                    insertTodos()
                } else if (response.body() != null) {
                    var message = response.body()!!.getMessage()

                    if (message == null) message = "Unknown error"
                    onSyncTodoDataListener!!.onSyncError(message)
                }
            }

            override fun onFailure(call: Call<GetNextRowVersionResponse?>, t: Throwable) {
                Log.d(TAG, "Get Next Row Version Response - onFailure: $t")
            }
        })
    }

    private val isLastInsertTodoRequest: Boolean
        private get() = if (currentInsertTodoRequest++ == insertTodoRequestCount) {
            true
        } else {
            false
        }

    private val isLastUpdateTodoRequest: Boolean
        private get() = if (currentUpdateTodoRequest++ == updateTodoRequestCount) {
            true
        } else {
            false
        }

    private val isAllTodoRequestsFinished: Boolean
        private get() = isUpdateTodoRequestsFinished && isInsertTodoRequestsFinished

    interface OnSyncTodoDataListener {
        fun onFinishSyncTodoData()
        fun onSyncError(errorMessage: String?)
    }

    companion object {
        private val TAG = TodoDataSynchronizer::class.java.simpleName
    }

    init {
        Objects.requireNonNull(instance)?.appComponent?.inject(this)
        apiService = retrofit.create(ApiService::class.java)
    }
}
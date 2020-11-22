package com.rolandvitezhu.todocloud.ui.activity.main.viewmodel

import androidx.databinding.Bindable
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rolandvitezhu.todocloud.BR
import com.rolandvitezhu.todocloud.app.AppController.Companion.instance
import com.rolandvitezhu.todocloud.data.Todo
import com.rolandvitezhu.todocloud.database.TodoCloudDatabaseDao
import com.rolandvitezhu.todocloud.helper.OnlineIdGenerator
import com.rolandvitezhu.todocloud.helper.SharedPreferencesHelper
import com.rolandvitezhu.todocloud.receiver.ReminderSetter
import com.rolandvitezhu.todocloud.repository.TodoRepository
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.SearchFragment
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.TodoListFragment
import kotlinx.coroutines.launch
import org.threeten.bp.*
import javax.inject.Inject

class TodosViewModel : ObservableViewModel() {

    @Inject
    lateinit var todoRepository: TodoRepository
    @Inject
    lateinit var todoCloudDatabaseDao: TodoCloudDatabaseDao

    private val _todos = MutableLiveData<ArrayList<Todo>>()
    val todos: LiveData<ArrayList<Todo>>
        get() = _todos
    var todo: Todo = Todo()
    var todosTitle: String = ""
    var whereCondition: String = ""
    var listOnlineId: String = ""
    var isPredefinedList = false
    val onCompletedPredefinedList: Boolean
        get() = whereCondition.contains("completed = 1")
    var todoTitle
        @Bindable
        get() = todo.title
        set(value) {
            todo.title = value
            notifyPropertyChanged(BR.todoTitle)
        }
    var todoDueDate
        @Bindable
        get() = todo.dueDate
        set(value) {
            todo.dueDate = value
            notifyPropertyChanged(BR.todoDueDate)
        }
    var todoReminderDateTime
        @Bindable
        get() = todo.reminderDateTime
        set(value) {
            todo.reminderDateTime = value
            notifyPropertyChanged(BR.todoReminderDateTime)
        }
    var originalTitle = ""

    var ldDueDate: LocalDate? = null
    private var zdtDueDate: ZonedDateTime? = null

    var ldtReminderDateTime: LocalDateTime? = null
    private var zdtReminderDateTime: ZonedDateTime? = null

    var shouldNavigateBack: Boolean = false

    private fun isSetReminder() = !todo.reminderDateTime.equals("-1")
    private fun isNotCompleted() = todo.completed?.not() ?: true
    private fun isNotDeleted() = todo.deleted?.not() ?: true
    private fun shouldCreateReminderService() = isNotCompleted() && isNotDeleted()

    /**
     * Clear the list of the todo items. Set the value of the isPredifinedList variable and clear
     * the listOnlineId variable or the whereCondition variable. The TodosViewModel exactly know
     * which list is open at the moment using these 3 variables.
     */
    private fun initTodosViewModel(isPredifinedList: Boolean) {
        _todos.value?.clear()
        this.isPredefinedList = isPredifinedList
        if (isPredefinedList)
            listOnlineId = "";
        else
            whereCondition = "";
    }

    /**
     * Update the list of todos by the where condition and set the title. We use it for
     * predefined lists and search lists.
     */
    suspend fun updateTodosViewModelByWhereCondition(title: String, whereCondition: String) {
        initTodosViewModel(true)
        this.todosTitle = title
        this.whereCondition = whereCondition
        _todos.value = todoCloudDatabaseDao.getTodosByWhereCondition(whereCondition)
    }

    /**
     * Update the list of todos by the listOnlineId and set the title. We use it for regular lists
     * which the user creates.
     */
    suspend fun updateTodosViewModelByListOnlineId(title: String, listOnlineId: String) {
        initTodosViewModel(false)
        this.todosTitle = title
        this.listOnlineId = listOnlineId
        _todos.value = todoCloudDatabaseDao.getTodosByListOnlineId(listOnlineId)
    }

    /**
     * Update the list of todos and set the title. We use it when the list which is related to
     * the TodosViewModel is already open.
     */
    suspend fun updateTodosViewModel() {
        if (isPredefinedList)
            updateTodosViewModelByWhereCondition(todosTitle, whereCondition)
        else
            updateTodosViewModelByListOnlineId(todosTitle, todo.listOnlineId!!)
    }

    /**
     * Synchronize the todos between the local and the remote databases.
     */
    suspend fun onSynchronization() {
        todoRepository.syncTodoData()
    }

    /**
     * Mark the todos as deleted in the local database and update the todos.
     */
    fun onSoftDelete(itemsToDelete: java.util.ArrayList<*>?, targetFragment: Fragment?) {
        val todos: java.util.ArrayList<Todo>? = itemsToDelete as java.util.ArrayList<Todo>?

        viewModelScope.launch {
            if (todos != null) {
                for (todoToSoftDelete in todos) {
                    todoCloudDatabaseDao.softDeleteTodo(todoToSoftDelete)
                    ReminderSetter.cancelReminderService(todoToSoftDelete)
                }
            }
            updateTodosViewModel()
        }

        when (targetFragment) {
            is TodoListFragment? -> targetFragment?.finishActionMode()
            is SearchFragment? -> targetFragment?.finishActionMode()
        }
    }

    /**
     * Mark the todo as deleted in the local database and update the todos.
     */
    fun onSoftDelete(onlineId: String?, targetFragment: Fragment?) {
        viewModelScope.launch {
            val todo = todoCloudDatabaseDao.getTodo(onlineId)
            todo?.let{ todoCloudDatabaseDao.softDeleteTodo(it) }
            updateTodosViewModel()
            ReminderSetter.cancelReminderService(todo)
            when (targetFragment) {
                is TodoListFragment? -> targetFragment?.finishActionMode()
                is SearchFragment? -> targetFragment?.finishActionMode()
            }
        }
    }

    /**
     * Set the initial values of the todo.
     */
    fun initializeTodo() {
        todo = Todo()

        todo.dirty = true
        if (listOnlineId.isNotEmpty())
            todo.listOnlineId = listOnlineId
        todo.completed = onCompletedPredefinedList
        viewModelScope.launch {
            todo.position = todoCloudDatabaseDao.getNextFirstTodoPosition()
        }

        initTodoDueDate()
        setTodoReminderDateTime(null)
    }

    /**
     * Create a todo, insert it into the local database, update the todos, set the reminder for the
     * todo, if should.
     */
    fun onCreateTodo() {
        viewModelScope.launch {
            createTodoInLocalDatabase()
            updateTodosViewModel()
            createReminderService()
        }
    }

    // Todo: Remove the suspend modifier, if it is not necessary.
    private fun createReminderService() {
        if (isSetReminder() && isNotCompleted()) {
            ReminderSetter.createReminderService(todo)
        }
    }

    /**
     * Modify the todo in the local database, update the todos and set the reminder for the todo.
     */
    fun onModifyTodo() {
        shouldNavigateBack = true
        viewModelScope.launch {
            todoCloudDatabaseDao.updateTodo(todo)
            todoCloudDatabaseDao.fixTodoPositions()
            updateTodosViewModel()

            if (isSetReminder()) {
                if (shouldCreateReminderService()) {
                    ReminderSetter.createReminderService(todo)
                }
            } else {
                ReminderSetter.cancelReminderService(todo)
            }
        }
    }

    /**
     * Create a todo and insert it into the local database.
     */
    private suspend fun createTodoInLocalDatabase() {
        todo.userOnlineId = todoCloudDatabaseDao.getCurrentUserOnlineId()
        todo._id = todoCloudDatabaseDao.insertTodo(todo)
        val todoOnlineId = OnlineIdGenerator.generateOnlineId(
                "todo",
                todo._id!!,
                todoCloudDatabaseDao.getCurrentApiKey()
        )
        todo.todoOnlineId = todoOnlineId
        todoCloudDatabaseDao.updateTodo(todo)
        todoCloudDatabaseDao.fixTodoPositions()
    }

    /**
     * Set the initial value of the todo's due date, which is the current date.
     */
    private fun initTodoDueDate() {
        ldDueDate = LocalDate.now()
        zdtDueDate = ldDueDate?.atStartOfDay(ZoneId.systemDefault())
        todoDueDate = zdtDueDate?.toInstant()?.toEpochMilli() ?: 0L
    }

    /**
     * Set the initial value of the due date related variables using the todo's dueDate variable.
     */
    private fun initDueDateByTodo() {
        if (todo.dueDate != 0L) {
            ldDueDate =
                    Instant.ofEpochMilli(todo.dueDate).atZone(ZoneId.systemDefault()).toLocalDate()
            zdtDueDate = ldDueDate?.atStartOfDay(ZoneId.systemDefault())
        }
    }

    /**
     * Set the initial value of the reminder date time related variables using the todo's
     * reminderDateTime variable.
     */
    private fun initReminderDateTimeByTodo() {
        if (todo.reminderDateTime != 0L) {
            ldtReminderDateTime = Instant.ofEpochMilli(todo.reminderDateTime)
                    .atZone(ZoneId.systemDefault()).toLocalDateTime()
            zdtReminderDateTime = ldtReminderDateTime?.atZone(ZoneId.systemDefault())
        }
    }

    /**
     * Set the due date of the todo.
     */
    fun setTodoDueDate(date: LocalDate?) {
        ldDueDate = date
        zdtDueDate = ldDueDate?.atStartOfDay(ZoneId.systemDefault())
        todoDueDate = zdtDueDate?.toInstant()?.toEpochMilli() ?: 0L
    }

    /**
     * Set the initial value of the todo's reminder date time, which is the current date time.
     */
    private fun initReminderDateTime() {
        ldtReminderDateTime = LocalDateTime.now()
        zdtReminderDateTime = ldtReminderDateTime?.atZone(ZoneId.systemDefault())
        todoReminderDateTime = zdtReminderDateTime?.toInstant()?.toEpochMilli() ?: 0L
    }

    /**
     * Set the reminder due date of the todo.
     */
    fun setTodoReminderDateTime(date: LocalDateTime?) {
        ldtReminderDateTime = date
        zdtReminderDateTime = ldtReminderDateTime?.atZone(ZoneId.systemDefault())
        todoReminderDateTime = zdtReminderDateTime?.toInstant()?.toEpochMilli() ?: 0L
    }

    /**
     * Clear the todo's due date and the value of all the variables needed to set the todo's due
     * date.
     */
    fun clearDueDate() {
        todoDueDate = 0L
        ldDueDate = LocalDate.now()
        zdtDueDate = ldDueDate?.atStartOfDay(ZoneId.systemDefault())
    }

    /**
     * Clear the todo's reminder date time and the value of all the variables needed to set the
     * todo's reminder date time.
     */
    fun clearReminderDateTime() {
        todoReminderDateTime = 0L
        ldtReminderDateTime = LocalDateTime.now()
        zdtReminderDateTime = ldtReminderDateTime?.atZone(ZoneId.systemDefault())
    }

    /**
     * Set the initial values to prepare the ViewModel to modify the todo.
     */
    fun initToModifyTodo(todo: Todo) {
        this.todo = todo
        originalTitle = this.todo.title ?: ""
        initDueDateByTodo()
        initReminderDateTimeByTodo()
        shouldNavigateBack = false
    }

    /**
     * Sort the todos by due date in an ascending or descending order. Every 1st order will be
     * an ascending one and every 2nd a descending.
     */
    fun sortByDueDate() {
        viewModelScope.launch {
            val originalTodos = deepCopyTodos()

            val isSortByDueDateAsc = SharedPreferencesHelper.getPreference(
                    SharedPreferencesHelper.PREFERENCE_NAME_SORT,
                    SharedPreferencesHelper.KEY_SORT_BY_DUE_DATE_ASC
            )

            todos.value?.let {
                if (isSortByDueDateAsc) {
                    sortTodosByDueDate(it, true)
                } else {
                    sortTodosByDueDate(it, false)
                }
            }

            // Invert the preference value
            SharedPreferencesHelper.setBooleanPreference(
                    SharedPreferencesHelper.PREFERENCE_NAME_SORT,
                    SharedPreferencesHelper.KEY_SORT_BY_DUE_DATE_ASC,
                    !isSortByDueDateAsc
            )

            fixTodoPositionValuesAndUpdateTodos(originalTodos)
        }
    }

    /**
     * Deep copy the todos into an ArrayList.
     */
    private fun deepCopyTodos(): ArrayList<Todo> {
        val originalTodos = ArrayList<Todo>()

        // Deep copy todos
        todos.value?.let {
            for (i in it.indices) {
                originalTodos.add(Todo(it[i]))
            }
        }
        return originalTodos
    }

    /**
     * Fix the position values of the re-ordered todos by the todos that are in the original order,
     * and update the todos in the local database. Fix the position values in the local database,
     * if there are duplications.
     */
    private suspend fun fixTodoPositionValuesAndUpdateTodos(originalTodos: ArrayList<Todo>) {
        todos.value?.let {
            // Fix position values
            for (i in it.indices) {
                val correctPosition = originalTodos[i].position
                it[i].position = correctPosition

                todoCloudDatabaseDao.updateTodo(it[i])
            }
        }

        todoCloudDatabaseDao.fixTodoPositions()
    }

    /**
     * Sort the todos by priority. Every 1st order will be an ascending one and every 2nd a
     * descending.
     */
    fun sortByPriority() {
        viewModelScope.launch {
            val originalTodos = deepCopyTodos()

            val isSortByPriority = SharedPreferencesHelper.getPreference(
                    SharedPreferencesHelper.PREFERENCE_NAME_SORT,
                    SharedPreferencesHelper.KEY_SORT_BY_PRIORITY
            )

            todos.value?.let {
                if (isSortByPriority)
                    sortTodosByPriority(it, true)
                else
                    sortTodosByPriority(it, false)
            }

            // Invert the preference value
            SharedPreferencesHelper.setBooleanPreference(
                    SharedPreferencesHelper.PREFERENCE_NAME_SORT,
                    SharedPreferencesHelper.KEY_SORT_BY_PRIORITY,
                    !isSortByPriority
            )

            fixTodoPositionValuesAndUpdateTodos(originalTodos)
        }
    }

    /**
     * Sort todos by priority or by not priority.
      */
    private fun sortTodosByPriority(todos: ArrayList<Todo>, isPriority: Boolean) {
        val todosToSort: MutableList<Todo> = ArrayList()
        todosToSort.addAll(todos)
        if (isPriority)
            todosToSort.sortBy { todo -> todo.priority }
        else
            todosToSort.sortBy { todo -> todo.priority?.not() }

        _todos.value = ArrayList(todosToSort)
    }

    /**
     * Sort todos by due date in ascending or descending order.
     */
    private fun sortTodosByDueDate(todos: ArrayList<Todo>, isAscending: Boolean) {
        val todosToSort: MutableList<Todo> = ArrayList()
        todosToSort.addAll(todos)
        if (isAscending)
            todosToSort.sortBy { todo -> todo.dueDate }
        else
            todosToSort.sortByDescending { todo -> todo.dueDate }

        _todos.value = ArrayList(todosToSort)
    }

    init {
        instance?.appComponent?.inject(this)
    }
}
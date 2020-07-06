package com.rolandvitezhu.todocloud.ui.activity.main.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController.Companion.instance
import com.rolandvitezhu.todocloud.app.AppController.Companion.isActionMode
import com.rolandvitezhu.todocloud.data.Todo
import com.rolandvitezhu.todocloud.datastorage.DbLoader
import com.rolandvitezhu.todocloud.di.FragmentScope
import com.rolandvitezhu.todocloud.helper.SharedPreferencesHelper
import com.rolandvitezhu.todocloud.receiver.ReminderSetter
import com.rolandvitezhu.todocloud.ui.activity.main.adapter.TodoAdapter.ItemViewHolder
import kotlinx.android.synthetic.main.item_todo.view.*
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@FragmentScope
class TodoAdapter @Inject constructor() : RecyclerView.Adapter<ItemViewHolder>() {

    @Inject
    lateinit var dbLoader: DbLoader

    private val todos: MutableList<Todo>
    private var itemTouchHelper: ItemTouchHelper? = null
    var isDraggingEnabled: Boolean = true

    fun update(todos: ArrayList<Todo>) {
        this.todos.clear()

        // Order todo list items ascending by position value
        ArrayList(todos).sortWith(compareBy<Todo> { it.position })
        this.todos.addAll(todos)
    }

    fun sortByDueDate() {
        val originalTodos = ArrayList<Todo>()
        val isSortByDueDateAsc = SharedPreferencesHelper.getPreference(
                SharedPreferencesHelper.PREFERENCE_NAME_SORT,
                SharedPreferencesHelper.KEY_SORT_BY_DUE_DATE_ASC
        )

        // Deep copy todos
        for (i in todos.indices) {
            originalTodos.add(Todo(todos[i]))
        }
        if (isSortByDueDateAsc) {
            // Order todo list items ascending by due date
            todos.sortBy { todo -> todo.dueDate }
        } else {
            // Order todo list items descending by due date
            todos.sortByDescending { todo -> todo.dueDate }
        }

        // Invert pref value
        SharedPreferencesHelper.setBooleanPreference(
                SharedPreferencesHelper.PREFERENCE_NAME_SORT,
                SharedPreferencesHelper.KEY_SORT_BY_DUE_DATE_ASC,
                !isSortByDueDateAsc
        )

        // Fix position values
        for (i in todos.indices) {
            val rightPosition = originalTodos[i].position!!
            todos[i].position = rightPosition
            dbLoader.updateTodo(todos[i])
        }
        dbLoader.fixTodoPositions(null)
    }

    fun sortByPriority() {
        val originalTodos = ArrayList<Todo>()
        val isSortByPriority = SharedPreferencesHelper.getPreference(
                SharedPreferencesHelper.PREFERENCE_NAME_SORT,
                SharedPreferencesHelper.KEY_SORT_BY_PRIORITY
        )

        // Deep copy todos
        for (i in todos.indices) {
            originalTodos.add(Todo(todos[i]))
        }
        if (isSortByPriority) {
            // Order todo list items by priority
            todos.sortBy { todo -> todo.priority }
        } else {
            // Order todo list items by not priority
            todos.sortBy { todo -> todo.priority?.not() }
        }

        // Invert pref value
        SharedPreferencesHelper.setBooleanPreference(
                SharedPreferencesHelper.PREFERENCE_NAME_SORT,
                SharedPreferencesHelper.KEY_SORT_BY_PRIORITY,
                !isSortByPriority
        )

        // Fix position values
        for (i in todos.indices) {
            val rightPosition = originalTodos[i].position!!
            todos[i].position = rightPosition
            dbLoader.updateTodo(todos[i])
        }
        dbLoader.fixTodoPositions(null)
    }

    fun clear() {
        todos.clear()
        notifyDataSetChanged()
    }

    fun setItemTouchHelper(itemTouchHelper: ItemTouchHelper?) {
        this.itemTouchHelper = itemTouchHelper
    }

    fun getTodos(): List<Todo> {
        return todos
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_todo, parent, false)
        return ItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val todo = todos[position]
        holder.bindView(todo, itemTouchHelper)
    }

    fun removeTodoFromAdapter(position: Int) {
        todos.removeAt(position)
        notifyItemRemoved(position)
    }

    private fun handleReminderService(todo: Todo) {
        if (todo.completed!!) {
            ReminderSetter.cancelReminderService(todo)
        } else if (isSetReminder(todo)) {
            ReminderSetter.createReminderService(todo)
        }
    }

    private fun isSetReminder(todo: Todo): Boolean {
        return todo.reminderDateTime!! > 0
    }

    override fun getItemCount(): Int {
        return todos.size
    }

    fun getTodo(position: Int): Todo {
        return todos[position]
    }

    fun toggleSelection(position: Int) {
        todos[position].isSelected = isNotSelected(position)
        notifyItemChanged(position)
    }

    fun clearSelection() {
        for (todo in todos) {
            todo.isSelected = false
        }
        notifyDataSetChanged()
    }

    val selectedItemCount: Int
        get() {
            var selectedItemCount = 0
            todos.forEach {
                if (it.isSelected!!)
                    selectedItemCount++
            }
            return selectedItemCount
        }

    val selectedTodos: ArrayList<Todo>
        get() {
            val selectedTodos = ArrayList<Todo>(selectedItemCount)
            todos.forEach {
                if (it.isSelected!!) selectedTodos.add(it)
            }
            return selectedTodos
        }

    private fun isNotSelected(position: Int): Boolean {
        return !todos[position].isSelected!!
    }

    private fun updateTodo(todo: Todo) {
        todo.dirty = true
        dbLoader.updateTodo(todo)
        dbLoader.fixTodoPositions(null)
    }

    private fun shouldHandleCheckBoxTouchEvent(event: MotionEvent, holder: ItemViewHolder): Boolean {
        // To reproduce "holder.getAdapterPosition() == -1", do the following: select 1 todo and
        // touch it's CheckBox.
        return (!isActionMode()
                && event.action == MotionEvent.ACTION_UP && holder.adapterPosition != -1)
    }

    private fun toggleCompleted(todo: Todo) {
        todo.completed = !todo.completed!!
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindView(todo: Todo, itemTouchHelper: ItemTouchHelper?) {
            itemView.checkbox_todo_completed.isChecked = todo.completed!!
            itemView.textview_todo_title.text = todo.title
            itemView.textview_todo_duedate.text = todo.formattedDueDateForListItem
            if (todo.priority != null && todo.priority == true)
                itemView.imageview_todo_priority.visibility = View.VISIBLE
            else
                itemView.imageview_todo_priority.visibility = View.GONE
            itemView.imageview_todo_draghandle.visibility =
                    if (isActionMode() && isDraggingEnabled) View.VISIBLE
                    else View.GONE
            itemView.imageview_todo_draghandle.setOnTouchListener { v, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper!!.startDrag(this)
                }
                false
            }
            itemView.isActivated = todo.isSelected!!
            itemView.checkbox_todo_completed.setOnTouchListener { v, event ->
                if (shouldHandleCheckBoxTouchEvent(event, this)) {
                    toggleCompleted(todo)
                    updateTodo(todo)
                    removeTodoFromAdapter(adapterPosition)
                    handleReminderService(todo)
                }
                true
            }
        }
    }

    init {
        Objects.requireNonNull(instance)?.appComponent?.fragmentComponent()?.create()?.inject(this)
        todos = ArrayList()
    }
}
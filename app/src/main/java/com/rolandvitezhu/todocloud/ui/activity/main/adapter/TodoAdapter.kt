package com.rolandvitezhu.todocloud.ui.activity.main.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.rolandvitezhu.todocloud.app.AppController.Companion.instance
import com.rolandvitezhu.todocloud.app.AppController.Companion.isActionMode
import com.rolandvitezhu.todocloud.data.Todo
import com.rolandvitezhu.todocloud.database.TodoCloudDatabaseDao
import com.rolandvitezhu.todocloud.databinding.ItemTodoBinding
import com.rolandvitezhu.todocloud.di.FragmentScope
import com.rolandvitezhu.todocloud.receiver.ReminderSetter
import com.rolandvitezhu.todocloud.ui.activity.main.adapter.TodoAdapter.ItemViewHolder
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@FragmentScope
class TodoAdapter @Inject constructor() : RecyclerView.Adapter<ItemViewHolder>() {

    @Inject
    lateinit var todoCloudDatabaseDao: TodoCloudDatabaseDao

    private val todos: MutableList<Todo>
    var itemTouchHelper: ItemTouchHelper? = null

    fun update(todos: MutableList<Todo>) {
        this.todos.clear()

        // Order todo list items ascending by position value
        ArrayList(todos).sortWith(compareBy<Todo> { it.position })
        this.todos.addAll(todos)
    }

    fun clear() {
        todos.clear()
        notifyDataSetChanged()
    }

    fun getTodos(): List<Todo> {
        return todos
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder.from(parent, this)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val todo = todos[position]
        holder.bindView(todo, itemTouchHelper)
    }

    fun removeTodoFromAdapter(position: Int) {
        todos.removeAt(position)
        notifyItemRemoved(position)
    }

    fun handleReminderService(todo: Todo) {
        if (todo.completed != null && todo.completed!!) {
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
                if (it.isSelected)
                    selectedItemCount++
            }
            return selectedItemCount
        }

    val selectedTodos: ArrayList<Todo>
        get() {
            val selectedTodos = ArrayList<Todo>(selectedItemCount)
            todos.forEach {
                if (it.isSelected) selectedTodos.add(it)
            }
            return selectedTodos
        }

    private fun isNotSelected(position: Int): Boolean {
        return !todos[position].isSelected
    }

    suspend fun updateTodo(todo: Todo) {
        todo.dirty = true
        todoCloudDatabaseDao.updateTodo(todo)
        todoCloudDatabaseDao.fixTodoPositions()
    }

    fun shouldHandleCheckBoxTouchEvent(event: MotionEvent, holder: ItemViewHolder): Boolean {
        // To reproduce "holder.getAdapterPosition() == -1", do the following: select 1 todo and
        // touch it's CheckBox.
        return (!isActionMode()
                && event.action == MotionEvent.ACTION_UP && holder.adapterPosition != -1)
    }

    fun toggleCompleted(todo: Todo) {
        todo.completed = todo.completed?.not() ?: true
    }

    class ItemViewHolder private constructor(val binding: ItemTodoBinding, val todoAdapter: TodoAdapter):
            RecyclerView.ViewHolder(binding.root) {

        fun bindView(todo: Todo, itemTouchHelper: ItemTouchHelper?) {
            binding.todo = todo
            binding.todoAdapter = todoAdapter
            binding.itemViewHolder = this
            binding.executePendingBindings()
        }

        companion object {

            fun from(parent: ViewGroup, todoAdapter: TodoAdapter): ItemViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemTodoBinding.inflate(
                        layoutInflater, parent, false)

                return ItemViewHolder(binding, todoAdapter)
            }
        }
    }

    init {
        Objects.requireNonNull(instance)?.appComponent?.fragmentComponent()?.create()?.inject(this)
        todos = ArrayList()
    }
}
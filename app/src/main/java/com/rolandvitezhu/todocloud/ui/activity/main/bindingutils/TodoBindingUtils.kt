package com.rolandvitezhu.todocloud.ui.activity.main.bindingutils

import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rolandvitezhu.todocloud.app.AppController
import com.rolandvitezhu.todocloud.data.Todo
import com.rolandvitezhu.todocloud.listener.RecyclerViewOnItemTouchListener
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity
import com.rolandvitezhu.todocloud.ui.activity.main.adapter.TodoAdapter
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.SearchFragment
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.TodoListFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@BindingAdapter("todoSelectedState")
fun ConstraintLayout.setTodoSelectedState(todo: Todo) {
    isActivated = todo.isSelected
}

@BindingAdapter("todoCompletedCheckedState")
fun AppCompatCheckBox.setTodoCompletedCheckedState(todo: Todo) {
    isChecked = todo.completed ?: false
}

@BindingAdapter("todoPriority")
fun ImageView.setTodoPriority(todo: Todo) {
    if (todo.priority != null && todo.priority == true)
        visibility = View.VISIBLE
    else
        visibility = View.GONE
}

@BindingAdapter("todoDragHandleVisible")
fun ImageView.setTodoDragHandleVisible(todo: Todo) {
    visibility =
            if (AppController.isActionMode() && AppController.isDraggingEnabled)
                View.VISIBLE
            else
                View.GONE
}

@BindingAdapter(value = ["dragHandleOnTouchListenerTodoAdapter",
    "dragHandleOnTouchListenerItemViewHolder"])
fun ImageView.setDragHandleOnTouchListener(
        todoAdapter: TodoAdapter, itemViewHolder: TodoAdapter.ItemViewHolder) {
    setOnTouchListener { v, event ->
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            todoAdapter.itemTouchHelper?.startDrag(itemViewHolder)
        }
        false
    }
}

@BindingAdapter(value = ["checkBoxTodoCompletedOnTouchListenerTodoAdapter",
"checkBoxTodoCompletedOnTouchListenerItemViewHolder", "checkBoxTodoCompletedOnTouchListenerTodo"])
fun AppCompatCheckBox.setCheckBoxTodoCompletedOnTouchListener(
        todoAdapter: TodoAdapter, itemViewHolder: TodoAdapter.ItemViewHolder, todo: Todo) {
    setOnTouchListener { v, event ->
        if (todoAdapter.shouldHandleCheckBoxTouchEvent(event, itemViewHolder)) {
            todoAdapter.toggleCompleted(todo)
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch { todoAdapter.updateTodo(todo) }
            todoAdapter.removeTodoFromAdapter(itemViewHolder.adapterPosition)
            todoAdapter.handleReminderService(todo)
        }
        true
    }
}

@BindingAdapter("todoListItemTouchListener")
fun RecyclerView.setTodoListItemTouchListener(todoListFragment: TodoListFragment) {
    addOnItemTouchListener(RecyclerViewOnItemTouchListener(
            context,
            this,
            object : RecyclerViewOnItemTouchListener.OnClickListener {
                override fun onClick(childView: View, childViewAdapterPosition: Int) {
                    if (!todoListFragment.isActionMode()) {
                        todoListFragment.openModifyTodoFragment(childViewAdapterPosition)
                    } else {
                        todoListFragment.todoAdapter.toggleSelection(childViewAdapterPosition)
                        if (todoListFragment.areSelectedItems()) {
                            todoListFragment.actionMode?.invalidate()
                        } else {
                            todoListFragment.actionMode?.finish()
                        }
                    }
                }

                override fun onLongClick(childView: View, childViewAdapterPosition: Int) {
                    if (!todoListFragment.isActionMode()) {
                        (todoListFragment.activity as MainActivity?)?.
                        onStartActionMode(todoListFragment.callback)
                        todoListFragment.todoAdapter.toggleSelection(childViewAdapterPosition)
                        todoListFragment.actionMode?.invalidate()
                    }
                }
            }
    )
    )
}

@BindingAdapter("searchItemTouchListener")
fun RecyclerView.setSearchItemTouchListener(searchFragment: SearchFragment) {
    addOnItemTouchListener(RecyclerViewOnItemTouchListener(
            context,
            this,
            object : RecyclerViewOnItemTouchListener.OnClickListener {
                override fun onClick(childView: View, childViewAdapterPosition: Int) {
                    if (!searchFragment.isActionMode()) {
                        searchFragment.openModifyTodoFragment(childViewAdapterPosition)
                    } else {
                        searchFragment.todoAdapter.toggleSelection(childViewAdapterPosition)
                        if (searchFragment.areSelectedItems()) {
                            searchFragment.actionMode?.invalidate()
                        } else {
                            searchFragment.actionMode?.finish()
                        }
                    }
                }

                override fun onLongClick(childView: View, childViewAdapterPosition: Int) {
                    if (!searchFragment.isActionMode()) {
                        (searchFragment.activity as MainActivity?)?.
                        onStartActionMode(searchFragment.callback)
                        searchFragment.todoAdapter.toggleSelection(childViewAdapterPosition)
                        searchFragment.actionMode?.invalidate()
                    }
                }
            }
    )
    )
}
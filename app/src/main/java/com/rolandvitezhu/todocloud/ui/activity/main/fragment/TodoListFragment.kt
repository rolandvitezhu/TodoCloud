package com.rolandvitezhu.todocloud.ui.activity.main.fragment

import android.content.Context
import android.content.DialogInterface
import android.os.AsyncTask
import android.os.Bundle
import android.view.*
import androidx.appcompat.view.ActionMode
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController
import com.rolandvitezhu.todocloud.app.AppController.Companion.instance
import com.rolandvitezhu.todocloud.data.Todo
import com.rolandvitezhu.todocloud.databinding.FragmentTodolistBinding
import com.rolandvitezhu.todocloud.datastorage.DbLoader
import com.rolandvitezhu.todocloud.datastorage.asynctask.UpdateViewModelTask
import com.rolandvitezhu.todocloud.listener.RecyclerViewOnItemTouchListener
import com.rolandvitezhu.todocloud.receiver.ReminderSetter
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity
import com.rolandvitezhu.todocloud.ui.activity.main.adapter.TodoAdapter
import com.rolandvitezhu.todocloud.ui.activity.main.dialog.SortTodoListDialog
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.ConfirmDeleteDialogFragment
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.ListsViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.PredefinedListsViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.TodosViewModel
import kotlinx.android.synthetic.main.layout_recyclerview_todolist.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.*
import javax.inject.Inject

class TodoListFragment : Fragment(), SortTodoListDialog.Presenter, DialogInterface.OnDismissListener {

    @Inject
    lateinit var dbLoader: DbLoader
    @Inject
    lateinit var todoAdapter: TodoAdapter

    private var actionMode: ActionMode? = null

    private var todosViewModel: TodosViewModel? = null
    private var listsViewModel: ListsViewModel? = null
    private var predefinedListsViewModel: PredefinedListsViewModel? = null
    private var swipedTodoAdapterPosition: Int? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Objects.requireNonNull(instance)?.appComponent?.fragmentComponent()?.create()?.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        todosViewModel = ViewModelProviders.of(activity!!).get(TodosViewModel::class.java)
        listsViewModel = ViewModelProviders.of(activity!!).get(ListsViewModel::class.java)
        predefinedListsViewModel = ViewModelProviders.of(activity!!).get(PredefinedListsViewModel::class.java)
        todosViewModel!!.todos.observe(
                this,
                Observer { todos ->
                    todoAdapter!!.update(todos)
                    todoAdapter!!.notifyDataSetChanged()
                }
        )
        updateTodosViewModel()
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View? {
        val fragmentTodoListBinding: FragmentTodolistBinding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_todolist, container, false)
        val view: View = fragmentTodoListBinding.root
        fragmentTodoListBinding.todoListFragment = this

        prepareRecyclerView(view)
        applyClickEvents(view)
        applySwipeToDismissAndDragToReorder(view)

        return view
    }

    override fun onResume() {
        super.onResume()
        setActionBarTitle()
    }

    private fun prepareRecyclerView(view: View) {
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(
                context!!.applicationContext
        )
        view.recyclerview_todolist!!.layoutManager = layoutManager
        view.recyclerview_todolist!!.adapter = todoAdapter
    }

    private fun applyClickEvents(view: View) {
        view.recyclerview_todolist!!.addOnItemTouchListener(RecyclerViewOnItemTouchListener(
                context!!.applicationContext,
                view.recyclerview_todolist,
                object : RecyclerViewOnItemTouchListener.OnClickListener {
                    override fun onClick(childView: View, childViewAdapterPosition: Int) {
                        if (!isActionMode()) {
                            openModifyTodoFragment(childViewAdapterPosition)
                        } else {
                            todoAdapter!!.toggleSelection(childViewAdapterPosition)
                            if (areSelectedItems()) {
                                actionMode!!.invalidate()
                            } else {
                                actionMode!!.finish()
                            }
                        }
                    }

                    override fun onLongClick(childView: View, childViewAdapterPosition: Int) {
                        if (!isActionMode()) {
                            (this@TodoListFragment.activity as MainActivity?)!!.onStartActionMode(callback)
                            todoAdapter!!.toggleSelection(childViewAdapterPosition)
                            actionMode!!.invalidate()
                        }
                    }
                }
        )
        )
    }

    private fun applySwipeToDismissAndDragToReorder(view: View) {
        val simpleItemTouchCallback: ItemTouchHelper.SimpleCallback = object : ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.START
        ) {
            override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
            ): Boolean {
                val todos: List<Todo?> = todoAdapter!!.getTodos()
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition

                // We swap the position of the todos on the list by moving them. We swap the
                // position values of the moved todos.
                if (fromPosition < toPosition) {
                    for (i in fromPosition until toPosition) {
                        swapItems(todos, i, i + 1)
                    }
                } else {
                    for (i in fromPosition downTo toPosition + 1) {
                        swapItems(todos, i, i - 1)
                    }
                }

                todoAdapter!!.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val swipedTodo = getSwipedTodo(viewHolder)
                swipedTodoAdapterPosition = viewHolder.adapterPosition
                openConfirmDeleteTodosDialog(swipedTodo, swipedTodoAdapterPosition!!)
            }

            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                val dragFlags: Int
                val swipeFlags: Int
                if (AppController.isActionMode()) {
                    dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                    swipeFlags = 0
                } else {
                    dragFlags = 0
                    swipeFlags = ItemTouchHelper.START
                }

                return ItemTouchHelper.Callback.makeMovementFlags(dragFlags, swipeFlags)
            }

            override fun isLongPressDragEnabled(): Boolean {
                return false
            }
        }

        val itemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(view.recyclerview_todolist)
        todoAdapter!!.setItemTouchHelper(itemTouchHelper)
    }

    /**
     * Change the todo position values.
     */
    private fun swapItems(todos: List<Todo?>, fromPosition: Int, toPosition: Int) {
        val todoFrom = todos[fromPosition]
        val todoTo = todos[toPosition]
        val tempTodoToPosition = todoFrom!!.position!!

        todoFrom.position = todoTo!!.position
        todoTo.position = tempTodoToPosition
        todoFrom.dirty = true
        todoTo.dirty = true

        Collections.swap(todos, fromPosition, toPosition)

        lifecycleScope.launch {
            persistSwappedItems(todoFrom, todoTo)
        }
    }

    /**
     * Update the position value of the todo items in the database as well and fix the duplications
     * of the positions if there are.
     */
    private suspend fun persistSwappedItems(todoFrom: Todo?, todoTo: Todo?) {
        withContext(Dispatchers.IO) {
            dbLoader!!.updateTodo(todoFrom)
            dbLoader!!.updateTodo(todoTo)
            dbLoader!!.fixTodoPositions(null)
        }
    }

    private fun getSwipedTodo(viewHolder: RecyclerView.ViewHolder): Todo {
        val swipedTodoAdapterPosition = viewHolder.adapterPosition
        return todoAdapter!!.getTodo(swipedTodoAdapterPosition)
    }

    private fun areSelectedItems(): Boolean {
        return todoAdapter!!.selectedItemCount > 0
    }

    private fun isActionMode(): Boolean {
        return actionMode != null
    }

    private fun openModifyTodoFragment(childViewAdapterPosition: Int) {
        val todo = todoAdapter!!.getTodo(childViewAdapterPosition)
        todosViewModel!!.todo = todo
        (activity as MainActivity?)!!.openModifyTodoFragment(this, null)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_todolist, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val menuItemId = item.itemId
        when (menuItemId) {
            R.id.menuitem_todolist_sort -> SortTodoListDialog(activity!!, this)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setActionBarTitle() {
        var actionBarTitle: String? = ""
        actionBarTitle = if (!todosViewModel!!.isPredefinedList) // List
            listsViewModel!!.list.title else  // PredefinedList
            predefinedListsViewModel!!.predefinedList.title
        (activity as MainActivity?)!!.onSetActionBarTitle(actionBarTitle)
    }

    private val callback: ActionMode.Callback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            setActionMode(mode)
            mode.menuInflater.inflate(R.menu.layout_appbar_todolist, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val title = prepareTitle()
            actionMode!!.title = title
            todoAdapter!!.notifyDataSetChanged()
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            val actionItemId = item.itemId
            when (actionItemId) {
                R.id.menuitem_layoutappbartodolist_delete -> openConfirmDeleteTodosDialog()
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            todoAdapter!!.clearSelection()
            setActionMode(null)
            todoAdapter!!.notifyDataSetChanged()
        }

        private fun prepareTitle(): String {
            val selectedItemCount = todoAdapter!!.selectedItemCount
            return selectedItemCount.toString() + " " + getString(R.string.all_selected)
        }
    }

    private fun setActionMode(actionMode: ActionMode?) {
        this.actionMode = actionMode
        AppController.setActionMode(actionMode)
    }

    private fun openConfirmDeleteTodosDialog() {
        val selectedTodos = todoAdapter!!.selectedTodos
        val arguments = Bundle()
        arguments.putString("itemType", "todo")
        arguments.putParcelableArrayList("itemsToDelete", selectedTodos)
        openConfirmDeleteDialogFragment(arguments)
    }

    private fun openConfirmDeleteTodosDialog(swipedTodo: Todo, swipedTodoAdapterPosition: Int) {
        val selectedTodos = ArrayList<Todo>()
        selectedTodos.add(swipedTodo)
        val arguments = Bundle()
        arguments.putString("itemType", "todo")
        arguments.putParcelableArrayList("itemsToDelete", selectedTodos)
        openConfirmDeleteDialogFragment(arguments, swipedTodoAdapterPosition)
    }

    private fun openConfirmDeleteDialogFragment(arguments: Bundle) {
        val confirmDeleteDialogFragment = ConfirmDeleteDialogFragment()
        confirmDeleteDialogFragment.setTargetFragment(this, 0)
        confirmDeleteDialogFragment.arguments = arguments
        confirmDeleteDialogFragment.show(fragmentManager!!, "ConfirmDeleteDialogFragment")
    }

    private fun openConfirmDeleteDialogFragment(
            arguments: Bundle, swipedTodoAdapterPosition: Int
    ) {
        val confirmDeleteDialogFragment = ConfirmDeleteDialogFragment()
        confirmDeleteDialogFragment.setTargetFragment(this, 0)
        confirmDeleteDialogFragment.arguments = arguments
        confirmDeleteDialogFragment.show(fragmentManager!!, "ConfirmDeleteDialogFragment")
    }

    private fun updateTodosViewModel() {
        val updateViewModelTask = UpdateViewModelTask(todosViewModel, activity)
        updateViewModelTask.execute()
    }

    fun onSoftDelete(onlineId: String?, itemType: String?) {
        val todoToSoftDelete = dbLoader!!.getTodo(onlineId)
        dbLoader!!.softDeleteTodo(todoToSoftDelete)
        updateTodosViewModel()
        ReminderSetter.cancelReminderService(todoToSoftDelete)
        if (actionMode != null) {
            actionMode!!.finish()
        }
    }

    fun onSoftDelete(itemsToDelete: ArrayList<*>, itemType: String?) {
        val todosToSoftDelete: ArrayList<Todo> = itemsToDelete as (ArrayList<Todo>)
        for (todoToSoftDelete in todosToSoftDelete) {
            dbLoader!!.softDeleteTodo(todoToSoftDelete)
            ReminderSetter.cancelReminderService(todoToSoftDelete)
        }
        updateTodosViewModel()
        if (actionMode != null) {
            actionMode!!.finish()
        }
    }

    override fun onSortByDueDatePushed() {
        SortAsyncTask(todoAdapter).execute(SortAsyncTask.SORT_BY_DUE_DATE)
    }

    override fun onSortByPriorityPushed() {
        SortAsyncTask(todoAdapter).execute(SortAsyncTask.SORT_BY_PRIORITY)
    }

    fun onFABClick(view: View) {
        if (isActionMode()) actionMode!!.finish()
        (this@TodoListFragment.activity as MainActivity?)!!.onOpenCreateTodoFragment(this@TodoListFragment)
    }

    override fun onDismiss(dialog: DialogInterface?) {
        if (swipedTodoAdapterPosition != null)
            todoAdapter.notifyItemChanged(swipedTodoAdapterPosition!!)
        else
            todoAdapter.notifyDataSetChanged()
    }

    private class SortAsyncTask internal constructor(context: TodoAdapter?) : AsyncTask<Int?, Long?, Void?>() {
        private val todoAdapterWeakReference: WeakReference<TodoAdapter?>

        override fun doInBackground(vararg params: Int?): Void? {
            when (params[0]) {
                SORT_BY_DUE_DATE -> todoAdapterWeakReference.get()!!.sortByDueDate()
                SORT_BY_PRIORITY -> todoAdapterWeakReference.get()!!.sortByPriority()
            }
            return null
        }

        override fun onPostExecute(aVoid: Void?) {
            super.onPostExecute(aVoid)
            val todoAdapter = todoAdapterWeakReference.get() ?: return
            todoAdapter.notifyDataSetChanged()
        }

        companion object {
            const val SORT_BY_DUE_DATE = 1001
            const val SORT_BY_PRIORITY = 1002
        }

        init {
            todoAdapterWeakReference = WeakReference(context)
        }
    }
}
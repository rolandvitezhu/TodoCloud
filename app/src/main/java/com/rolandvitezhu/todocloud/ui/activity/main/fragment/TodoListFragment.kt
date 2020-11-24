package com.rolandvitezhu.todocloud.ui.activity.main.fragment

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController
import com.rolandvitezhu.todocloud.app.AppController.Companion.instance
import com.rolandvitezhu.todocloud.app.AppController.Companion.isDraggingEnabled
import com.rolandvitezhu.todocloud.data.Todo
import com.rolandvitezhu.todocloud.database.TodoCloudDatabaseDao
import com.rolandvitezhu.todocloud.databinding.FragmentTodolistBinding
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity
import com.rolandvitezhu.todocloud.ui.activity.main.adapter.TodoAdapter
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.ConfirmDeleteDialogFragment
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.SortTodoListDialogFragment
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.ListsViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.PredefinedListsViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.TodosViewModel
import kotlinx.android.synthetic.main.layout_recyclerview_todolist.view.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

class TodoListFragment : Fragment(), DialogInterface.OnDismissListener {

    @Inject
    lateinit var todoCloudDatabaseDao: TodoCloudDatabaseDao
    @Inject
    lateinit var todoAdapter: TodoAdapter

    var actionMode: ActionMode? = null

    private val todosViewModel by lazy {
        activity?.let { ViewModelProvider(it).get(TodosViewModel::class.java) }
    }
    private val listsViewModel by lazy {
        activity?.let { ViewModelProvider(it).get(ListsViewModel::class.java) }
    }
    private val predefinedListsViewModel by lazy {
        activity?.let { ViewModelProvider(it).get(PredefinedListsViewModel::class.java) }
    }

    private var swipedTodoAdapterPosition: Int? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Objects.requireNonNull(instance)?.appComponent?.fragmentComponent()?.create()?.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        todosViewModel?.todos?.observe(
                this,
                Observer { todos ->
                    todos?.let { todoAdapter.update(it.toMutableList()) }
                    todoAdapter.notifyDataSetChanged()
                }
        )
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View? {
        val fragmentTodoListBinding: FragmentTodolistBinding =
                FragmentTodolistBinding.inflate(inflater, container, false)
        val view: View = fragmentTodoListBinding.root

        prepareRecyclerView()
        applySwipeToDismissAndDragToReorder(view)

        fragmentTodoListBinding.lifecycleOwner = this
        fragmentTodoListBinding.todoListFragment = this
        fragmentTodoListBinding.executePendingBindings()

        return view
    }

    override fun onResume() {
        super.onResume()
        setActionBarTitle()
    }

    private fun prepareRecyclerView() {
        isDraggingEnabled = true
    }

    private fun applySwipeToDismissAndDragToReorder(view: View) {
        val simpleItemTouchCallback: ItemTouchHelper.SimpleCallback =
                object : ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.START
        ) {
            override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
            ): Boolean {
                val todos: List<Todo?> = todoAdapter.getTodos()
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

                todoAdapter.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val swipedTodo = getSwipedTodo(viewHolder)
                swipedTodoAdapterPosition = viewHolder.adapterPosition
                swipedTodoAdapterPosition?.let { openConfirmDeleteTodosDialog(swipedTodo, it) }
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
        todoAdapter.itemTouchHelper = itemTouchHelper
    }

    /**
     * Change the todo position values.
     */
    private fun swapItems(todos: List<Todo?>, fromPosition: Int, toPosition: Int) {
        val todoFrom = todos[fromPosition]
        val todoTo = todos[toPosition]
        if (todoFrom != null && todoTo != null) {
            val tempTodoToPosition = todoFrom.position

            todoFrom.position = todoTo.position
            todoTo.position = tempTodoToPosition
            todoFrom.dirty = true
            todoTo.dirty = true

            Collections.swap(todos, fromPosition, toPosition)

            lifecycleScope.launch {
                persistSwappedItems(todoFrom, todoTo)
            }
        }
    }

    /**
     * Update the position value of the todo items in the database as well and fix the duplications
     * of the positions if there are.
     */
    private suspend fun persistSwappedItems(todoFrom: Todo?, todoTo: Todo?) {
        todoFrom?.let{ todoCloudDatabaseDao.updateTodo(it) }
        todoTo?.let{ todoCloudDatabaseDao.updateTodo(it) }
        todoCloudDatabaseDao.fixTodoPositions()
    }

    private fun getSwipedTodo(viewHolder: RecyclerView.ViewHolder): Todo {
        val swipedTodoAdapterPosition = viewHolder.adapterPosition
        return todoAdapter.getTodo(swipedTodoAdapterPosition)
    }

    fun areSelectedItems(): Boolean {
        return todoAdapter.selectedItemCount > 0
    }

    fun isActionMode() = actionMode != null

    fun openModifyTodoFragment(childViewAdapterPosition: Int) {
        todosViewModel?.initToModifyTodo(todoAdapter.getTodo(childViewAdapterPosition))
        (activity as MainActivity?)?.openModifyTodoFragment(this, null)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_todolist, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuitem_todolist_sort -> openSortTodoListDialogFragment()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openSortTodoListDialogFragment() {
        val sortTodoListDialogFragment = SortTodoListDialogFragment()
        sortTodoListDialogFragment.setTargetFragment(this, 0)
        sortTodoListDialogFragment.show(parentFragmentManager, "SortTodoListDialogFragment")
    }

    private fun setActionBarTitle() {
        (activity as MainActivity?)?.onSetActionBarTitle(todosViewModel?.todosTitle)
    }

    val callback: ActionMode.Callback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            setTodoListActionMode(mode)
            mode.menuInflater.inflate(R.menu.layout_appbar_todolist, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val title = prepareTitle()
            actionMode?.title = title
            todoAdapter.notifyDataSetChanged()
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.menuitem_layoutappbartodolist_delete -> openConfirmDeleteTodosDialog()
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            todoAdapter.clearSelection()
            setTodoListActionMode(null)
            todoAdapter.notifyDataSetChanged()
        }

        private fun prepareTitle(): String {
            val selectedItemCount = todoAdapter.selectedItemCount
            return selectedItemCount.toString() + " " + getString(R.string.all_selected)
        }
    }

    private fun setTodoListActionMode(actionMode: ActionMode?) {
        this.actionMode = actionMode
        AppController.setActionMode(actionMode)
    }

    private fun openConfirmDeleteTodosDialog() {
        val selectedTodos = todoAdapter.selectedTodos
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
        openConfirmDeleteDialogFragment(arguments)
    }

    private fun openConfirmDeleteDialogFragment(arguments: Bundle) {
        val confirmDeleteDialogFragment = ConfirmDeleteDialogFragment()
        confirmDeleteDialogFragment.setTargetFragment(this, 0)
        confirmDeleteDialogFragment.arguments = arguments
        confirmDeleteDialogFragment.show(parentFragmentManager, "ConfirmDeleteDialogFragment")
    }

    fun onFABClick() {
        actionMode?.finish()
        (this@TodoListFragment.activity as MainActivity?)?.
        onOpenCreateTodoFragment(this@TodoListFragment)
    }

    override fun onDismiss(dialog: DialogInterface?) {
        if (swipedTodoAdapterPosition != null)
            todoAdapter.notifyItemChanged(swipedTodoAdapterPosition!!)
        else
            todoAdapter.notifyDataSetChanged()
    }

    /**
     * Finish the action mode, if the fragment is in action mode.
     */
    fun finishActionMode() {
        actionMode?.finish()
    }
}
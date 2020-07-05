package com.rolandvitezhu.todocloud.ui.activity.main.fragment

import android.app.SearchManager
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController
import com.rolandvitezhu.todocloud.app.AppController.Companion.instance
import com.rolandvitezhu.todocloud.data.PredefinedList
import com.rolandvitezhu.todocloud.data.Todo
import com.rolandvitezhu.todocloud.datastorage.DbLoader
import com.rolandvitezhu.todocloud.datastorage.asynctask.UpdateViewModelTask
import com.rolandvitezhu.todocloud.listener.RecyclerViewOnItemTouchListener
import com.rolandvitezhu.todocloud.receiver.ReminderSetter
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity
import com.rolandvitezhu.todocloud.ui.activity.main.adapter.TodoAdapter
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.ConfirmDeleteDialogFragment
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.PredefinedListsViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.SearchListsViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.TodosViewModel
import kotlinx.android.synthetic.main.layout_recyclerview_search.view.*
import java.util.*
import javax.inject.Inject

class SearchFragment : Fragment(), DialogInterface.OnDismissListener {

    @Inject
    lateinit var dbLoader: DbLoader

    @Inject
    lateinit var todoAdapter: TodoAdapter

    private var searchView: SearchView? = null

    private var actionMode: ActionMode? = null

    private var todosViewModel: TodosViewModel? = null
    private var searchListsViewModel: SearchListsViewModel? = null
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
        searchListsViewModel = ViewModelProviders.of(this@SearchFragment.activity!!).get(SearchListsViewModel::class.java)
        predefinedListsViewModel = ViewModelProviders.of(this@SearchFragment.activity!!).get(PredefinedListsViewModel::class.java)
        todosViewModel!!.todos.observe(
                this,
                Observer { todos ->
                    todoAdapter!!.update(todos)
                    todoAdapter!!.notifyDataSetChanged()
                }
        )
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_search, container, false)
        prepareRecyclerView(view)
        applyClickEvents(view)
        applySwipeToDismissAndDragToReorder(view)
        return view
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)!!.onSetActionBarTitle("")
        prepareSearchViewAfterModifyTodo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    private fun prepareRecyclerView(view: View) {
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(
                context!!.applicationContext
        )
        view.recyclerview_search!!.layoutManager = layoutManager
        view.recyclerview_search!!.adapter = todoAdapter
    }

    private fun applyClickEvents(view: View) {
        view.recyclerview_search!!.addOnItemTouchListener(RecyclerViewOnItemTouchListener(
                context!!.applicationContext,
                view.recyclerview_search,
                object : RecyclerViewOnItemTouchListener.OnClickListener {
                    override fun onClick(childView: View, childViewAdapterPosition: Int) {
                        if (!isActionMode()) {
                            hideSoftInput()
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
                            (this@SearchFragment.activity as MainActivity?)!!.onStartActionMode(callback)
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
        itemTouchHelper.attachToRecyclerView(view.recyclerview_search)
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
        dbLoader!!.updateTodo(todoFrom)
        dbLoader!!.updateTodo(todoTo)
        dbLoader!!.fixTodoPositions(null)
        Collections.swap(todos, fromPosition, toPosition)
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

    private fun prepareSearchViewAfterModifyTodo() {
        if (searchView != null && view?.recyclerview_search != null) {
            searchView!!.post {
                restoreQueryTextState()
                view?.recyclerview_search!!.requestFocusFromTouch()
                searchView!!.clearFocus()
                hideSoftInput()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        restoreQueryTextState()
    }

    private fun restoreQueryTextState() {
        if (searchView != null) searchView!!.setQuery(searchListsViewModel!!.queryText, false)
    }

    private val callback: ActionMode.Callback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            setActionMode(mode)
            mode.menuInflater.inflate(R.menu.layout_appbar_search, menu)
            preventTypeIntoSearchView()
            return true
        }

        private fun preventTypeIntoSearchView() {
            if (searchView != null && view?.recyclerview_search != null) {
                view?.recyclerview_search!!.requestFocusFromTouch()
            }
            hideSoftInput()
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val title = prepareTitle()
            actionMode!!.title = title
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            val menuItemId = item.itemId
            when (menuItemId) {
                R.id.menuitem_layoutappbarsearch_delete -> openConfirmDeleteTodosDialog()
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            todoAdapter!!.clearSelection()
            setActionMode(null)
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_search, menu)
        prepareSearchView(menu)
    }

    private fun prepareSearchView(menu: Menu) {
        val searchMenuItem = menu.findItem(R.id.menuitem_search)
        searchView = searchMenuItem.actionView as SearchView
        val searchManager = activity
                ?.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchableInfo = searchManager.getSearchableInfo(
                activity!!.componentName
        )
        searchView!!.setSearchableInfo(searchableInfo)
        searchView!!.maxWidth = Int.MAX_VALUE
        searchView!!.isIconified = false
        searchView!!.isFocusable = true
        searchView!!.requestFocusFromTouch()
        disableSearchViewCloseButton()
        removeSearchViewUnderline()
        removeSearchViewHintIcon()
        applyOnQueryTextEvents()
    }

    private fun removeSearchViewUnderline() {
        val searchPlateId = searchView!!.context.resources.getIdentifier(
                "android:id/search_plate", null, null
        )
        val searchPlate = searchView!!.findViewById<View>(searchPlateId)
        searchPlate?.setBackgroundResource(0)
    }

    private fun removeSearchViewHintIcon() {
        if (searchView != null) {
            val searchMagIconId = searchView!!.context.resources.getIdentifier(
                    "android:id/search_mag_icon", null, null
            )
            val searchMagIcon = searchView!!.findViewById<View>(searchMagIconId)
            if (searchMagIcon != null) {
                searchView!!.isIconifiedByDefault = false
                searchMagIcon.layoutParams = LinearLayout.LayoutParams(0, 0)
            }
        }
    }

    private fun disableSearchViewCloseButton() {
        searchView!!.setOnCloseListener { true }
    }

    private fun applyOnQueryTextEvents() {
        searchView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                preventToExecuteQueryTextSubmitTwice()
                return true
            }

            private fun preventToExecuteQueryTextSubmitTwice() {
                if (searchView != null) {
                    searchView!!.clearFocus()
                    hideSoftInput()
                }
            }

            override fun onQueryTextChange(newText: String): Boolean {
                saveQueryTextState(newText)
                if (!newText.isEmpty()) {
                    showSearchResults(newText)
                } else {
                    clearSearchResults()
                }
                return true
            }

            private fun saveQueryTextState(queryText: String) {
                searchListsViewModel!!.queryText = queryText
            }

            private fun showSearchResults(newText: String) {
                setUpdateTodosViewModelObjects(newText)
                updateTodosViewModel()
            }

            private fun clearSearchResults() {
                todoAdapter!!.clear()
            }

            private fun setUpdateTodosViewModelObjects(queryText: String) {
                val where = dbLoader!!.prepareSearchWhere(queryText)
                todosViewModel!!.setIsPredefinedList(true)
                predefinedListsViewModel!!.predefinedList = PredefinedList("0", where)
            }
        })
    }

    private fun hideSoftInput() {
        val inputMethodManager = activity!!.getSystemService(
                Context.INPUT_METHOD_SERVICE
        ) as InputMethodManager
        val currentlyFocusedView = activity!!.currentFocus
        if (currentlyFocusedView != null) {
            val windowToken = currentlyFocusedView.windowToken
            inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    private fun isSetReminder(todo: Todo): Boolean {
        return !todo.reminderDateTime?.equals("-1")!!
    }

    private fun isNotCompleted(todo: Todo): Boolean {
        return !todo.completed!!
    }

    private fun shouldCreateReminderService(todoToModify: Todo): Boolean {
        return isNotCompleted(todoToModify) && isNotDeleted(todoToModify)
    }

    private fun isNotDeleted(todo: Todo): Boolean {
        return !todo.deleted!!
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
        val todosToSoftDelete: ArrayList<Todo> = itemsToDelete as ArrayList<Todo>
        for (todoToSoftDelete in todosToSoftDelete) {
            dbLoader!!.softDeleteTodo(todoToSoftDelete)
            ReminderSetter.cancelReminderService(todoToSoftDelete)
        }
        updateTodosViewModel()
        if (actionMode != null) {
            actionMode!!.finish()
        }
    }

    override fun onDismiss(dialog: DialogInterface?) {
        if (swipedTodoAdapterPosition != null)
            todoAdapter.notifyItemChanged(swipedTodoAdapterPosition!!)
        else
            todoAdapter.notifyDataSetChanged()
    }
}
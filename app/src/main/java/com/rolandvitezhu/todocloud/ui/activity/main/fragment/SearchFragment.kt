package com.rolandvitezhu.todocloud.ui.activity.main.fragment

import android.app.SearchManager
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.SearchView
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
import com.rolandvitezhu.todocloud.data.PredefinedList
import com.rolandvitezhu.todocloud.data.Todo
import com.rolandvitezhu.todocloud.database.TodoCloudDatabaseDao
import com.rolandvitezhu.todocloud.databinding.FragmentSearchBinding
import com.rolandvitezhu.todocloud.helper.hideSoftInput
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity
import com.rolandvitezhu.todocloud.ui.activity.main.adapter.TodoAdapter
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.ConfirmDeleteDialogFragment
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.PredefinedListsViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.SearchListsViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.TodosViewModel
import kotlinx.android.synthetic.main.layout_recyclerview_search.view.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

class SearchFragment : Fragment(), DialogInterface.OnDismissListener {

    @Inject
    lateinit var todoCloudDatabaseDao: TodoCloudDatabaseDao
    @Inject
    lateinit var todoAdapter: TodoAdapter

    private var searchView: SearchView? = null

    var actionMode: ActionMode? = null

    private val todosViewModel by lazy {
        activity?.let { ViewModelProvider(it).get(TodosViewModel::class.java) }
    }
    private val searchListsViewModel by lazy {
        activity?.let { ViewModelProvider(it).get(SearchListsViewModel::class.java) }
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
                    todoAdapter.update(todos)
                    todoAdapter.notifyDataSetChanged()
                }
        )
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val fragmentSearchBinding =
                FragmentSearchBinding.inflate(layoutInflater, null, false)
        val view: View = fragmentSearchBinding.root

        prepareRecyclerView()
        applySwipeToDismissAndDragToReorder(view)

        fragmentSearchBinding.lifecycleOwner = this
        fragmentSearchBinding.searchFragment = this
        fragmentSearchBinding.executePendingBindings()

        return view
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)?.onSetActionBarTitle("")
        prepareSearchViewAfterModifyTodo()
    }

    private fun prepareRecyclerView() {
        isDraggingEnabled = false
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
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val swipedTodo = getSwipedTodo(viewHolder)
                swipedTodoAdapterPosition = viewHolder.adapterPosition
                swipedTodoAdapterPosition?.let {
                    openConfirmDeleteTodosDialog(swipedTodo, it)
                }
            }

            override fun getMovementFlags(recyclerView: RecyclerView,
                                          viewHolder: RecyclerView.ViewHolder): Int {
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
        todoAdapter.itemTouchHelper = itemTouchHelper
    }

    private fun getSwipedTodo(viewHolder: RecyclerView.ViewHolder): Todo {
        val swipedTodoAdapterPosition = viewHolder.adapterPosition
        return todoAdapter.getTodo(swipedTodoAdapterPosition)
    }

    fun areSelectedItems(): Boolean {
        return todoAdapter.selectedItemCount > 0
    }

    fun isActionMode(): Boolean {
        return actionMode != null
    }

    fun openModifyTodoFragment(childViewAdapterPosition: Int) {
        todosViewModel?.todo = todoAdapter.getTodo(childViewAdapterPosition)
        (activity as MainActivity?)?.openModifyTodoFragment(this, null)
    }

    private fun prepareSearchViewAfterModifyTodo() {
        if (searchView != null && view?.recyclerview_search != null) {
            searchView?.post {
                restoreQueryTextState()
                view?.recyclerview_search?.requestFocusFromTouch()
                searchView?.clearFocus()
                hideSoftInput()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        restoreQueryTextState()
    }

    private fun restoreQueryTextState() {
        if (searchView != null)
            searchView?.setQuery(searchListsViewModel?.queryText, false)
    }

    val callback: ActionMode.Callback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            setSearchActionMode(mode)
            mode.menuInflater.inflate(R.menu.layout_appbar_search, menu)
            preventTypeIntoSearchView()
            return true
        }

        private fun preventTypeIntoSearchView() {
            if (searchView != null && view?.recyclerview_search != null) {
                view?.recyclerview_search?.requestFocusFromTouch()
            }
            hideSoftInput()
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val title = prepareTitle()
            actionMode?.title = title
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
            todoAdapter.clearSelection()
            setSearchActionMode(null)
        }

        private fun prepareTitle(): String {
            val selectedItemCount = todoAdapter.selectedItemCount
            return selectedItemCount.toString() + " " + getString(R.string.all_selected)
        }
    }

    private fun setSearchActionMode(actionMode: ActionMode?) {
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
        openConfirmDeleteDialogFragment(arguments, swipedTodoAdapterPosition)
    }

    private fun openConfirmDeleteDialogFragment(arguments: Bundle) {
        val confirmDeleteDialogFragment = ConfirmDeleteDialogFragment()
        confirmDeleteDialogFragment.setTargetFragment(this, 0)
        confirmDeleteDialogFragment.arguments = arguments
        confirmDeleteDialogFragment.show(parentFragmentManager, "ConfirmDeleteDialogFragment")
    }

    private fun openConfirmDeleteDialogFragment(
            arguments: Bundle, swipedTodoAdapterPosition: Int
    ) {
        val confirmDeleteDialogFragment = ConfirmDeleteDialogFragment()
        confirmDeleteDialogFragment.setTargetFragment(this, 0)
        confirmDeleteDialogFragment.arguments = arguments
        confirmDeleteDialogFragment.show(parentFragmentManager, "ConfirmDeleteDialogFragment")
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
                requireActivity().componentName
        )
        searchView?.setSearchableInfo(searchableInfo)
        searchView?.maxWidth = Int.MAX_VALUE
        searchView?.isIconified = false
        searchView?.isFocusable = true
        searchView?.requestFocusFromTouch()
        disableSearchViewCloseButton()
        removeSearchViewUnderline()
        removeSearchViewHintIcon()
        applyOnQueryTextEvents()
    }

    private fun removeSearchViewUnderline() {
        val searchPlateId = searchView?.context?.resources?.getIdentifier(
                "android:id/search_plate", null, null
        )
        if (searchPlateId != null) {
            val searchPlate = searchView?.findViewById<View>(searchPlateId)
            searchPlate?.setBackgroundResource(0)
        }
    }

    private fun removeSearchViewHintIcon() {
        if (searchView != null) {
            val searchMagIconId = searchView?.context?.resources?.getIdentifier(
                    "android:id/search_mag_icon", null, null
            )
            if (searchMagIconId != null) {
                val searchMagIcon = searchView!!.findViewById<View>(searchMagIconId)
                if (searchMagIcon != null) {
                    searchView?.isIconifiedByDefault = false
                    searchMagIcon.layoutParams = LinearLayout.LayoutParams(0, 0)
                }
            }
        }
    }

    private fun disableSearchViewCloseButton() {
        searchView?.setOnCloseListener { true }
    }

    private fun applyOnQueryTextEvents() {
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                preventToExecuteQueryTextSubmitTwice()
                return true
            }

            private fun preventToExecuteQueryTextSubmitTwice() {
                if (searchView != null) {
                    searchView?.clearFocus()
                    hideSoftInput()
                }
            }

            override fun onQueryTextChange(newText: String): Boolean {
                saveQueryTextState(newText)
                if (newText.isNotEmpty()) {
                    showSearchResults(newText)
                } else {
                    clearSearchResults()
                }
                return true
            }

            private fun saveQueryTextState(queryText: String) {
                searchListsViewModel?.queryText = queryText
            }

            private fun showSearchResults(newText: String) {
                lifecycleScope.launch {
                    val whereCondition =
                            todoCloudDatabaseDao.prepareSearchWhereCondition(newText)
                    predefinedListsViewModel?.predefinedList =
                            PredefinedList("0", whereCondition)
                    todosViewModel?.updateTodosViewModelByWhereCondition(
                            "",
                            whereCondition
                    )
                }
            }

            private fun clearSearchResults() {
                todoAdapter.clear()
            }
        })
    }

    private fun isSetReminder(todo: Todo): Boolean {
        return !todo.reminderDateTime?.equals("-1")!!
    }

    private fun isNotCompleted(todo: Todo): Boolean {
        return todo.completed?.not() ?: true
    }

    private fun shouldCreateReminderService(todoToModify: Todo): Boolean {
        return isNotCompleted(todoToModify) && isNotDeleted(todoToModify)
    }

    private fun isNotDeleted(todo: Todo): Boolean {
        return todo.deleted?.not() ?: true
    }

    /**
     * Finish action mode, if the Fragment is in action mode.
     */
    fun finishActionMode() {
        actionMode?.finish()
    }

    override fun onDismiss(dialog: DialogInterface?) {
        if (swipedTodoAdapterPosition != null)
            todoAdapter.notifyItemChanged(swipedTodoAdapterPosition!!)
        else
            todoAdapter.notifyDataSetChanged()
    }
}
package com.rolandvitezhu.todocloud.ui.activity.main.fragment

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.view.View.OnCreateContextMenuListener
import android.view.View.OnTouchListener
import android.view.ViewTreeObserver.OnScrollChangedListener
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.ExpandableListView
import android.widget.ExpandableListView.*
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.ListFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.material.snackbar.Snackbar
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController
import com.rolandvitezhu.todocloud.app.AppController.Companion.isActionModeEnabled
import com.rolandvitezhu.todocloud.app.AppController.Companion.showWhiteTextSnackbar
import com.rolandvitezhu.todocloud.data.Category
import com.rolandvitezhu.todocloud.data.PredefinedList
import com.rolandvitezhu.todocloud.databinding.FragmentMainlistBinding
import com.rolandvitezhu.todocloud.datastorage.DbLoader
import com.rolandvitezhu.todocloud.repository.CategoryRepository
import com.rolandvitezhu.todocloud.repository.ListRepository
import com.rolandvitezhu.todocloud.repository.TodoRepository
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity
import com.rolandvitezhu.todocloud.ui.activity.main.adapter.CategoryAdapter
import com.rolandvitezhu.todocloud.ui.activity.main.adapter.ListAdapter
import com.rolandvitezhu.todocloud.ui.activity.main.adapter.PredefinedListAdapter
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.*
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.CategoriesViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.ListsViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.PredefinedListsViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.UserViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.viewmodelfactory.ListsViewModelFactory
import kotlinx.android.synthetic.main.fragment_mainlist.*
import kotlinx.android.synthetic.main.fragment_mainlist.view.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

class MainListFragment() : ListFragment(), OnRefreshListener {

    private val TAG: String = javaClass.simpleName

    @Inject
    lateinit var dbLoader: DbLoader
    @Inject
    lateinit var todoRepository: TodoRepository
    @Inject
    lateinit var listDataSynchronizer: ListRepository
    @Inject
    lateinit var categoryRepository: CategoryRepository
    @Inject
    lateinit var predefinedListAdapter: PredefinedListAdapter
    @Inject
    lateinit var categoryAdapter: CategoryAdapter
    @Inject
    lateinit var listAdapter: ListAdapter

    private var actionMode: ActionMode? = null
    private var actionModeStartedWithELV: Boolean = false

    private val selectedCategories: ArrayList<Category> = ArrayList()
    private val selectedListsInCategory: ArrayList<com.rolandvitezhu.todocloud.data.List> = ArrayList()
    private val selectedLists: ArrayList<com.rolandvitezhu.todocloud.data.List> = ArrayList()

    private val categoriesViewModel by lazy {
        activity?.let { ViewModelProvider(it).get(CategoriesViewModel::class.java) }
    }
    private val listsViewModel by lazy {
        activity?.let {
            ViewModelProvider(it, ListsViewModelFactory(categoriesViewModel)).
            get(ListsViewModel::class.java)
        }
    }
    private val predefinedListsViewModel by lazy {
        activity?.let { ViewModelProvider(it).get(PredefinedListsViewModel::class.java) }
    }
    private val userViewModel by lazy {
        activity?.let { ViewModelProvider(it).get(UserViewModel::class.java) }
    }

    private val isNoSelectedItems: Boolean
        private get() {
            val checkedItemCount: Int = checkedItemCount
            return checkedItemCount == 0
        }
    private val checkedItemCount: Int
        private get() {
            return selectedCategories.size + selectedListsInCategory.size + selectedLists.size
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as AppController).appComponent.
        fragmentComponent().create().inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        categoriesViewModel?.lhmCategories?.observe(
                this,
                {
                    lhmCategories ->
                    categoryAdapter.update(lhmCategories)
                    categoryAdapter.notifyDataSetChanged()
                }
        )
        listsViewModel?.lists?.observe(
                this,
                {
                    lists ->
                    listAdapter.update(lists)
                    listAdapter.notifyDataSetChanged()
                }
        )
        predefinedListsViewModel?.predefinedLists?.observe(
                this,
                { predefinedLists ->
                    predefinedListAdapter.update(predefinedLists)
                    predefinedListAdapter.notifyDataSetChanged()
                }
        )
        userViewModel?.user?.observe(
                this,
                {
                    user ->
                    // We do not need to do anything here. We use the userViewModel to bind the data,
                    // so it will automatically update on the UI. Use it as an example when implementing
                    // the same for the other viewmodels.
                }
        )

        (this@MainListFragment.activity as MainActivity).onPrepareNavigationHeader()
        updateLists()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val fragmentMainListBinding: FragmentMainlistBinding =
                FragmentMainlistBinding.inflate(inflater, container, false)
        val view: View = fragmentMainListBinding.root

        prepareExpandableListView(view)
        prepareSwipeRefreshLayout(view)

        syncData(view)

        fragmentMainListBinding.mainListFragment = this
        fragmentMainListBinding.executePendingBindings()

        return view
    }

    private fun prepareSwipeRefreshLayout(view: View) {
        setScrollViewSwipeRefreshBehavior(view)
        view.swiperefreshlayout_mainlist?.setOnRefreshListener(this)
    }

    private fun setScrollViewSwipeRefreshBehavior(view: View) {
        view.scrollview_mainlist?.viewTreeObserver?.addOnScrollChangedListener(
                object : OnScrollChangedListener {

                    override fun onScrollChanged() {
                        try {
                            val scrollY: Int? = view.scrollview_mainlist?.scrollY
                            if (shouldSwipeRefresh(scrollY))
                                // We can only start the swipe refresh, when we are on the top of
                                // the list - which means scroll position y == 0 - and the action
                                // mode is disabled.
                                view.swiperefreshlayout_mainlist?.isEnabled = true
                        } catch (e: NullPointerException) {
                            // ScrollView nor SwipeRefreshLayout doesn't exists already.
                        }
                    }

                    private fun shouldSwipeRefresh(scrollY: Int?): Boolean {
                        return scrollY != null && scrollY == 0 && !isActionModeEnabled
                    }
                })
    }

    private fun prepareExpandableListView(view: View) {
        view.expandableheightexpandablelistview_mainlist_category?.setAdapter(categoryAdapter)
        view.expandableheightexpandablelistview_mainlist_category?.
        setOnChildClickListener(expLVChildClicked)
        view.expandableheightexpandablelistview_mainlist_category?.
        setOnGroupClickListener(expLVGroupClicked)
        applyExpLVLongClickEvents(view)
    }

    private fun applyExpLVLongClickEvents(view: View) {
        view.expandableheightexpandablelistview_mainlist_category?.
        setOnCreateContextMenuListener(expLVCategoryContextMenuListener)
    }

    private val callback: ActionMode.Callback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            isActionModeEnabled = true
            actionMode = mode
            fixExpandableListViewBehavior()
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val actionModeTitle: String = prepareActionModeTitle()
            actionMode?.title = actionModeTitle
            prepareMenu(mode, menu)
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            applyActionItemBehavior(item)
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            deselectItems()
            selectedCategories.clear()
            selectedListsInCategory.clear()
            selectedLists.clear()
            isActionModeEnabled = false
            view?.expandableheightexpandablelistview_mainlist_category?.choiceMode =
                    AbsListView.CHOICE_MODE_NONE
            view?.expandableheightlistview_mainlist_predefinedlist?.choiceMode =
                    AbsListView.CHOICE_MODE_NONE
        }

        /**
         * If ActionMode has started with ExpandableListView, it may stop it accidentally. It can cause
         * an unwanted click event after started the ActionMode. This method prevent that.
         */
        private fun fixExpandableListViewBehavior() {
            if (actionModeStartedWithELV) {
                preventUnwantedClickEvent()
                actionModeStartedWithELV = false
            }
        }

        private fun preventUnwantedClickEvent() {
            view?.expandableheightexpandablelistview_mainlist_category?.setOnTouchListener(
                    object : OnTouchListener {
                        override fun onTouch(v: View, event: MotionEvent): Boolean {
                            if (unwantedClickEventPassOut(event)) restoreDefaultBehavior()
                            return true
                        }
                    })
        }

        private fun unwantedClickEventPassOut(event: MotionEvent): Boolean {
            return event.action == MotionEvent.ACTION_UP ||
                    event.action == MotionEvent.ACTION_CANCEL
        }

        private fun restoreDefaultBehavior() {
            view?.expandableheightexpandablelistview_mainlist_category?.setOnTouchListener(null)
        }

        private fun prepareActionModeTitle(): String {
            return checkedItemCount.toString() + " " + getString(R.string.all_selected)
        }

        private fun prepareMenu(mode: ActionMode, menu: Menu) {
            if (oneCategorySelected()) {
                menu.clear()
                mode.menuInflater.inflate(R.menu.layout_appbar_mainlist_group, menu)
            } else if (oneListInCategorySelected()) {
                menu.clear()
                mode.menuInflater.inflate(R.menu.layout_appbar_mainlist_child, menu)
            } else if (oneListSelected()) {
                menu.clear()
                mode.menuInflater.inflate(R.menu.layout_appbar_mainlist_item, menu)
            } else if (manyCategoriesSelected()) {
                menu.clear()
                mode.menuInflater.inflate(R.menu.layout_appbar_mainlist_many_group, menu)
            } else if (manyListsInCategorySelected()) {
                menu.clear()
                mode.menuInflater.inflate(R.menu.layout_appbar_mainlist_many_child, menu)
            } else if (manyListsSelected()) {
                menu.clear()
                mode.menuInflater.inflate(R.menu.layout_appbar_mainlist_many_item, menu)
            } else if (manyCategoriesAndListsInCategorySelected()) {
                menu.clear()
            } else if (manyCategoriesAndListsSelected()) {
                menu.clear()
            } else if (manyListsInCategoryAndListsSelected()) {
                menu.clear()
            } else if (manyCategoriesAndListsInCategoryAndListsSelected()) {
                menu.clear()
            }
        }

        private fun applyActionItemBehavior(item: MenuItem) {
            if (oneCategorySelected()) {
                when (item.itemId) {
                    R.id.menuitem_layoutappbarmainlistgroup_createlist ->
                        openCreateListInCategoryDialogFragment()
                    R.id.menuitem_layoutappbarmainlistgroup_modify ->
                        openModifyCategoryDialogFragment()
                    R.id.menuitem_layoutappbarmainlistgroup_delete ->
                        openConfirmDeleteCategoryDialog()
                }
            } else if (oneListInCategorySelected()) {
                when (item.itemId) {
                    R.id.menuitem_layoutappbarmainlistchild_modify ->
                        openModifyListInCategoryDialog()
                    R.id.menuitem_layoutappbarmainlistchild_delete ->
                        openConfirmDeleteListInCategoryDialog()
                    R.id.menuitem_layoutappbarmainlistchild_move -> openMoveListInCategoryDialog()
                }
            } else if (oneListSelected()) {
                when (item.itemId) {
                    R.id.menuitem_layoutappbarmainlistitem_modify -> openModifyListDialog()
                    R.id.menuitem_layoutappbarmainlistitem_delete -> openConfirmDeleteListDialog()
                    R.id.menuitem_layoutappbarmainlistitem_move ->
                        openMoveListIntoAnotherCategoryDialog()
                }
            } else if (manyCategoriesSelected()) {
                if (item.itemId == R.id.menuitem_layoutappbarmainlistmanygroup_delete)
                    openConfirmDeleteCategoriesDialog()
            } else if (manyListsInCategorySelected()) {
                if (item.itemId == R.id.menuitem_layoutappbarmainlistmanychild_delete)
                    openConfirmDeleteListsInCategoryDialog()
            } else if (manyListsSelected()) {
                if (item.itemId == R.id.menuitem_layoutappbarmainlistmanyitem_delete)
                    openConfirmDeleteListsDialog()
            } else if (manyCategoriesAndListsInCategorySelected()) {
            } else if (manyCategoriesAndListsSelected()) {
            } else if (manyListsInCategoryAndListsSelected()) {
            } else if (manyCategoriesAndListsInCategoryAndListsSelected()) {
            }
        }

        private fun oneCategorySelected(): Boolean {
            return (selectedCategories.size == 1) && (selectedListsInCategory.size == 0) && (selectedLists.size == 0)
        }

        private fun oneListInCategorySelected(): Boolean {
            return (selectedCategories.size == 0) && (selectedListsInCategory.size == 1) && (selectedLists.size == 0)
        }

        private fun oneListSelected(): Boolean {
            return (selectedCategories.size == 0) && (selectedListsInCategory.size == 0) && (selectedLists.size == 1)
        }

        private fun manyCategoriesSelected(): Boolean {
            return (selectedCategories.size > 1) && (selectedListsInCategory.size == 0) && (selectedLists.size == 0)
        }

        private fun manyListsInCategorySelected(): Boolean {
            return (selectedCategories.size == 0) && (selectedListsInCategory.size > 1) && (selectedLists.size == 0)
        }

        private fun manyListsSelected(): Boolean {
            return (selectedCategories.size == 0) && (selectedListsInCategory.size == 0) && (selectedLists.size > 1)
        }

        private fun manyCategoriesAndListsInCategorySelected(): Boolean {
            return (selectedCategories.size > 0) && (selectedListsInCategory.size > 0) && (selectedLists.size == 0)
        }

        private fun manyCategoriesAndListsSelected(): Boolean {
            return (selectedCategories.size > 0) && (selectedListsInCategory.size == 0) && (selectedLists.size > 0)
        }

        private fun manyListsInCategoryAndListsSelected(): Boolean {
            return (selectedCategories.size == 0) && (selectedListsInCategory.size > 0) && (selectedLists.size > 0)
        }

        private fun manyCategoriesAndListsInCategoryAndListsSelected(): Boolean {
            return (selectedCategories.size > 0) && (selectedListsInCategory.size > 0) && (selectedLists.size > 0)
        }

        private fun deselectItems() {
            deselectListItems()
            deselectExpandableListViewVisibleItems()
        }

        private fun deselectListItems() {
            for (i in 0 until listAdapter.count) {
                view?.expandableheightlistview_mainlist_list?.setItemChecked(i, false)
            }
        }

        /**
         * Should deselect the visible items. Visible items are the group items, and the child
         * items of expanded group items.
         */
        private fun deselectExpandableListViewVisibleItems() {
            if (view?.expandableheightexpandablelistview_mainlist_category != null) {
                for (i in 0..view?.expandableheightexpandablelistview_mainlist_category!!.lastVisiblePosition) {
                    view!!.expandableheightexpandablelistview_mainlist_category!!.setItemChecked(i, false)
                    categoriesViewModel?.deselectItems()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_mainlist, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuitem_mainlist_search ->
                (this@MainListFragment.activity as MainActivity).onSearchActionItemClick()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openCreateListDialogFragment() {
        val createListDialogFragment = CreateListDialogFragment()
        createListDialogFragment.setTargetFragment(this, 0)
        createListDialogFragment.show(parentFragmentManager, "CreateListDialogFragment")
    }

    private fun openCreateCategoryDialogFragment() {
        val createCategoryDialogFragment = CreateCategoryDialogFragment()
        createCategoryDialogFragment.setTargetFragment(this, 0)
        createCategoryDialogFragment.show(parentFragmentManager, "CreateCategoryDialogFragment")
    }

    private fun openCreateListInCategoryDialogFragment() {
        categoriesViewModel?.category = selectedCategories.get(0)
        val createListInCategoryDialogFragment = CreateListInCategoryDialogFragment()
        createListInCategoryDialogFragment.setTargetFragment(this, 0)
        createListInCategoryDialogFragment.show(parentFragmentManager, "CreateListInCategoryDialogFragment")
    }

    private fun openModifyListInCategoryDialog() {
        listsViewModel?.list = selectedListsInCategory.get(0)
        listsViewModel?.isInCategory = true
        openModifyListDialogFragment()
    }

    private fun openConfirmDeleteListInCategoryDialog() {
        val list: com.rolandvitezhu.todocloud.data.List = selectedListsInCategory.get(0)
        val onlineId: String? = list.listOnlineId
        val title: String? = list.title
        val arguments = Bundle()

        arguments.putString("itemType", "listInCategory")
        arguments.putString("itemTitle", title)
        arguments.putString("onlineId", onlineId)

        listsViewModel?.isInCategory = true

        openConfirmDeleteDialogFragment(arguments)
    }

    private fun openConfirmDeleteListsInCategoryDialog() {
        val arguments = Bundle()

        arguments.putString("itemType", "listInCategory")
        arguments.putParcelableArrayList("itemsToDelete", selectedListsInCategory)

        listsViewModel?.isInCategory = true

        openConfirmDeleteDialogFragment(arguments)
    }

    private fun openMoveListInCategoryDialog() {
        val list: com.rolandvitezhu.todocloud.data.List = selectedListsInCategory.get(0)
        categoriesViewModel?.category = dbLoader.getCategoryByCategoryOnlineId(list.categoryOnlineId)
        listsViewModel?.list = list
        openMoveListDialogFragment()
    }

    private fun openMoveListIntoAnotherCategoryDialog() {
        categoriesViewModel?.category = Category(getString(R.string.movelist_spinneritemlistnotincategory))
        listsViewModel?.list = selectedLists.get(0)
        openMoveListDialogFragment()
    }

    private fun openMoveListDialogFragment() {
        val moveListDialogFragment = MoveListDialogFragment()
        moveListDialogFragment.setTargetFragment(this, 0)
        moveListDialogFragment.show(parentFragmentManager, "MoveListDialogFragment")
    }

    private fun openConfirmDeleteCategoryDialog() {
        val category: Category = selectedCategories.get(0)
        val onlineId: String? = category.categoryOnlineId
        val title: String? = category.title
        val arguments = Bundle()

        arguments.putString("itemType", "category")
        arguments.putString("itemTitle", title)
        arguments.putString("onlineId", onlineId)

        openConfirmDeleteDialogFragment(arguments)
    }

    private fun openConfirmDeleteCategoriesDialog() {
        val arguments = Bundle()
        arguments.putString("itemType", "category")
        arguments.putParcelableArrayList("itemsToDelete", selectedCategories)
        openConfirmDeleteDialogFragment(arguments)
    }

    private fun openConfirmDeleteDialogFragment(arguments: Bundle) {
        val confirmDeleteDialogFragment = ConfirmDeleteDialogFragment()
        confirmDeleteDialogFragment.setTargetFragment(this, 0)
        confirmDeleteDialogFragment.arguments = arguments
        confirmDeleteDialogFragment.show(parentFragmentManager, "ConfirmDeleteDialogFragment")
    }

    private fun openConfirmDeleteListDialog() {
        val list: com.rolandvitezhu.todocloud.data.List = selectedLists.get(0)
        val onlineId: String? = list.listOnlineId
        val title: String? = list.title
        val arguments = Bundle()

        arguments.putString("itemType", "list")
        arguments.putString("itemTitle", title)
        arguments.putString("onlineId", onlineId)

        listsViewModel?.isInCategory = false

        openConfirmDeleteDialogFragment(arguments)
    }

    private fun openConfirmDeleteListsDialog() {
        val arguments = Bundle()

        arguments.putString("itemType", "list")
        arguments.putParcelableArrayList("itemsToDelete", selectedLists)

        listsViewModel?.isInCategory = false

        openConfirmDeleteDialogFragment(arguments)
    }

    private fun openModifyCategoryDialogFragment() {
        categoriesViewModel?.category = selectedCategories.get(0)
        val modifyCategoryDialogFragment = ModifyCategoryDialogFragment()
        modifyCategoryDialogFragment.setTargetFragment(this, 0)
        modifyCategoryDialogFragment.show(parentFragmentManager, "ModifyCategoryDialogFragment")
    }

    private fun openModifyListDialog() {
        listsViewModel?.list = selectedLists.get(0)
        openModifyListDialogFragment()
    }

    private fun openModifyListDialogFragment() {
        val modifyListDialogFragment = ModifyListDialogFragment()
        modifyListDialogFragment.setTargetFragment(this, 0)
        modifyListDialogFragment.show(parentFragmentManager, "ModifyListDialogFragment")
    }

    /**
     * Show the circle icon as the synchronization starts and hide it as the synchronization
     * finishes. Synchronizes all the entities - categories, lists, todos - between the local and
     * remote database. Refreshes the UI to show the up-to-date data. Shows the error messages.
     */
    private fun syncData(view: View?) {
        lifecycleScope.launch {
            try {
                if (view != null)
                    view.swiperefreshlayout_mainlist?.isRefreshing = true
                else
                    this@MainListFragment.swiperefreshlayout_mainlist?.isRefreshing = true
                categoriesViewModel?.onSynchronization()
                listsViewModel?.onSynchronization()
                categoriesViewModel?.updateCategoriesViewModel()
                todoRepository.syncTodoData()
            } catch (cause: Throwable) {
                showErrorMessage(cause.message)
                categoriesViewModel?.updateCategoriesViewModel()
                listsViewModel?.updateListsViewModel()
            } finally {
                if (view != null)
                    view.swiperefreshlayout_mainlist?.isRefreshing = false
                else
                    this@MainListFragment.swiperefreshlayout_mainlist?.isRefreshing = false
            }
        }
    }

    /**
     * Update all of the lists: predefined lists, lists, categories.
     */
    private fun updateLists() {
        lifecycleScope.launch {
            predefinedListsViewModel?.updatePredefinedListsViewModel()
            categoriesViewModel?.updateCategoriesViewModel()
            listsViewModel?.updateListsViewModel()
        }
    }

    val predefinedListItemClicked: OnItemClickListener = object : OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            if (!isActionModeEnabled) {
                val predefinedList: PredefinedList = parent.adapter.getItem(
                        position) as PredefinedList
                (this@MainListFragment.activity as MainActivity).onClickPredefinedList(predefinedList)
            }
        }
    }
    val listItemClicked: OnItemClickListener = object : OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
            val clickedList: com.rolandvitezhu.todocloud.data.List =
                    listAdapter.getItem(position) as com.rolandvitezhu.todocloud.data.List
            if (!isActionModeEnabled) {
                (this@MainListFragment.activity as MainActivity).onClickList(clickedList)
            } else {
                handleItemSelection(position, clickedList)
            }
        }

        private fun handleItemSelection(position: Int, clickedList: com.rolandvitezhu.todocloud.data.List) {
            // Item checked state being set automatically on item click event. We should track
            // the changes only.
            if (view?.expandableheightlistview_mainlist_list?.isItemChecked(position) == true) {
                selectedLists.add(clickedList)
            } else {
                selectedLists.remove(clickedList)
            }
            if (isNoSelectedItems) {
                actionMode?.finish()
            } else {
                actionMode?.invalidate()
            }
        }
    }
    val listItemLongClicked: OnItemLongClickListener = object : OnItemLongClickListener {
        override fun onItemLongClick(parent: AdapterView<*>?, view: View, position: Int, id: Long):
                Boolean {
            if (!isActionModeEnabled) {
                startActionMode(position)
            }

            return true
        }

        private fun startActionMode(position: Int) {
            (this@MainListFragment.activity as MainActivity).onStartActionMode(callback)
            view?.expandableheightlistview_mainlist_list?.choiceMode =
                    AbsListView.CHOICE_MODE_MULTIPLE
            view?.expandableheightlistview_mainlist_list?.setItemChecked(position, true)
            val selectedList: com.rolandvitezhu.todocloud.data.List =
                    view?.expandableheightlistview_mainlist_list?.getItemAtPosition(position) as
                            com.rolandvitezhu.todocloud.data.List
            selectedLists.add(selectedList)
            view?.expandableheightlistview_mainlist_list?.choiceMode =
                    AbsListView.CHOICE_MODE_MULTIPLE
            actionMode?.invalidate()
        }
    }
    private val expLVChildClicked: OnChildClickListener = object : OnChildClickListener {

        override fun onChildClick(parent: ExpandableListView, v: View, groupPosition: Int,
                                  childPosition: Int, id: Long): Boolean {
            val clickedList: com.rolandvitezhu.todocloud.data.List = categoryAdapter.getChild(
                    groupPosition,
                    childPosition
            ) as com.rolandvitezhu.todocloud.data.List
            if (!isActionModeEnabled) {
                (this@MainListFragment.activity as MainActivity).onClickList(clickedList)
            } else {
                handleItemSelection(clickedList)
            }

            return true
        }

        private fun handleItemSelection(clickedList: com.rolandvitezhu.todocloud.data.List) {
            categoriesViewModel?.toggleListSelected(clickedList)
            if (clickedList.isSelected) {
                selectedListsInCategory.add(clickedList)
            } else {
                selectedListsInCategory.remove(clickedList)
            }
            if (isNoSelectedItems) {
                actionMode?.finish()
            } else {
                actionMode?.invalidate()
            }
        }
    }
    private val expLVGroupClicked: OnGroupClickListener = object : OnGroupClickListener {

        override fun onGroupClick(parent: ExpandableListView, v: View, groupPosition: Int, id: Long): Boolean {
            val packedPosition: Long = getPackedPositionForGroup(groupPosition)
            val position: Int = parent.getFlatListPosition(packedPosition)
            if (!isActionModeEnabled) {
                handleGroupExpanding(parent, groupPosition)
            } else {
                handleItemSelection(parent, position)
            }

            return true
        }

        private fun handleItemSelection(
                parent: ExpandableListView,
                position: Int
        ) {
            val clickedCategory: Category = parent.getItemAtPosition(position) as Category
            categoriesViewModel?.toggleCategorySelected(clickedCategory)
            if (clickedCategory.isSelected) {
                selectedCategories.add(clickedCategory)
            } else {
                selectedCategories.remove(clickedCategory)
            }
            if (isNoSelectedItems) {
                actionMode?.finish()
            } else {
                actionMode?.invalidate()
            }
        }

        private fun handleGroupExpanding(parent: ExpandableListView, groupPosition: Int) {
            if (!parent.isGroupExpanded(groupPosition)) {
                parent.expandGroup(groupPosition)
            } else {
                parent.collapseGroup(groupPosition)
            }
        }
    }

    private val expLVCategoryContextMenuListener: OnCreateContextMenuListener =
            object : OnCreateContextMenuListener {
        override fun onCreateContextMenu(menu: ContextMenu, v: View,
                                         menuInfo: ContextMenuInfo) {
            if (!isActionModeEnabled &&
                    view?.expandableheightexpandablelistview_mainlist_category != null) {
                val info: ExpandableListContextMenuInfo = menuInfo as ExpandableListContextMenuInfo
                val packedPosition: Long = info.packedPosition
                val position: Int = view?.expandableheightexpandablelistview_mainlist_category!!.
                getFlatListPosition(packedPosition)
                val packedPositionType: Int = getPackedPositionType(packedPosition)
                if (categoryClicked(packedPositionType)) {
                    startActionModeWithCategory(position)
                } else if (listClicked(packedPositionType)) {
                    startActionModeWithList(position)
                }
            }
        }

        private fun startActionModeWithList(position: Int) {
            if (view?.expandableheightexpandablelistview_mainlist_category != null) {
                actionModeStartedWithELV = true
                (this@MainListFragment.activity as MainActivity).onStartActionMode(callback)
                val clickedList: com.rolandvitezhu.todocloud.data.List =
                        view?.expandableheightexpandablelistview_mainlist_category?.
                        getItemAtPosition(position) as com.rolandvitezhu.todocloud.data.List
                categoriesViewModel?.toggleListSelected(clickedList)
                selectedListsInCategory.add(clickedList)
                view?.expandableheightlistview_mainlist_list?.choiceMode =
                        AbsListView.CHOICE_MODE_MULTIPLE
                actionMode?.invalidate()
            }
        }

        private fun startActionModeWithCategory(position: Int) {
            if (view?.expandableheightexpandablelistview_mainlist_category != null) {
                actionModeStartedWithELV = true
                (this@MainListFragment.activity as MainActivity).onStartActionMode(callback)
                val clickedCategory: Category =
                        view?.expandableheightexpandablelistview_mainlist_category!!.
                        getItemAtPosition(position) as Category
                categoriesViewModel?.toggleCategorySelected(clickedCategory)
                selectedCategories.add(clickedCategory)
                view?.expandableheightlistview_mainlist_list?.choiceMode =
                        AbsListView.CHOICE_MODE_MULTIPLE
                actionMode?.invalidate()
            }
        }

        private fun listClicked(packedPositionType: Int): Boolean {
            return packedPositionType == PACKED_POSITION_TYPE_CHILD
        }

        private fun categoryClicked(packedPositionType: Int): Boolean {
            return packedPositionType == PACKED_POSITION_TYPE_GROUP
        }
    }

    /**
     * Finish the action mode if the screen is in it.
     */
    fun finishActionMode() {
        actionMode?.finish()
    }

    override fun onRefresh() {
        syncData(null)
    }

    private fun showErrorMessage(errorMessage: String?) {
        if (errorMessage != null) {
            val upperCaseErrorMessage = errorMessage.toUpperCase()
            if (upperCaseErrorMessage.contains("FAILED TO CONNECT") ||
                    upperCaseErrorMessage.contains("UNABLE TO RESOLVE HOST") ||
                    upperCaseErrorMessage.contains("TIMEOUT")) {
                showFailedToConnectError()
            } else {
                showAnErrorOccurredError()
            }
        }
    }

    private fun showFailedToConnectError() {
        // Android Studio hotswap/coldswap may cause getView == null
        try {
            val snackbar: Snackbar = Snackbar.make(
                    view?.constraintlayout_mainlist!!,
                    R.string.all_failedtoconnect,
                    Snackbar.LENGTH_LONG
            )
            showWhiteTextSnackbar(snackbar)
        } catch (e: NullPointerException) {
            // Snackbar or constraintLayout doesn't exists already.
        }
    }

    private fun showAnErrorOccurredError() {
        try {
            val snackbar: Snackbar = Snackbar.make(
                    view?.constraintlayout_mainlist!!,
                    R.string.all_anerroroccurred,
                    Snackbar.LENGTH_LONG
            )
            showWhiteTextSnackbar(snackbar)
        } catch (e: NullPointerException) {
            // Snackbar or constraintLayout doesn't exists already.
        }
    }

    fun onFABCreateCategoryClick() {
        openCreateCategoryDialogFragment()
        this.main_fam.close(true)
    }

    fun onFABCreateListClick() {
        openCreateListDialogFragment()
        this.main_fam.close(true)
    }
}

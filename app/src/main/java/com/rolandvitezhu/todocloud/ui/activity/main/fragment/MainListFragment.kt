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
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.ListFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.material.snackbar.Snackbar
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController
import com.rolandvitezhu.todocloud.app.AppController.Companion.isActionModeEnabled
import com.rolandvitezhu.todocloud.app.AppController.Companion.showWhiteTextSnackbar
import com.rolandvitezhu.todocloud.data.Category
import com.rolandvitezhu.todocloud.data.PredefinedList
import com.rolandvitezhu.todocloud.data.User
import com.rolandvitezhu.todocloud.databinding.FragmentMainlistBinding
import com.rolandvitezhu.todocloud.datastorage.DbConstants
import com.rolandvitezhu.todocloud.datastorage.DbLoader
import com.rolandvitezhu.todocloud.datastorage.asynctask.UpdateViewModelTask
import com.rolandvitezhu.todocloud.datasynchronizer.DataSynchronizer
import com.rolandvitezhu.todocloud.datasynchronizer.DataSynchronizer.OnSyncDataListener
import com.rolandvitezhu.todocloud.helper.OnlineIdGenerator
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity
import com.rolandvitezhu.todocloud.ui.activity.main.adapter.CategoryAdapter
import com.rolandvitezhu.todocloud.ui.activity.main.adapter.ListAdapter
import com.rolandvitezhu.todocloud.ui.activity.main.adapter.PredefinedListAdapter
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.*
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.CategoriesViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.ListsViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.PredefinedListsViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.UserViewModel
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_mainlist.*
import kotlinx.android.synthetic.main.fragment_mainlist.view.*
import java.util.*
import javax.inject.Inject

class MainListFragment() : ListFragment(), OnRefreshListener, OnSyncDataListener {
    private val TAG: String = javaClass.simpleName
    private val disposable: CompositeDisposable = CompositeDisposable()

    @Inject
    lateinit var dbLoader: DbLoader

    @Inject
    lateinit var dataSynchronizer: DataSynchronizer

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
    private var categoriesViewModel: CategoriesViewModel? = null
    private var listsViewModel: ListsViewModel? = null
    private var predefinedListsViewModel: PredefinedListsViewModel? = null
    private var userViewModel: UserViewModel? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (Objects.requireNonNull(activity)?.application as AppController).appComponent.
        fragmentComponent().create().inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        categoriesViewModel = ViewModelProviders.of((activity)!!).get(CategoriesViewModel::class.java)
        listsViewModel = ViewModelProviders.of((activity)!!).get(ListsViewModel::class.java)
        predefinedListsViewModel = ViewModelProviders.of((activity)!!).get(PredefinedListsViewModel::class.java)
        userViewModel = ViewModelProviders.of((activity)!!).get(UserViewModel::class.java)
        categoriesViewModel!!.categories.observe(
                this,
                object : Observer<LinkedHashMap<Category, List<com.rolandvitezhu.todocloud.data.List>>> {
                    override fun onChanged(
                            lhmCategories: LinkedHashMap<Category, List<com.rolandvitezhu.todocloud.data.List>>) {
                        categoryAdapter!!.update(lhmCategories)
                        categoryAdapter!!.notifyDataSetChanged()
                    }
                }
        )
        listsViewModel!!.lists.observe(
                this, object : Observer<List<com.rolandvitezhu.todocloud.data.List>> {
            override fun onChanged(lists: List<com.rolandvitezhu.todocloud.data.List>) {
                listAdapter!!.update(lists)
                listAdapter!!.notifyDataSetChanged()
            }
        })
        predefinedListsViewModel!!.predefinedLists.observe(
                this, object : Observer<List<PredefinedList>> {
            override fun onChanged(predefinedLists: List<PredefinedList>) {
                predefinedListAdapter!!.update(predefinedLists)
                predefinedListAdapter!!.notifyDataSetChanged()
            }
        }
        )
        userViewModel!!.user.observe(
                this, object : Observer<User?> {
            override fun onChanged(user: User?) {
                // We do not need to do anything here. We use the userViewModel to bind the data,
                // so it will automatically update on the UI.
            }
        }
        )
        (this@MainListFragment.getActivity() as MainActivity).onPrepareNavigationHeader()
        updatePredefinedListsViewModel()
        updateCategoriesViewModel()
        updateListsViewModel()
        dataSynchronizer!!.setOnSyncDataListener(this)
        dataSynchronizer!!.syncData(disposable)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val fragmentMainListBinding: FragmentMainlistBinding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_mainlist, container, false)
        val view: View = fragmentMainListBinding.root
        fragmentMainListBinding.mainListFragment = this;

        preparePredefinedList(view)
        prepareExpandableListView(view)
        prepareList(view)
        prepareSwipeRefreshLayout(view)

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposable.clear()
    }

    private fun prepareSwipeRefreshLayout(view: View) {
        setScrollViewSwipeRefreshBehavior(view)
        view.swiperefreshlayout_mainlist!!.setOnRefreshListener(this)
    }

    private fun setScrollViewSwipeRefreshBehavior(view: View) {
        view.scrollview_mainlist!!.viewTreeObserver.addOnScrollChangedListener(
                object : OnScrollChangedListener {
                    override fun onScrollChanged() {
                        try {
                            val scrollY: Int = view.scrollview_mainlist!!.scrollY
                            if (shouldSwipeRefresh(scrollY)) view.swiperefreshlayout_mainlist!!.isEnabled = true else view.swiperefreshlayout_mainlist!!.isEnabled = false
                        } catch (e: NullPointerException) {
                            // ScrollView nor SwipeRefreshLayout doesn't exists already.
                        }
                    }

                    private fun shouldSwipeRefresh(scrollY: Int): Boolean {
                        return scrollY == 0 && !isActionModeEnabled
                    }
                })
    }

    private fun prepareList(view: View) {
        view.expandableheightlistview_mainlist_list!!.isExpanded = true
        view.expandableheightlistview_mainlist_list!!.adapter = listAdapter
        view.expandableheightlistview_mainlist_list!!.onItemClickListener = listItemClicked
        view.expandableheightlistview_mainlist_list!!.onItemLongClickListener = listItemLongClicked
    }

    private fun prepareExpandableListView(view: View) {
        view.expandableheightexpandablelistview_mainlist_category!!.isExpanded = true
        view.expandableheightexpandablelistview_mainlist_category!!.setAdapter(categoryAdapter)
        view.expandableheightexpandablelistview_mainlist_category!!.setOnChildClickListener(expLVChildClicked)
        view.expandableheightexpandablelistview_mainlist_category!!.setOnGroupClickListener(expLVGroupClicked)
        applyExpLVLongClickEvents(view)
    }

    private fun applyExpLVLongClickEvents(view: View) {
        view.expandableheightexpandablelistview_mainlist_category!!.setOnCreateContextMenuListener(expLVCategoryContextMenuListener)
    }

    private fun preparePredefinedList(view: View) {
        view.expandableheightlistview_mainlist_predefinedlist!!.isExpanded = true
        view.expandableheightlistview_mainlist_predefinedlist!!.adapter = predefinedListAdapter
        view.expandableheightlistview_mainlist_predefinedlist!!.onItemClickListener = predefinedListItemClicked
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
            actionMode!!.title = actionModeTitle
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
            view?.expandableheightexpandablelistview_mainlist_category!!.choiceMode = AbsListView.CHOICE_MODE_NONE
            view?.expandableheightlistview_mainlist_predefinedlist!!.choiceMode = AbsListView.CHOICE_MODE_NONE
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
            view?.expandableheightexpandablelistview_mainlist_category!!.setOnTouchListener(object : OnTouchListener {
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
            view?.expandableheightexpandablelistview_mainlist_category!!.setOnTouchListener(null)
        }

        private fun prepareActionModeTitle(): String {
            val checkedItemCount: Int = (view?.expandableheightlistview_mainlist_list!!.checkedItemCount
                    + view!!.expandableheightexpandablelistview_mainlist_category!!.checkedItemCount)
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
            val menuItemId: Int = item.itemId
            if (oneCategorySelected()) {
                when (menuItemId) {
                    R.id.menuitem_layoutappbarmainlistgroup_createlist -> openCreateListInCategoryDialogFragment()
                    R.id.menuitem_layoutappbarmainlistgroup_modify -> openModifyCategoryDialogFragment()
                    R.id.menuitem_layoutappbarmainlistgroup_delete -> openConfirmDeleteCategoryDialog()
                }
            } else if (oneListInCategorySelected()) {
                when (menuItemId) {
                    R.id.menuitem_layoutappbarmainlistchild_modify -> openModifyListInCategoryDialog()
                    R.id.menuitem_layoutappbarmainlistchild_delete -> openConfirmDeleteListInCategoryDialog()
                    R.id.menuitem_layoutappbarmainlistchild_move -> openMoveListInCategoryDialog()
                }
            } else if (oneListSelected()) {
                when (menuItemId) {
                    R.id.menuitem_layoutappbarmainlistitem_modify -> openModifyListDialog()
                    R.id.menuitem_layoutappbarmainlistitem_delete -> openConfirmDeleteListDialog()
                    R.id.menuitem_layoutappbarmainlistitem_move -> openMoveListIntoAnotherCategoryDialog()
                }
            } else if (manyCategoriesSelected()) {
                if (menuItemId == R.id.menuitem_layoutappbarmainlistmanygroup_delete) openConfirmDeleteCategoriesDialog()
            } else if (manyListsInCategorySelected()) {
                if (menuItemId == R.id.menuitem_layoutappbarmainlistmanychild_delete) openConfirmDeleteListsInCategoryDialog()
            } else if (manyListsSelected()) {
                if (menuItemId == R.id.menuitem_layoutappbarmainlistmanyitem_delete) openConfirmDeleteListsDialog()
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
            for (i in 0 until listAdapter!!.count) {
                view?.expandableheightlistview_mainlist_list!!.setItemChecked(i, false)
            }
        }

        /**
         * Should deselect the visible items. Visible items are the group items, and the child
         * items of expanded group items.
         */
        private fun deselectExpandableListViewVisibleItems() {
            for (i in 0..view?.expandableheightexpandablelistview_mainlist_category!!.lastVisiblePosition) {
                view!!.expandableheightexpandablelistview_mainlist_category!!.setItemChecked(i, false)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_mainlist, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val menuItemId: Int = item.itemId
        when (menuItemId) {
            R.id.menuitem_mainlist_search -> (this@MainListFragment.getActivity() as MainActivity).onSearchActionItemClick()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openCreateListDialogFragment() {
        val createListDialogFragment: CreateListDialogFragment = CreateListDialogFragment()
        createListDialogFragment.setTargetFragment(this, 0)
        createListDialogFragment.show((fragmentManager)!!, "CreateListDialogFragment")
    }

    private fun openCreateCategoryDialogFragment() {
        val createCategoryDialogFragment: CreateCategoryDialogFragment = CreateCategoryDialogFragment()
        createCategoryDialogFragment.setTargetFragment(this, 0)
        createCategoryDialogFragment.show((fragmentManager)!!, "CreateCategoryDialogFragment")
    }

    private fun updatePredefinedListsViewModel() {
        val updateViewModelTask: UpdateViewModelTask = UpdateViewModelTask(predefinedListsViewModel, activity)
        updateViewModelTask.execute()
    }

    private fun updateCategoriesViewModel() {
        val updateViewModelTask: UpdateViewModelTask = UpdateViewModelTask(categoriesViewModel, activity)
        updateViewModelTask.execute()
    }

    private fun updateListsViewModel() {
        val updateViewModelTask: UpdateViewModelTask = UpdateViewModelTask(listsViewModel, activity)
        updateViewModelTask.execute()
    }

    private fun openCreateListInCategoryDialogFragment() {
        categoriesViewModel!!.category = selectedCategories.get(0)
        val createListInCategoryDialogFragment: CreateListInCategoryDialogFragment = CreateListInCategoryDialogFragment()
        createListInCategoryDialogFragment.setTargetFragment(this, 0)
        createListInCategoryDialogFragment.show((fragmentManager)!!, "CreateListInCategoryDialogFragment")
    }

    private fun openModifyListInCategoryDialog() {
        listsViewModel!!.list = selectedListsInCategory.get(0)
        listsViewModel!!.setIsInCategory(true)
        openModifyListDialogFragment()
    }

    private fun openConfirmDeleteListInCategoryDialog() {
        val list: com.rolandvitezhu.todocloud.data.List = selectedListsInCategory.get(0)
        val onlineId: String? = list.listOnlineId
        val title: String? = list.title
        val arguments: Bundle = Bundle()

        arguments.putString("itemType", "listInCategory")
        arguments.putString("itemTitle", title)
        arguments.putString("onlineId", onlineId)

        openConfirmDeleteDialogFragment(arguments)
    }

    private fun openConfirmDeleteListsInCategoryDialog() {
        val arguments: Bundle = Bundle()
        arguments.putString("itemType", "listInCategory")
        arguments.putParcelableArrayList("itemsToDelete", selectedListsInCategory)
        openConfirmDeleteDialogFragment(arguments)
    }

    private fun openMoveListInCategoryDialog() {
        val list: com.rolandvitezhu.todocloud.data.List = selectedListsInCategory.get(0)
        val category: Category = dbLoader!!.getCategoryByCategoryOnlineId(list.categoryOnlineId)
        categoriesViewModel!!.category = category
        listsViewModel!!.list = list
        openMoveListDialogFragment()
    }

    private fun openMoveListIntoAnotherCategoryDialog() {
        val category: Category = Category(getString(R.string.movelist_spinneritemlistnotincategory))
        val list: com.rolandvitezhu.todocloud.data.List = selectedLists.get(0)
        categoriesViewModel!!.category = category
        listsViewModel!!.list = list
        openMoveListDialogFragment()
    }

    private fun openMoveListDialogFragment() {
        val moveListDialogFragment: MoveListDialogFragment = MoveListDialogFragment()
        moveListDialogFragment.setTargetFragment(this, 0)
        moveListDialogFragment.show((fragmentManager)!!, "MoveListDialogFragment")
    }

    private fun openConfirmDeleteCategoryDialog() {
        val category: Category = selectedCategories.get(0)
        val onlineId: String? = category.categoryOnlineId
        val title: String? = category.title
        val arguments: Bundle = Bundle()

        arguments.putString("itemType", "category")
        arguments.putString("itemTitle", title)
        arguments.putString("onlineId", onlineId)

        openConfirmDeleteDialogFragment(arguments)
    }

    private fun openConfirmDeleteCategoriesDialog() {
        val arguments: Bundle = Bundle()
        arguments.putString("itemType", "category")
        arguments.putParcelableArrayList("itemsToDelete", selectedCategories)
        openConfirmDeleteDialogFragment(arguments)
    }

    private fun openConfirmDeleteDialogFragment(arguments: Bundle) {
        val confirmDeleteDialogFragment: ConfirmDeleteDialogFragment = ConfirmDeleteDialogFragment()
        confirmDeleteDialogFragment.setTargetFragment(this, 0)
        confirmDeleteDialogFragment.arguments = arguments
        confirmDeleteDialogFragment.show((fragmentManager)!!, "ConfirmDeleteDialogFragment")
    }

    private fun openConfirmDeleteListDialog() {
        val list: com.rolandvitezhu.todocloud.data.List = selectedLists.get(0)
        val onlineId: String? = list.listOnlineId
        val title: String? = list.title
        val arguments: Bundle = Bundle()

        arguments.putString("itemType", "list")
        arguments.putString("itemTitle", title)
        arguments.putString("onlineId", onlineId)

        openConfirmDeleteDialogFragment(arguments)
    }

    private fun openConfirmDeleteListsDialog() {
        val arguments: Bundle = Bundle()
        arguments.putString("itemType", "list")
        arguments.putParcelableArrayList("itemsToDelete", selectedLists)
        openConfirmDeleteDialogFragment(arguments)
    }

    private fun openModifyCategoryDialogFragment() {
        val category: Category = selectedCategories.get(0)
        categoriesViewModel!!.category = category
        val modifyCategoryDialogFragment: ModifyCategoryDialogFragment = ModifyCategoryDialogFragment()
        modifyCategoryDialogFragment.setTargetFragment(this, 0)
        modifyCategoryDialogFragment.show((fragmentManager)!!, "ModifyCategoryDialogFragment")
    }

    private fun openModifyListDialog() {
        listsViewModel!!.list = selectedLists.get(0)
        openModifyListDialogFragment()
    }

    private fun openModifyListDialogFragment() {
        val modifyListDialogFragment: ModifyListDialogFragment = ModifyListDialogFragment()
        modifyListDialogFragment.setTargetFragment(this, 0)
        modifyListDialogFragment.show((fragmentManager)!!, "ModifyListDialogFragment")
    }

    private fun syncData() {
        dataSynchronizer!!.syncData(disposable)
    }

    private val predefinedListItemClicked: OnItemClickListener = object : OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            if (!isActionModeEnabled) {
                val predefinedList: PredefinedList = parent.adapter.getItem(
                        position) as PredefinedList
                (this@MainListFragment.getActivity() as MainActivity).onClickPredefinedList(predefinedList)
            }
        }
    }
    private val listItemClicked: OnItemClickListener = object : OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
            val clickedList: com.rolandvitezhu.todocloud.data.List = listAdapter!!.getItem(position) as com.rolandvitezhu.todocloud.data.List
            if (!isActionModeEnabled) {
                (this@MainListFragment.getActivity() as MainActivity).onClickList(clickedList)
            } else {
                handleItemSelection(position, clickedList)
            }
        }

        private fun handleItemSelection(position: Int, clickedList: com.rolandvitezhu.todocloud.data.List) {
            // Item checked state being set automatically on item click event. We should track
            // the changes only.
            if (view?.expandableheightlistview_mainlist_list!!.isItemChecked(position)) {
                selectedLists.add(clickedList)
            } else {
                selectedLists.remove(clickedList)
            }
            if (isNoSelectedItems) {
                actionMode!!.finish()
            } else {
                actionMode!!.invalidate()
            }
        }

        private val isNoSelectedItems: Boolean
            private get() = checkedItemCount == 0

        private val checkedItemCount: Int
            private get() = view?.expandableheightlistview_mainlist_list!!.checkedItemCount +
                    view!!.expandableheightexpandablelistview_mainlist_category!!.checkedItemCount
    }
    private val listItemLongClicked: OnItemLongClickListener = object : OnItemLongClickListener {
        override fun onItemLongClick(parent: AdapterView<*>?, view: View, position: Int, id: Long): Boolean {
            if (!isActionModeEnabled) {
                startActionMode(position)
            }

            return true
        }

        private fun startActionMode(position: Int) {
            (this@MainListFragment.getActivity() as MainActivity).onStartActionMode(callback)
            view?.expandableheightlistview_mainlist_list!!.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE
            view?.expandableheightlistview_mainlist_list!!.setItemChecked(position, true)
            val selectedList: com.rolandvitezhu.todocloud.data.List = view?.expandableheightlistview_mainlist_list!!.getItemAtPosition(position) as com.rolandvitezhu.todocloud.data.List
            selectedLists.add(selectedList)
            view?.expandableheightlistview_mainlist_list!!.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE
            actionMode!!.invalidate()
        }
    }
    private val expLVChildClicked: OnChildClickListener = object : OnChildClickListener {
        override fun onChildClick(parent: ExpandableListView, v: View, groupPosition: Int,
                                  childPosition: Int, id: Long): Boolean {
            val clickedList: com.rolandvitezhu.todocloud.data.List = categoryAdapter!!.getChild(groupPosition, childPosition) as com.rolandvitezhu.todocloud.data.List
            val packedPosition: Long = getPackedPositionForChild(
                    groupPosition,
                    childPosition
            )
            val position: Int = parent.getFlatListPosition(packedPosition)
            if (!isActionModeEnabled) {
                (this@MainListFragment.getActivity() as MainActivity).onClickList(clickedList)
            } else {
                handleItemSelection(parent, clickedList, position)
            }

            return true
        }

        private fun handleItemSelection(parent: ExpandableListView, clickedList: com.rolandvitezhu.todocloud.data.List, position: Int) {
            toggleItemCheckedState(parent, position)
            if (parent.isItemChecked(position)) {
                selectedListsInCategory.add(clickedList)
            } else {
                selectedListsInCategory.remove(clickedList)
            }
            if (isNoSelectedItems) {
                actionMode!!.finish()
            } else {
                actionMode!!.invalidate()
            }
        }

        private fun toggleItemCheckedState(parent: ExpandableListView, position: Int) {
            view?.expandableheightlistview_mainlist_list!!.setItemChecked(position, !parent.isItemChecked(position))
        }

        private val isNoSelectedItems: Boolean
            private get() {
                val checkedItemCount: Int = checkedItemCount
                return checkedItemCount == 0
            }

        private val checkedItemCount: Int
            private get() {
                return view?.expandableheightlistview_mainlist_list!!.checkedItemCount +
                        view!!.expandableheightexpandablelistview_mainlist_category!!.checkedItemCount
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

        private fun handleItemSelection(parent: ExpandableListView, position: Int) {
            toggleItemCheckedState(parent, position)
            val clickedCategory: Category = parent.getItemAtPosition(position) as Category
            if (parent.isItemChecked(position)) {
                selectedCategories.add(clickedCategory)
            } else {
                selectedCategories.remove(clickedCategory)
            }
            if (isNoSelectedItems) {
                actionMode!!.finish()
            } else {
                actionMode!!.invalidate()
            }
        }

        private fun toggleItemCheckedState(parent: ExpandableListView, position: Int) {
            view?.expandableheightexpandablelistview_mainlist_category!!.setItemChecked(position, !parent.isItemChecked(position))
        }

        private fun handleGroupExpanding(parent: ExpandableListView, groupPosition: Int) {
            if (!parent.isGroupExpanded(groupPosition)) {
                parent.expandGroup(groupPosition)
            } else {
                parent.collapseGroup(groupPosition)
            }
        }

        private val isNoSelectedItems: Boolean
            private get() {
                return checkedItemCount == 0
            }

        private val checkedItemCount: Int
            private get() {
                return view?.expandableheightlistview_mainlist_list!!.checkedItemCount +
                        view!!.expandableheightexpandablelistview_mainlist_category!!.checkedItemCount
            }
    }
    private val expLVCategoryContextMenuListener: OnCreateContextMenuListener = object : OnCreateContextMenuListener {
        override fun onCreateContextMenu(menu: ContextMenu, v: View,
                                         menuInfo: ContextMenuInfo) {
            if (!isActionModeEnabled) {
                val info: ExpandableListContextMenuInfo = menuInfo as ExpandableListContextMenuInfo
                val packedPosition: Long = info.packedPosition
                val position: Int = view?.expandableheightexpandablelistview_mainlist_category!!.getFlatListPosition(packedPosition)
                val packedPositionType: Int = getPackedPositionType(packedPosition)
                if (categoryClicked(packedPositionType)) {
                    startActionModeWithCategory(position)
                } else if (listClicked(packedPositionType)) {
                    startActionModeWithList(position)
                }
            }
        }

        private fun startActionModeWithList(position: Int) {
            actionModeStartedWithELV = true
            (this@MainListFragment.getActivity() as MainActivity).onStartActionMode(callback)
            view?.expandableheightexpandablelistview_mainlist_category!!.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE
            view?.expandableheightexpandablelistview_mainlist_category!!.setItemChecked(position, true)
            val clickedList: com.rolandvitezhu.todocloud.data.List = view?.expandableheightexpandablelistview_mainlist_category!!.getItemAtPosition(position) as com.rolandvitezhu.todocloud.data.List
            selectedListsInCategory.add(clickedList)
            view?.expandableheightlistview_mainlist_list!!.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE
            actionMode!!.invalidate()
        }

        private fun startActionModeWithCategory(position: Int) {
            actionModeStartedWithELV = true
            (this@MainListFragment.getActivity() as MainActivity).onStartActionMode(callback)
            view?.expandableheightexpandablelistview_mainlist_category!!.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE
            view?.expandableheightexpandablelistview_mainlist_category!!.setItemChecked(position, true)
            val clickedCategory: Category = view?.expandableheightexpandablelistview_mainlist_category!!.getItemAtPosition(position) as Category
            selectedCategories.add(clickedCategory)
            view?.expandableheightlistview_mainlist_list!!.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE
            actionMode!!.invalidate()
        }

        private fun listClicked(packedPositionType: Int): Boolean {
            return packedPositionType == PACKED_POSITION_TYPE_CHILD
        }

        private fun categoryClicked(packedPositionType: Int): Boolean {
            return packedPositionType == PACKED_POSITION_TYPE_GROUP
        }
    }

    fun onCreateCategory() {
        createCategoryInLocalDatabase(categoriesViewModel!!.category)
        updateCategoriesViewModel()
    }

    private fun createCategoryInLocalDatabase(category: Category) {
        category.userOnlineId = dbLoader!!.userOnlineId
        category._id = dbLoader!!.createCategory(category)
        val categoryOnlineId: String = OnlineIdGenerator.generateOnlineId(
                DbConstants.Category.DATABASE_TABLE,
                category._id!!,
                dbLoader!!.apiKey
        )
        category.categoryOnlineId = categoryOnlineId
        dbLoader!!.updateCategory(category)
    }

    fun onModifyCategory() {
        val category: Category = categoriesViewModel!!.category
        category.dirty = true
        dbLoader!!.updateCategory(category)
        updateCategoriesViewModel()
        actionMode!!.finish()
    }

    fun onCreateList() {
        createListInLocalDatabase(listsViewModel!!.list)
        updateListsViewModel()
    }

    private fun createListInLocalDatabase(list: com.rolandvitezhu.todocloud.data.List) {
        list.userOnlineId = dbLoader!!.userOnlineId
        list._id = dbLoader!!.createList(list)
        val listOnlineId: String = OnlineIdGenerator.generateOnlineId(
                DbConstants.List.DATABASE_TABLE,
                list._id!!,
                dbLoader!!.apiKey
        )
        list.listOnlineId = listOnlineId
        dbLoader!!.updateList(list)
    }

    fun onModifyList() {
        val list: com.rolandvitezhu.todocloud.data.List = listsViewModel!!.list
        list.dirty = true
        dbLoader!!.updateList(list)
        if (listsViewModel!!.isInCategory) updateCategoriesViewModel() else updateListsViewModel()
        actionMode!!.finish()
    }

    fun onCreateListInCategory() {
        createListInCategoryInLocalDatabase(
                listsViewModel!!.list,
                categoriesViewModel!!.category.categoryOnlineId
        )
        updateCategoriesViewModel()
        actionMode!!.finish()
    }

    private fun createListInCategoryInLocalDatabase(
            list: com.rolandvitezhu.todocloud.data.List,
            categoryOnlineId: String?
    ) {
        list.userOnlineId = dbLoader!!.userOnlineId
        list.categoryOnlineId = categoryOnlineId
        list._id = dbLoader!!.createList(list)
        val listOnlineId: String = OnlineIdGenerator.generateOnlineId(
                DbConstants.List.DATABASE_TABLE,
                list._id!!,
                dbLoader!!.apiKey
        )
        list.listOnlineId = listOnlineId
        dbLoader!!.updateList(list)
    }

    fun onMoveList(isListNotInCategoryBeforeMove: Boolean) {
        val categoryOnlineId: String? = categoriesViewModel!!.category.categoryOnlineId
        val list: com.rolandvitezhu.todocloud.data.List = listsViewModel!!.list
        when (if (isListNotInCategoryBeforeMove) "isListNotInCategoryBeforeMove" else "isListInCategoryBeforeMove") {
            "isListNotInCategoryBeforeMove" -> if (moveListOutsideCategory(categoryOnlineId)) {
                actionMode!!.finish()
            } else {
                moveListIntoCategory(list, categoryOnlineId)
                actionMode!!.finish()
            }
            "isListInCategoryBeforeMove" -> if (moveListOutsideCategory(categoryOnlineId)) {
                moveListOutsideCategory(list)
                actionMode!!.finish()
            } else {
                moveListIntoAnotherCategory(list, categoryOnlineId)
                actionMode!!.finish()
            }
        }
    }

    private fun moveListIntoCategory(
            list: com.rolandvitezhu.todocloud.data.List,
            categoryOnlineId: String?
    ) {
        list.categoryOnlineId = categoryOnlineId
        list.dirty = true
        dbLoader!!.updateList(list)
        updateListsViewModel()
        updateCategoriesViewModel()
    }

    private fun moveListIntoAnotherCategory(
            list: com.rolandvitezhu.todocloud.data.List,
            categoryOnlineId: String?
    ) {
        list.categoryOnlineId = categoryOnlineId
        list.dirty = true
        dbLoader!!.updateList(list)
        updateCategoriesViewModel()
    }

    private fun moveListOutsideCategory(list: com.rolandvitezhu.todocloud.data.List) {
        list.categoryOnlineId = null
        list.dirty = true
        dbLoader!!.updateList(list)
        updateCategoriesViewModel()
        updateListsViewModel()
    }

    private fun moveListOutsideCategory(categoryOnlineId: String?): Boolean {
        return categoryOnlineId == null
    }

    override fun onRefresh() {
        syncData()
    }

    fun onSoftDelete(onlineId: String?, itemType: String?) {
        when (itemType) {
            "list" -> {
                dbLoader!!.softDeleteListAndRelatedTodos(onlineId)
                updateListsViewModel()
                actionMode!!.finish()
            }
            "listInCategory" -> {
                dbLoader!!.softDeleteListAndRelatedTodos(onlineId)
                updateCategoriesViewModel()
                actionMode!!.finish()
            }
            "category" -> {
                dbLoader!!.softDeleteCategoryAndListsAndTodos(onlineId)
                updateCategoriesViewModel()
                actionMode!!.finish()
            }
        }
    }

    fun onSoftDelete(itemsToDelete: ArrayList<*>, itemType: String?) {
        when (itemType) {
            "list" -> {
                val lists: ArrayList<com.rolandvitezhu.todocloud.data.List> =
                        itemsToDelete as ArrayList<com.rolandvitezhu.todocloud.data.List>
                for (list: com.rolandvitezhu.todocloud.data.List in lists) {
                    dbLoader!!.softDeleteListAndRelatedTodos(list.listOnlineId)
                }
                updateListsViewModel()
                actionMode!!.finish()
            }
            "listInCategory" -> {
                val listsInCategory: ArrayList<com.rolandvitezhu.todocloud.data.List> =
                        itemsToDelete as ArrayList<com.rolandvitezhu.todocloud.data.List>
                for (list: com.rolandvitezhu.todocloud.data.List in listsInCategory) {
                    dbLoader!!.softDeleteListAndRelatedTodos(list.listOnlineId)
                }
                updateCategoriesViewModel()
                actionMode!!.finish()
            }
            "category" -> {
                val categories: ArrayList<Category> = itemsToDelete as ArrayList<Category>
                for (category: Category in categories) {
                    dbLoader!!.softDeleteCategoryAndListsAndTodos(category.categoryOnlineId)
                }
                updateCategoriesViewModel()
                actionMode!!.finish()
            }
        }
    }

    override fun onFinishSyncListData() {
        updateListsViewModel()
        updateCategoriesViewModel()
    }

    override fun onFinishSyncCategoryData() {
        updateCategoriesViewModel()
    }

    override fun onFinishSyncData() {
        try {
            view?.swiperefreshlayout_mainlist!!.isRefreshing = false
        } catch (e: NullPointerException) {
            // SwipeRefreshLayout doesn't already exists.
        }
    }

    override fun onSyncError(errorMessage: String) {
        showErrorMessage(errorMessage)
    }

    private fun showErrorMessage(errorMessage: String) {
        if (errorMessage.contains("failed to connect")) {
            showFailedToConnectError()
        } else {
            showAnErrorOccurredError()
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

    fun onFABCreateCategoryClick(view: View) {
        openCreateCategoryDialogFragment()
        this.main_fam.close(true)
    }

    fun onFABCreateListClick(view: View) {
        openCreateListDialogFragment()
        this.main_fam.close(true)
    }
}

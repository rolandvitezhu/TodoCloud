package com.example.todocloud.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.view.ActionMode;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ScrollView;

import com.example.todocloud.R;
import com.example.todocloud.adapter.CategoryAdapter;
import com.example.todocloud.adapter.ListAdapter;
import com.example.todocloud.adapter.PredefinedListAdapter;
import com.example.todocloud.app.AppController;
import com.example.todocloud.customcomponent.ExpandableHeightExpandableListView;
import com.example.todocloud.customcomponent.ExpandableHeightListView;
import com.example.todocloud.data.Category;
import com.example.todocloud.data.PredefinedListItem;
import com.example.todocloud.datastorage.DbConstants;
import com.example.todocloud.datastorage.DbLoader;
import com.example.todocloud.datastorage.asynctask.UpdateAdapterTask;
import com.example.todocloud.datasynchronizer.CategoryDataSynchronizer;
import com.example.todocloud.datasynchronizer.ListDataSynchronizer;
import com.example.todocloud.datasynchronizer.TodoDataSynchronizer;
import com.example.todocloud.helper.OnlineIdGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainListFragment extends ListFragment implements
    CategoryCreateFragment.ICategoryCreateFragment, CategoryModifyFragment.ICategoryModifyFragment,
    ListCreateFragment.IListCreateFragment, ListModifyFragment.IListModifyFragment,
    ListInCategoryCreateFragment.IListInCategoryCreateFragment,
    ListMoveFragment.IListMoveFragment, SwipeRefreshLayout.OnRefreshListener,
    ConfirmDeleteDialogFragment.IConfirmDeleteDialogFragment, LogoutFragment.ILogoutFragment,
    TodoDataSynchronizer.OnSyncTodoDataListener, ListDataSynchronizer.OnSyncListDataListener,
    CategoryDataSynchronizer.OnSyncCategoryDataListener {

  private static final String TAG = MainListFragment.class.getSimpleName();

  private DbLoader dbLoader;

  private PredefinedListAdapter predefinedListAdapter;
  private CategoryAdapter categoryAdapter;
  private ListAdapter listAdapter;

  private TodoDataSynchronizer todoDataSynchronizer;
  private ListDataSynchronizer listDataSynchronizer;
  private CategoryDataSynchronizer categoryDataSynchronizer;

  private SwipeRefreshLayout swipeRefreshLayout;
  private CoordinatorLayout coordinatorLayout;
  private ScrollView scrollView;

  private IMainListFragment listener;

  private ExpandableHeightExpandableListView expandableListView;
  private ExpandableHeightListView list;

  private ActionMode actionMode;
  private boolean actionModeStartedWithELV;

  private ArrayList<Category> selectedCategories = new ArrayList<>();
  private ArrayList<com.example.todocloud.data.List> selectedListsInCategory = new ArrayList<>();
  private ArrayList<com.example.todocloud.data.List> selectedLists = new ArrayList<>();

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (IMainListFragment) context;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    dbLoader = new DbLoader(getActivity());
    listener.onSetNavigationHeader();
    updatePredefinedListAdapter();
    updateCategoryAdapter();
    updateListAdapter();

    todoDataSynchronizer = new TodoDataSynchronizer(dbLoader);
    todoDataSynchronizer.setOnSyncTodoDataListener(this);
    listDataSynchronizer = new ListDataSynchronizer(dbLoader);
    listDataSynchronizer.setOnSyncListDataListener(this);
    categoryDataSynchronizer = new CategoryDataSynchronizer(dbLoader);
    categoryDataSynchronizer.setOnSyncCategoryDataListener(this);

    todoDataSynchronizer.getTodos();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View combinedListView = inflater.inflate(R.layout.main_list, null);
    coordinatorLayout = (CoordinatorLayout) combinedListView.findViewById(R.id.coordinatorLayout);

    preparePredefinedList(combinedListView);
    prepareExpandableListView(combinedListView);
    prepareList(combinedListView);
    prepareSwipeRefreshLayout(combinedListView);

    return combinedListView;
  }

  private void prepareSwipeRefreshLayout(View combinedListView) {
    swipeRefreshLayout = (SwipeRefreshLayout)
        combinedListView.findViewById(R.id.swipe_refresh_layout);
    setScrollViewSwipeRefreshBehavior(combinedListView);
    swipeRefreshLayout.setOnRefreshListener(this);
  }

  private void setScrollViewSwipeRefreshBehavior(View combinedListView) {
    scrollView = (ScrollView) combinedListView.findViewById(R.id.scroll_view);
    scrollView.getViewTreeObserver().addOnScrollChangedListener(
        new ViewTreeObserver.OnScrollChangedListener() {

          @Override
          public void onScrollChanged() {
            int scrollY = scrollView.getScrollY();
            if (shouldSwipeRefresh(scrollY))
              swipeRefreshLayout.setEnabled(true);
            else {
              swipeRefreshLayout.setEnabled(false);
            }
          }

          private boolean shouldSwipeRefresh(int scrollY) {
            return scrollY == 0 && !AppController.isActionModeEnabled();
          }

        });
  }

  private void prepareList(View combinedListView) {
    list = (ExpandableHeightListView) combinedListView.findViewById(R.id.lvList);
    list.setExpanded(true);
    list.setAdapter(listAdapter);
    list.setOnItemClickListener(listItemClicked);
    list.setOnItemLongClickListener(listItemLongClicked);
  }

  private void prepareExpandableListView(View combinedListView) {
    expandableListView = (ExpandableHeightExpandableListView)
        combinedListView.findViewById(R.id.expLVCategory);
    expandableListView.setExpanded(true);
    expandableListView.setAdapter(categoryAdapter);
    expandableListView.setOnChildClickListener(expLVChildClicked);
    expandableListView.setOnGroupClickListener(expLVGroupClicked);
    applyExpLVLongClickEvents();
  }

  private void applyExpLVLongClickEvents() {
    expandableListView.setOnCreateContextMenuListener(expLVCategoryContextMenuListener);
  }

  private void preparePredefinedList(View combinedListView) {
    final ExpandableHeightListView predefinedList =
        (ExpandableHeightListView) combinedListView.findViewById(R.id.lvPredefinedList);
    predefinedList.setExpanded(true);
    predefinedList.setAdapter(predefinedListAdapter);
    predefinedList.setOnItemClickListener(predefinedListItemClicked);
  }

  private ActionMode.Callback callback =
      new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
          AppController.setActionModeEnabled(true);
          actionMode = mode;
          fixExpandableListViewBehavior();

          return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
          String actionModeTitle = prepareActionModeTitle();
          actionMode.setTitle(actionModeTitle);
          prepareMenu(mode, menu);

          return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
          applyActionItemBehavior(item);

          return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
          deselectItems();

          selectedCategories.clear();
          selectedListsInCategory.clear();
          selectedLists.clear();

          AppController.setActionModeEnabled(false);
          expandableListView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
          list.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
        }

        /**
         * If ActionMode has started with ExpandableListView, it may stop it accidentally. It can cause
         * an unwanted click event after started the ActionMode. This method prevent that.
         */
        private void fixExpandableListViewBehavior() {
          if (actionModeStartedWithELV) {
            preventUnwantedClickEvent();
            actionModeStartedWithELV = false;
          }
        }

        private void preventUnwantedClickEvent() {
          expandableListView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
              if (unwantedClickEventPassOut(event))
                restoreDefaultBehavior();
              return true;
            }

          });
        }

        private boolean unwantedClickEventPassOut(MotionEvent event) {
          return event.getAction() == MotionEvent.ACTION_UP ||
              event.getAction() == MotionEvent.ACTION_CANCEL;
        }

        private void restoreDefaultBehavior() {
          expandableListView.setOnTouchListener(null);
        }

        @NonNull
        private String prepareActionModeTitle() {
          int checkedItemCount = list.getCheckedItemCount()
              + expandableListView.getCheckedItemCount();
          return checkedItemCount + " " + getString(R.string.selected);
        }

        private void prepareMenu(ActionMode mode, Menu menu) {
          if (oneCategorySelected()) {
            menu.clear();
            mode.getMenuInflater().inflate(R.menu.group, menu);
          } else if (oneListInCategorySelected()) {
            menu.clear();
            mode.getMenuInflater().inflate(R.menu.child, menu);
          } else if (oneListSelected()) {
            menu.clear();
            mode.getMenuInflater().inflate(R.menu.item, menu);
          } else if (manyCategoriesSelected()) {
            menu.clear();
            mode.getMenuInflater().inflate(R.menu.many_group, menu);
          } else if (manyListsInCategorySelected()) {
            menu.clear();
            mode.getMenuInflater().inflate(R.menu.many_child, menu);
          } else if (manyListsSelected()) {
            menu.clear();
            mode.getMenuInflater().inflate(R.menu.many_item, menu);
          } else if (manyCategoriesAndListsInCategorySelected()) {
            menu.clear();
          } else if (manyCategoriesAndListsSelected()) {
            menu.clear();
          } else if (manyListsInCategoryAndListsSelected()) {
            menu.clear();
          } else if (manyCategoriesAndListsInCategoryAndListsSelected()) {
            menu.clear();
          }
        }

        private void applyActionItemBehavior(MenuItem item) {
          int itemId = item.getItemId();
          if (oneCategorySelected()) {
            switch (itemId) {
              case R.id.itemNewList:
                openListInCategoryCreateFragment();
                break;
              case R.id.itemModify:
                modifyCategory();
                break;
              case R.id.itemDelete:
                deleteCategory();
                break;
            }
          } else if (oneListInCategorySelected()) {
            switch (itemId) {
              case R.id.itemModify:
                modifyListInCategory();
                break;
              case R.id.itemDelete:
                deleteListInCategory();
                break;
              case R.id.itemMove:
                moveListInCategory();
                break;
            }
          } else if (oneListSelected()) {
            switch (itemId) {
              case R.id.itemModify:
                modifyList();
                break;
              case R.id.itemDelete:
                deleteList();
                break;
              case R.id.itemMove:
                moveListIntoAnotherCategory();
                break;
            }
          } else if (manyCategoriesSelected()) {
            if (itemId == R.id.itemDelete)
              deleteCategories();
          } else if (manyListsInCategorySelected()) {
            if (itemId == R.id.itemDelete)
              deleteListsInCategory();
          } else if (manyListsSelected()) {
            if (itemId == R.id.itemDelete)
              deleteLists();
          } else if (manyCategoriesAndListsInCategorySelected()) {

          } else if (manyCategoriesAndListsSelected()) {

          } else if (manyListsInCategoryAndListsSelected()) {

          } else if (manyCategoriesAndListsInCategoryAndListsSelected()) {

          }
        }

        private boolean oneCategorySelected() {
          return selectedCategories.size() == 1 && selectedListsInCategory.size() == 0 && selectedLists.size() == 0;
        }

        private boolean oneListInCategorySelected() {
          return selectedCategories.size() == 0 && selectedListsInCategory.size() == 1 && selectedLists.size() == 0;
        }

        private boolean oneListSelected() {
          return selectedCategories.size() == 0 && selectedListsInCategory.size() == 0 && selectedLists.size() == 1;
        }

        private boolean manyCategoriesSelected() {
          return selectedCategories.size() > 1 && selectedListsInCategory.size() == 0 && selectedLists.size() == 0;
        }

        private boolean manyListsInCategorySelected() {
          return selectedCategories.size() == 0 && selectedListsInCategory.size() > 1 && selectedLists.size() == 0;
        }

        private boolean manyListsSelected() {
          return selectedCategories.size() == 0 && selectedListsInCategory.size() == 0 && selectedLists.size() > 1;
        }

        private boolean manyCategoriesAndListsInCategorySelected() {
          return selectedCategories.size() > 0 && selectedListsInCategory.size() > 0 && selectedLists.size() == 0;
        }

        private boolean manyCategoriesAndListsSelected() {
          return selectedCategories.size() > 0 && selectedListsInCategory.size() == 0 && selectedLists.size() > 0;
        }

        private boolean manyListsInCategoryAndListsSelected() {
          return selectedCategories.size() == 0 && selectedListsInCategory.size() > 0 && selectedLists.size() > 0;
        }

        private boolean manyCategoriesAndListsInCategoryAndListsSelected() {
          return selectedCategories.size() > 0 && selectedListsInCategory.size() > 0 && selectedLists.size() > 0;
        }

        private void deselectItems() {
          deselectListItems();
          deselectExpandableListViewVisibleItems();
        }

        private void deselectListItems() {
          for (int i = 0; i < listAdapter.getCount(); i++) {
            list.setItemChecked(i, false);
          }
        }

        /**
         * Should deselect the visible items. Visible items are the group items, and the child
         * items of expanded group items.
         */
        private void deselectExpandableListViewVisibleItems() {
          for (int i = 0; i <= expandableListView.getLastVisiblePosition(); i++) {
            expandableListView.setItemChecked(i, false);
          }
        }

      };

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.main_options_menu, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int itemId = item.getItemId();

    switch (itemId) {
      case R.id.itemCreateCategory:
        openCategoryCreateFragment();
        break;
      case R.id.itemCreateList:
        openListCreateFragment();
        break;
    }

    return super.onOptionsItemSelected(item);
  }

  private void openListCreateFragment() {
    ListCreateFragment listCreateFragment = new ListCreateFragment();
    listCreateFragment.setTargetFragment(this, 0);
    listCreateFragment.show(getFragmentManager(), "ListCreateFragment");
  }

  private void openCategoryCreateFragment() {
    CategoryCreateFragment categoryCreateFragment = new CategoryCreateFragment();
    categoryCreateFragment.setTargetFragment(this, 0);
    categoryCreateFragment.show(getFragmentManager(), "CategoryCreateFragment");
  }

  private void updatePredefinedListAdapter() {
    predefinedListAdapter = new PredefinedListAdapter(new ArrayList<PredefinedListItem>());
    UpdateAdapterTask updateAdapterTask = new UpdateAdapterTask(dbLoader, predefinedListAdapter);
    updateAdapterTask.execute();
  }

  private void updateCategoryAdapter() {
    if (categoryAdapter == null) {
      categoryAdapter = new CategoryAdapter(new ArrayList<Category>(),
          new HashMap<Category, List<com.example.todocloud.data.List>>());
    }
    UpdateAdapterTask updateAdapterTask = new UpdateAdapterTask(dbLoader, categoryAdapter);
    updateAdapterTask.execute();
  }

  private void updateListAdapter() {
    if (listAdapter == null) {
      listAdapter = new ListAdapter(new ArrayList<com.example.todocloud.data.List>());
    }
    UpdateAdapterTask updateAdapterTask = new UpdateAdapterTask(dbLoader, listAdapter);
    updateAdapterTask.execute();
  }

  private void openListInCategoryCreateFragment() {
    ListInCategoryCreateFragment listInCategoryCreateFragment = new ListInCategoryCreateFragment();
    listInCategoryCreateFragment.setTargetFragment(this, 0);
    String categoryOnlineId = selectedCategories.get(0).getCategoryOnlineId();
    Bundle arguments = new Bundle();
    arguments.putString("categoryOnlineId", categoryOnlineId);
    listInCategoryCreateFragment.setArguments(arguments);
    listInCategoryCreateFragment.show(getFragmentManager(), "ListInCategoryCreateFragment");
  }

  private void modifyListInCategory() {
    com.example.todocloud.data.List list = selectedListsInCategory.get(0);
    ListModifyFragment listModifyFragment = new ListModifyFragment();
    listModifyFragment.setTargetFragment(this, 0);
    Bundle arguments = new Bundle();
    arguments.putParcelable("list", list);
    arguments.putBoolean("isInCategory", true);
    listModifyFragment.setArguments(arguments);
    listModifyFragment.show(getFragmentManager(), "ListModifyFragment");
  }

  private void deleteListInCategory() {
    com.example.todocloud.data.List list = selectedListsInCategory.get(0);
    String onlineId = list.getListOnlineId();
    String title = list.getTitle();
    ConfirmDeleteDialogFragment confirmDeleteDialogFragment = new ConfirmDeleteDialogFragment();
    confirmDeleteDialogFragment.setTargetFragment(this, 0);
    Bundle arguments = new Bundle();
    arguments.putString("type", "listInCategory");
    arguments.putString("title", title);
    arguments.putString("onlineId", onlineId);
    confirmDeleteDialogFragment.setArguments(arguments);
    confirmDeleteDialogFragment.show(getFragmentManager(), "ConfirmDeleteDialogFragment");
  }

  private void deleteListsInCategory() {
    ConfirmDeleteDialogFragment confirmDeleteDialogFragment = new ConfirmDeleteDialogFragment();
    confirmDeleteDialogFragment.setTargetFragment(this, 0);
    Bundle arguments = new Bundle();
    arguments.putString("type", "listInCategory");
    arguments.putParcelableArrayList("items", selectedListsInCategory);
    confirmDeleteDialogFragment.setArguments(arguments);
    confirmDeleteDialogFragment.show(getFragmentManager(), "ConfirmDeleteDialogFragment");
  }

  private void moveListInCategory() {
    com.example.todocloud.data.List list = selectedListsInCategory.get(0);
    Category category = dbLoader.getCategoryByCategoryOnlineId(list.getCategoryOnlineId());
    ListMoveFragment listMoveFragment = new ListMoveFragment();
    listMoveFragment.setTargetFragment(this, 0);
    Bundle arguments = new Bundle();
    arguments.putParcelable("category", category);
    arguments.putParcelable("list", list);
    listMoveFragment.setArguments(arguments);
    listMoveFragment.show(getFragmentManager(), "ListMoveFragment");
  }

  private void moveListIntoAnotherCategory() {
    Category category = new Category("Kategórián kívül");
    com.example.todocloud.data.List list = selectedLists.get(0);
    ListMoveFragment listMoveFragment = new ListMoveFragment();
    listMoveFragment.setTargetFragment(this, 0);
    Bundle arguments = new Bundle();
    arguments.putParcelable("category", category);
    arguments.putParcelable("list", list);
    listMoveFragment.setArguments(arguments);
    listMoveFragment.show(getFragmentManager(), "ListMoveFragment");
  }

  private void deleteCategory() {
    Category category = selectedCategories.get(0);
    String onlineId = category.getCategoryOnlineId();
    String title = category.getTitle();
    ConfirmDeleteDialogFragment confirmDeleteDialogFragment = new ConfirmDeleteDialogFragment();
    confirmDeleteDialogFragment.setTargetFragment(this, 0);
    Bundle arguments = new Bundle();
    arguments.putString("type", "category");
    arguments.putString("title", title);
    arguments.putString("onlineId", onlineId);
    confirmDeleteDialogFragment.setArguments(arguments);
    confirmDeleteDialogFragment.show(getFragmentManager(), "ConfirmDeleteDialogFragment");
  }

  private void deleteCategories() {
    ConfirmDeleteDialogFragment confirmDeleteDialogFragment = new ConfirmDeleteDialogFragment();
    confirmDeleteDialogFragment.setTargetFragment(this, 0);
    Bundle arguments = new Bundle();
    arguments.putString("type", "category");
    arguments.putParcelableArrayList("items", selectedCategories);
    confirmDeleteDialogFragment.setArguments(arguments);
    confirmDeleteDialogFragment.show(getFragmentManager(), "ConfirmDeleteDialogFragment");
  }

  private void deleteList() {
    com.example.todocloud.data.List list = selectedLists.get(0);
    String onlineId = list.getListOnlineId();
    String title = list.getTitle();
    ConfirmDeleteDialogFragment confirmDeleteDialogFragment = new ConfirmDeleteDialogFragment();
    confirmDeleteDialogFragment.setTargetFragment(this, 0);
    Bundle arguments = new Bundle();
    arguments.putString("type", "list");
    arguments.putString("title", title);
    arguments.putString("onlineId", onlineId);
    confirmDeleteDialogFragment.setArguments(arguments);
    confirmDeleteDialogFragment.show(getFragmentManager(), "ConfirmDeleteDialogFragment");
  }

  private void deleteLists() {
    ConfirmDeleteDialogFragment confirmDeleteDialogFragment = new ConfirmDeleteDialogFragment();
    confirmDeleteDialogFragment.setTargetFragment(this, 0);
    Bundle arguments = new Bundle();
    arguments.putString("type", "list");
    arguments.putParcelableArrayList("items", selectedLists);
    confirmDeleteDialogFragment.setArguments(arguments);
    confirmDeleteDialogFragment.show(getFragmentManager(), "ConfirmDeleteDialogFragment");
  }

  private void modifyCategory() {
    Category category = selectedCategories.get(0);
    CategoryModifyFragment categoryModifyFragment = new CategoryModifyFragment();
    categoryModifyFragment.setTargetFragment(this, 0);
    Bundle arguments = new Bundle();
    arguments.putParcelable("category", category);
    categoryModifyFragment.setArguments(arguments);
    categoryModifyFragment.show(getFragmentManager(), "CategoryModifyFragment");
  }

  private void modifyList() {
    com.example.todocloud.data.List list = selectedLists.get(0);
    ListModifyFragment listModifyFragment = new ListModifyFragment();
    listModifyFragment.setTargetFragment(this, 0);
    Bundle arguments = new Bundle();
    arguments.putParcelable("list", list);
    listModifyFragment.setArguments(arguments);
    listModifyFragment.show(getFragmentManager(), "ListModifyFragment");
  }

  /**
   * Call update and insert methods only, if get requests processed successfully. Otherwise the
   * client will have data in the local database with the biggest current row_version before it
   * get all of the data from the remote database, which is missing in the local database. Hence
   * it don't will get that data.
   * If an error occurs in the processing of the requests, they should be aborted and start the
   * whole processing from the beginning, with the call of get methods.
   */
  private void sync() {
    todoDataSynchronizer.getTodos();
    swipeRefreshLayout.setRefreshing(false);
  }

  private AdapterView.OnItemClickListener predefinedListItemClicked =
      new AdapterView.OnItemClickListener() {

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      if (!AppController.isActionModeEnabled()) {
        PredefinedListItem predefinedListItem = (PredefinedListItem) parent.getAdapter().getItem(
            position);
        listener.onOpenTodoListFragment(predefinedListItem);
      }
    }

  };

  private AdapterView.OnItemClickListener listItemClicked = new AdapterView.OnItemClickListener() {

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      com.example.todocloud.data.List clickedList =
          (com.example.todocloud.data.List) listAdapter.getItem(position);
      if (!AppController.isActionModeEnabled()) {
        listener.onOpenTodoListFragment(clickedList);
      } else {
        handleItemSelection(position, clickedList);
      }
    }

    private void handleItemSelection(int position, com.example.todocloud.data.List clickedList) {
      // Item checked state being set automatically on item click event. We should track
      // the changes only.
      if (list.isItemChecked(position)) {
        selectedLists.add(clickedList);
      } else {
        selectedLists.remove(clickedList);
      }

      if (isNoSelectedItems()) {
        actionMode.finish();
      } else {
        actionMode.invalidate();
      }
    }

    private boolean isNoSelectedItems() {
      int checkedItemCount = getCheckedItemCount();
      return checkedItemCount == 0;
    }

    private int getCheckedItemCount() {
      return list.getCheckedItemCount() +
          expandableListView.getCheckedItemCount();
    }

  };

  private AdapterView.OnItemLongClickListener listItemLongClicked =
      new AdapterView.OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
          if (!AppController.isActionModeEnabled()) {
            startActionMode(position);
          }

          return true;
        }

        private void startActionMode(int position) {
          listener.onStartActionMode(callback);
          list.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
          list.setItemChecked(position, true);
          com.example.todocloud.data.List selectedList =
              (com.example.todocloud.data.List) list.getItemAtPosition(position);
          selectedLists.add(selectedList);
          expandableListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
          actionMode.invalidate();
        }

      };

  private ExpandableListView.OnChildClickListener expLVChildClicked =
      new ExpandableListView.OnChildClickListener() {

        @Override
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                    int childPosition, long id) {
          com.example.todocloud.data.List clickedList = (com.example.todocloud.data.List)
              categoryAdapter.getChild(groupPosition, childPosition);
          long packedPosition = ExpandableListView.getPackedPositionForChild(
              groupPosition,
              childPosition
          );
          int position = parent.getFlatListPosition(packedPosition);
          if (!AppController.isActionModeEnabled()) {
            listener.onOpenTodoListFragment(clickedList);
          } else {
            handleItemSelection(parent, clickedList, position);
          }

          return true;
        }

        private void handleItemSelection(ExpandableListView parent, com.example.todocloud.data.List clickedList, int position) {
          toggleItemCheckedState(parent, position);
          if (parent.isItemChecked(position)) {
            selectedListsInCategory.add(clickedList);
          } else {
            selectedListsInCategory.remove(clickedList);
          }

          if (isNoSelectedItems()) {
            actionMode.finish();
          } else {
            actionMode.invalidate();
          }
        }

        private void toggleItemCheckedState(ExpandableListView parent, int position) {
          expandableListView.setItemChecked(position, !parent.isItemChecked(position));
        }

        private boolean isNoSelectedItems() {
          int checkedItemCount = getCheckedItemCount();
          return checkedItemCount == 0;
        }

        private int getCheckedItemCount() {
          return list.getCheckedItemCount() +
              expandableListView.getCheckedItemCount();
        }

      };

  private ExpandableListView.OnGroupClickListener expLVGroupClicked =
      new ExpandableListView.OnGroupClickListener() {

        @Override
        public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
          long packedPosition = ExpandableListView.getPackedPositionForGroup(groupPosition);
          int position = parent.getFlatListPosition(packedPosition);
          if (!AppController.isActionModeEnabled()) {
            handleGroupExpanding(parent, groupPosition);
          } else {
            handleItemSelection(parent, position);
          }

          return true;
        }

        private void handleItemSelection(ExpandableListView parent, int position) {
          toggleItemCheckedState(parent, position);
          Category clickedCategory = (Category) parent.getItemAtPosition(position);
          if (parent.isItemChecked(position)) {
            selectedCategories.add(clickedCategory);
          } else {
            selectedCategories.remove(clickedCategory);
          }

          if (isNoSelectedItems()) {
            actionMode.finish();
          } else {
            actionMode.invalidate();
          }
        }

        private void toggleItemCheckedState(ExpandableListView parent, int position) {
          expandableListView.setItemChecked(position, !parent.isItemChecked(position));
        }

        private void handleGroupExpanding(ExpandableListView parent, int groupPosition) {
          if (!parent.isGroupExpanded(groupPosition)) {
            parent.expandGroup(groupPosition);
          } else {
            parent.collapseGroup(groupPosition);
          }
        }

        private boolean isNoSelectedItems() {
          int checkedItemCount = getCheckedItemCount();
          return checkedItemCount == 0;
        }

        private int getCheckedItemCount() {
          return list.getCheckedItemCount() +
              expandableListView.getCheckedItemCount();
        }

      };

  private ExpandableListView.OnCreateContextMenuListener expLVCategoryContextMenuListener =
      new ExpandableListView.OnCreateContextMenuListener() {

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
      if (!AppController.isActionModeEnabled()) {
        ExpandableListView.ExpandableListContextMenuInfo info =
            (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
        long packedPosition = info.packedPosition;
        int position = expandableListView.getFlatListPosition(packedPosition);
        int packedPositionType = ExpandableListView.getPackedPositionType(packedPosition);

        if (categoryClicked(packedPositionType)) {
          startActionModeWithCategory(position);
        } else if (listClicked(packedPositionType)) {
          startActionModeWithList(position);
        }
      }
    }

        private void startActionModeWithList(int position) {
          actionModeStartedWithELV = true;
          listener.onStartActionMode(callback);
          expandableListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
          expandableListView.setItemChecked(position, true);
          com.example.todocloud.data.List clickedList = (com.example.todocloud.data.List)
              expandableListView.getItemAtPosition(position);
          selectedListsInCategory.add(clickedList);
          list.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
          actionMode.invalidate();
        }

        private void startActionModeWithCategory(int position) {
          actionModeStartedWithELV = true;
          listener.onStartActionMode(callback);
          expandableListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
          expandableListView.setItemChecked(position, true);
          Category clickedCategory = (Category) expandableListView.getItemAtPosition(position);
          selectedCategories.add(clickedCategory);
          list.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
          actionMode.invalidate();
        }

        private boolean listClicked(int packedPositionType) {
          return packedPositionType == ExpandableListView.PACKED_POSITION_TYPE_CHILD;
        }

        private boolean categoryClicked(int packedPositionType) {
          return packedPositionType == ExpandableListView.PACKED_POSITION_TYPE_GROUP;
        }

      };

  @Override
  public void onCreateCategory(Category category) {
    createCategoryInLocalDatabase(category);
    updateCategoryAdapter();
  }

  private void createCategoryInLocalDatabase(Category category) {
    category.setUserOnlineId(dbLoader.getUserOnlineId());
    category.set_id(dbLoader.createCategory(category));
    String categoryOnlineId = OnlineIdGenerator.generateOnlineId(
        DbConstants.Category.DATABASE_TABLE,
        category.get_id(),
        dbLoader.getApiKey()
    );
    category.setCategoryOnlineId(categoryOnlineId);
    dbLoader.updateCategory(category);
  }

  @Override
  public void onModifyCategory(Category category) {
    category.setDirty(true);
    dbLoader.updateCategory(category);
    updateCategoryAdapter();
    actionMode.finish();
  }

  @Override
  public void onCreateList(com.example.todocloud.data.List list) {
    createListInLocalDatabase(list);
    updateListAdapter();
  }

  private void createListInLocalDatabase(com.example.todocloud.data.List list) {
    list.setUserOnlineId(dbLoader.getUserOnlineId());
    list.set_id(dbLoader.createList(list));
    String listOnlineId = OnlineIdGenerator.generateOnlineId(
        DbConstants.List.DATABASE_TABLE,
        list.get_id(),
        dbLoader.getApiKey()
    );
    list.setListOnlineId(listOnlineId);
    dbLoader.updateList(list);
  }

  @Override
  public void onModifyList(com.example.todocloud.data.List list, boolean isInCategory) {
    list.setDirty(true);
    dbLoader.updateList(list);
    if (isInCategory) {
      updateCategoryAdapter();
    } else {
      updateListAdapter();
    }
    actionMode.finish();
  }

  @Override
  public void onCreateListInCategory(com.example.todocloud.data.List list,
                                     String categoryOnlineId) {
    createListInCategoryInLocalDatabase(list, categoryOnlineId);
    updateCategoryAdapter();
    actionMode.finish();
  }

  private void createListInCategoryInLocalDatabase(
      com.example.todocloud.data.List list,
      String categoryOnlineId
  ) {
    list.setUserOnlineId(dbLoader.getUserOnlineId());
    list.setCategoryOnlineId(categoryOnlineId);
    list.set_id(dbLoader.createList(list));
    String listOnlineId = OnlineIdGenerator.generateOnlineId(
        DbConstants.List.DATABASE_TABLE,
        list.get_id(),
        dbLoader.getApiKey()
    );
    list.setListOnlineId(listOnlineId);
    dbLoader.updateList(list);
  }

  @Override
  public void onMoveList(com.example.todocloud.data.List list, String categoryOnlineId,
                         boolean listIsNotInCategory) {
    switch (listIsNotInCategory ? "listIsNotInCategory" : "listIsInCategory") {
      case "listIsNotInCategory":
        if (moveListOutsideCategory(categoryOnlineId)) {
          actionMode.finish();
        } else {
          moveListIntoCategory(list, categoryOnlineId);
          actionMode.finish();
        }
        break;
      case "listIsInCategory":
      if (moveListOutsideCategory(categoryOnlineId)) {
          moveListOutsideCategory(list);
          actionMode.finish();
        } else {
          moveListIntoAnotherCategory(list, categoryOnlineId);
          actionMode.finish();
        }
      break;
    }
  }

  private void moveListIntoCategory(com.example.todocloud.data.List list, String categoryOnlineId) {
    list.setCategoryOnlineId(categoryOnlineId);
    list.setDirty(true);
    dbLoader.updateList(list);
    updateListAdapter();
    updateCategoryAdapter();
  }

  private void moveListIntoAnotherCategory(com.example.todocloud.data.List list, String categoryOnlineId) {
    list.setCategoryOnlineId(categoryOnlineId);
    list.setDirty(true);
    dbLoader.updateList(list);
    updateCategoryAdapter();
  }

  private void moveListOutsideCategory(com.example.todocloud.data.List list) {
    list.setCategoryOnlineId(null);
    list.setDirty(true);
    dbLoader.updateList(list);
    updateCategoryAdapter();
    updateListAdapter();
  }

  private boolean moveListOutsideCategory(String categoryOnlineId) {
    return categoryOnlineId == null;
  }

  @Override
  public void onRefresh() {
    sync();
  }

  @Override
  public void onSoftDelete(String onlineId, String type) {
    switch (type) {
      case "list":
        dbLoader.deleteListAndTodos(onlineId);
        updateListAdapter();
        actionMode.finish();
        break;
      case "listInCategory":
        dbLoader.deleteListAndTodos(onlineId);
        updateCategoryAdapter();
        actionMode.finish();
        break;
      case "category":
        dbLoader.deleteCategoryAndListsAndTodos(onlineId);
        updateCategoryAdapter();
        actionMode.finish();
        break;
    }
  }

  @Override
  public void onSoftDelete(ArrayList items, String type) {
    switch (type) {
      case "list":
        ArrayList<com.example.todocloud.data.List> lists = items;
        for (com.example.todocloud.data.List list:lists) {
          dbLoader.deleteListAndTodos(list.getListOnlineId());
        }
        updateListAdapter();
        actionMode.finish();
        break;
      case "listInCategory":
        ArrayList<com.example.todocloud.data.List> listsInCategory = items;
        for (com.example.todocloud.data.List list:listsInCategory) {
          dbLoader.deleteListAndTodos(list.getListOnlineId());
        }
        updateCategoryAdapter();
        actionMode.finish();
        break;
      case "category":
        ArrayList<Category> categories = items;
        for (Category category:categories) {
          dbLoader.deleteCategoryAndListsAndTodos(category.getCategoryOnlineId());
        }
        updateCategoryAdapter();
        actionMode.finish();
        break;
    }
  }

  @Override
  public void onLogout() {
    listener.onLogout();
  }

  @Override
  public void onFinishGetTodos() {
    listDataSynchronizer.getLists();
  }

  @Override
  public void onFinishGetLists() {
    categoryDataSynchronizer.getCategories();
  }

  @Override
  public void onFinishGetCategories() {
    todoDataSynchronizer.updateTodos();
  }

  @Override
  public void onFinishUpdateTodos() {
    listDataSynchronizer.updateLists();
  }

  @Override
  public void onFinishUpdateLists() {
    categoryDataSynchronizer.updateCategories();
  }

  @Override
  public void onFinishUpdateCategories() {
    todoDataSynchronizer.insertTodos();
  }

  @Override
  public void onFinishInsertTodos() {
    listDataSynchronizer.insertLists();
  }

  @Override
  public void onFinishInsertLists() {
    categoryDataSynchronizer.insertCategories();
  }

  @Override
  public void onProcessLastListRequest() {
    updateListAdapter();
  }

  @Override
  public void onProcessLastCategoryRequest() {
    updateCategoryAdapter();
  }

  @Override
  public void onSyncError(String errorMessage) {
    showErrorMessage(errorMessage);
  }

  private void showErrorMessage(String errorMessage) {
    if (errorMessage.contains("failed to connect")) {
      // Android Studio hotswap/coldswap may cause getView == null
      if (getView() != null) {
        Snackbar snackbar = Snackbar.make(
            coordinatorLayout,
            R.string.failed_to_connect,
            Snackbar.LENGTH_LONG
        );
        AppController.showWhiteTextSnackbar(snackbar);
      }
    }
  }

  public interface IMainListFragment {
    void onOpenTodoListFragment(PredefinedListItem predefinedListItem);
    void onOpenTodoListFragment(com.example.todocloud.data.List listToOpen);
    void onLogout();
    void onStartActionMode(ActionMode.Callback callback);
    void onOpenSettings();
    void onSetNavigationHeader();
  }

}

package com.rolandvitezhu.todocloud.fragment;

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

import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.adapter.CategoryAdapter;
import com.rolandvitezhu.todocloud.adapter.ListAdapter;
import com.rolandvitezhu.todocloud.adapter.PredefinedListAdapter;
import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.customcomponent.ExpandableHeightExpandableListView;
import com.rolandvitezhu.todocloud.customcomponent.ExpandableHeightListView;
import com.rolandvitezhu.todocloud.data.Category;
import com.rolandvitezhu.todocloud.data.PredefinedList;
import com.rolandvitezhu.todocloud.datastorage.DbConstants;
import com.rolandvitezhu.todocloud.datastorage.DbLoader;
import com.rolandvitezhu.todocloud.datastorage.asynctask.UpdateAdapterTask;
import com.rolandvitezhu.todocloud.datasynchronizer.DataSynchronizer;
import com.rolandvitezhu.todocloud.helper.OnlineIdGenerator;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainListFragment extends ListFragment implements
    CreateCategoryDialogFragment.ICreateCategoryDialogFragment,
    ModifyCategoryDialogFragment.IModifyCategoryDialogFragment,
    CreateListDialogFragment.ICreateListDialogFragment,
    ModifyListDialogFragment.IModifyListDialogFragment,
    CreateListInCategoryDialogFragment.ICreateListInCategoryDialogFragment,
    MoveListDialogFragment.IMoveListDialogFragment,
    SwipeRefreshLayout.OnRefreshListener,
    ConfirmDeleteDialogFragment.IConfirmDeleteDialogFragment,
    LogoutUserDialogFragment.ILogoutUserDialogFragment,
    DataSynchronizer.OnSyncDataListener {

  private DbLoader dbLoader;

  private IMainListFragment listener;

  private DataSynchronizer dataSynchronizer;

  private PredefinedListAdapter predefinedListAdapter;
  private CategoryAdapter categoryAdapter;
  private ListAdapter listAdapter;

  private ExpandableHeightExpandableListView expandableHeightExpandableListView;
  private ExpandableHeightListView ehlvList;

  private SwipeRefreshLayout swipeRefreshLayout;
  private CoordinatorLayout coordinatorLayout;
  private ScrollView scrollView;

  private FloatingActionMenu fam;
  private FloatingActionButton fabCreateCategory;
  private FloatingActionButton fabCreateList;

  private ActionMode actionMode;
  private boolean actionModeStartedWithELV;

  private ArrayList<Category> selectedCategories = new ArrayList<>();
  private ArrayList<com.rolandvitezhu.todocloud.data.List> selectedListsInCategory = new ArrayList<>();
  private ArrayList<com.rolandvitezhu.todocloud.data.List> selectedLists = new ArrayList<>();

  private View.OnClickListener onFabClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      switch (v.getId()) {
        case R.id.main_fab_create_category:
          openCreateCategoryDialogFragment();
          break;
        case R.id.main_fab_create_list:
          openCreateListDialogFragment();
          break;
      }
      fam.close(true);
    }
  };

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (IMainListFragment) context;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    dbLoader = new DbLoader();
    listener.onPrepareNavigationHeader();
    updatePredefinedListAdapter();
    updateCategoryAdapter();
    updateListAdapter();

    dataSynchronizer = new DataSynchronizer(dbLoader);
    dataSynchronizer.setOnSyncDataListener(this);

    dataSynchronizer.syncData();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View combinedListView = inflater.inflate(R.layout.fragment_mainlist, null);
    coordinatorLayout = (CoordinatorLayout) combinedListView.findViewById(
        R.id.coordinatorlayout_mainlist
    );

    preparePredefinedList(combinedListView);
    prepareExpandableListView(combinedListView);
    prepareList(combinedListView);
    prepareSwipeRefreshLayout(combinedListView);
    prepareFloatingActionButtons(combinedListView);

    return combinedListView;
  }

  private void prepareFloatingActionButtons(View combinedListView) {
    fam = combinedListView.findViewById(R.id.main_fam);
    fabCreateCategory = combinedListView.findViewById(R.id.main_fab_create_category);
    fabCreateList = combinedListView.findViewById(R.id.main_fab_create_list);
    fabCreateCategory.setOnClickListener(onFabClickListener);
    fabCreateList.setOnClickListener(onFabClickListener);
  }

  private void prepareSwipeRefreshLayout(View combinedListView) {
    swipeRefreshLayout = (SwipeRefreshLayout)
        combinedListView.findViewById(R.id.swiperefreshlayout_mainlist);
    setScrollViewSwipeRefreshBehavior(combinedListView);
    swipeRefreshLayout.setOnRefreshListener(this);
  }

  private void setScrollViewSwipeRefreshBehavior(View combinedListView) {
    scrollView = (ScrollView) combinedListView.findViewById(R.id.scrollview_mainlist);
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
    ehlvList = (ExpandableHeightListView) combinedListView.findViewById(
        R.id.expandableheightlistview_mainlist_list
    );
    ehlvList.setExpanded(true);
    ehlvList.setAdapter(listAdapter);
    ehlvList.setOnItemClickListener(listItemClicked);
    ehlvList.setOnItemLongClickListener(listItemLongClicked);
  }

  private void prepareExpandableListView(View combinedListView) {
    expandableHeightExpandableListView = (ExpandableHeightExpandableListView)
        combinedListView.findViewById(R.id.expandableheightexpandablelistview_mainlist_category);
    expandableHeightExpandableListView.setExpanded(true);
    expandableHeightExpandableListView.setAdapter(categoryAdapter);
    expandableHeightExpandableListView.setOnChildClickListener(expLVChildClicked);
    expandableHeightExpandableListView.setOnGroupClickListener(expLVGroupClicked);
    applyExpLVLongClickEvents();
  }

  private void applyExpLVLongClickEvents() {
    expandableHeightExpandableListView.setOnCreateContextMenuListener(expLVCategoryContextMenuListener);
  }

  private void preparePredefinedList(View combinedListView) {
    final ExpandableHeightListView ehlvPredefinedList =
        (ExpandableHeightListView) combinedListView.findViewById(
            R.id.expandableheightlistview_mainlist_predefinedlist
        );
    ehlvPredefinedList.setExpanded(true);
    ehlvPredefinedList.setAdapter(predefinedListAdapter);
    ehlvPredefinedList.setOnItemClickListener(predefinedListItemClicked);
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
          expandableHeightExpandableListView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
          ehlvList.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
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
          expandableHeightExpandableListView.setOnTouchListener(new View.OnTouchListener() {

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
          expandableHeightExpandableListView.setOnTouchListener(null);
        }

        @NonNull
        private String prepareActionModeTitle() {
          int checkedItemCount = ehlvList.getCheckedItemCount()
              + expandableHeightExpandableListView.getCheckedItemCount();
          return checkedItemCount + " " + getString(R.string.all_selected);
        }

        private void prepareMenu(ActionMode mode, Menu menu) {
          if (oneCategorySelected()) {
            menu.clear();
            mode.getMenuInflater().inflate(R.menu.layout_appbar_mainlist_group, menu);
          } else if (oneListInCategorySelected()) {
            menu.clear();
            mode.getMenuInflater().inflate(R.menu.layout_appbar_mainlist_child, menu);
          } else if (oneListSelected()) {
            menu.clear();
            mode.getMenuInflater().inflate(R.menu.layout_appbar_mainlist_item, menu);
          } else if (manyCategoriesSelected()) {
            menu.clear();
            mode.getMenuInflater().inflate(R.menu.layout_appbar_mainlist_many_group, menu);
          } else if (manyListsInCategorySelected()) {
            menu.clear();
            mode.getMenuInflater().inflate(R.menu.layout_appbar_mainlist_many_child, menu);
          } else if (manyListsSelected()) {
            menu.clear();
            mode.getMenuInflater().inflate(R.menu.layout_appbar_mainlist_many_item, menu);
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
          int menuItemId = item.getItemId();
          if (oneCategorySelected()) {
            switch (menuItemId) {
              case R.id.menuitem_layoutappbarmainlistgroup_createlist:
                openCreateListInCategoryDialogFragment();
                break;
              case R.id.menuitem_layoutappbarmainlistgroup_modify:
                openModifyCategoryDialogFragment();
                break;
              case R.id.menuitem_layoutappbarmainlistgroup_delete:
                openConfirmDeleteCategoryDialog();
                break;
            }
          } else if (oneListInCategorySelected()) {
            switch (menuItemId) {
              case R.id.menuitem_layoutappbarmainlistchild_modify:
                openModifyListInCategoryDialog();
                break;
              case R.id.menuitem_layoutappbarmainlistchild_delete:
                openConfirmDeleteListInCategoryDialog();
                break;
              case R.id.menuitem_layoutappbarmainlistchild_move:
                openMoveListInCategoryDialog();
                break;
            }
          } else if (oneListSelected()) {
            switch (menuItemId) {
              case R.id.menuitem_layoutappbarmainlistitem_modify:
                openModifyListDialog();
                break;
              case R.id.menuitem_layoutappbarmainlistitem_delete:
                openConfirmDeleteListDialog();
                break;
              case R.id.menuitem_layoutappbarmainlistitem_move:
                openMoveListIntoAnotherCategoryDialog();
                break;
            }
          } else if (manyCategoriesSelected()) {
            if (menuItemId == R.id.menuitem_layoutappbarmainlistmanygroup_delete)
              openConfirmDeleteCategoriesDialog();
          } else if (manyListsInCategorySelected()) {
            if (menuItemId == R.id.menuitem_layoutappbarmainlistmanychild_delete)
              openConfirmDeleteListsInCategoryDialog();
          } else if (manyListsSelected()) {
            if (menuItemId == R.id.menuitem_layoutappbarmainlistmanyitem_delete)
              openConfirmDeleteListsDialog();
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
            ehlvList.setItemChecked(i, false);
          }
        }

        /**
         * Should deselect the visible items. Visible items are the group items, and the child
         * items of expanded group items.
         */
        private void deselectExpandableListViewVisibleItems() {
          for (int i = 0; i <= expandableHeightExpandableListView.getLastVisiblePosition(); i++) {
            expandableHeightExpandableListView.setItemChecked(i, false);
          }
        }

      };

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.fragment_mainlist, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int menuItemId = item.getItemId();

    switch (menuItemId) {
      case R.id.menuitem_mainlist_search:
        listener.onSearchActionItemClick();
        break;
    }

    return super.onOptionsItemSelected(item);
  }

  private void openCreateListDialogFragment() {
    CreateListDialogFragment createListDialogFragment = new CreateListDialogFragment();
    createListDialogFragment.setTargetFragment(this, 0);
    createListDialogFragment.show(getFragmentManager(), "CreateListDialogFragment");
  }

  private void openCreateCategoryDialogFragment() {
    CreateCategoryDialogFragment createCategoryDialogFragment = new CreateCategoryDialogFragment();
    createCategoryDialogFragment.setTargetFragment(this, 0);
    createCategoryDialogFragment.show(getFragmentManager(), "CreateCategoryDialogFragment");
  }

  private void updatePredefinedListAdapter() {
    predefinedListAdapter = new PredefinedListAdapter(new ArrayList<PredefinedList>());
    UpdateAdapterTask updateAdapterTask = new UpdateAdapterTask(dbLoader, predefinedListAdapter);
    updateAdapterTask.execute();
  }

  private void updateCategoryAdapter() {
    if (categoryAdapter == null) {
      categoryAdapter = new CategoryAdapter(new ArrayList<Category>(),
          new HashMap<Category, List<com.rolandvitezhu.todocloud.data.List>>());
    }
    UpdateAdapterTask updateAdapterTask = new UpdateAdapterTask(dbLoader, categoryAdapter);
    updateAdapterTask.execute();
  }

  private void updateListAdapter() {
    if (listAdapter == null) {
      listAdapter = new ListAdapter(new ArrayList<com.rolandvitezhu.todocloud.data.List>());
    }
    UpdateAdapterTask updateAdapterTask = new UpdateAdapterTask(dbLoader, listAdapter);
    updateAdapterTask.execute();
  }

  private void openCreateListInCategoryDialogFragment() {
    String categoryOnlineId = selectedCategories.get(0).getCategoryOnlineId();
    Bundle arguments = new Bundle();
    arguments.putString("categoryOnlineId", categoryOnlineId);
    CreateListInCategoryDialogFragment createListInCategoryDialogFragment = new CreateListInCategoryDialogFragment();
    createListInCategoryDialogFragment.setTargetFragment(this, 0);
    createListInCategoryDialogFragment.setArguments(arguments);
    createListInCategoryDialogFragment.show(getFragmentManager(), "CreateListInCategoryDialogFragment");
  }

  private void openModifyListInCategoryDialog() {
    com.rolandvitezhu.todocloud.data.List list = selectedListsInCategory.get(0);
    Bundle arguments = new Bundle();
    arguments.putParcelable("list", list);
    arguments.putBoolean("isInCategory", true);
    openModifyListDialogFragment(arguments);
  }

  private void openConfirmDeleteListInCategoryDialog() {
    com.rolandvitezhu.todocloud.data.List list = selectedListsInCategory.get(0);
    String onlineId = list.getListOnlineId();
    String title = list.getTitle();
    Bundle arguments = new Bundle();
    arguments.putString("itemType", "listInCategory");
    arguments.putString("itemTitle", title);
    arguments.putString("onlineId", onlineId);
    openConfirmDeleteDialogFragment(arguments);
  }

  private void openConfirmDeleteListsInCategoryDialog() {
    Bundle arguments = new Bundle();
    arguments.putString("itemType", "listInCategory");
    arguments.putParcelableArrayList("itemsToDelete", selectedListsInCategory);
    openConfirmDeleteDialogFragment(arguments);
  }

  private void openMoveListInCategoryDialog() {
    com.rolandvitezhu.todocloud.data.List list = selectedListsInCategory.get(0);
    Category category = dbLoader.getCategoryByCategoryOnlineId(list.getCategoryOnlineId());
    Bundle arguments = new Bundle();
    arguments.putParcelable("category", category);
    arguments.putParcelable("list", list);
    openMoveListDialogFragment(arguments);
  }

  private void openMoveListIntoAnotherCategoryDialog() {
    Category category = new Category("Kategórián kívül");
    com.rolandvitezhu.todocloud.data.List list = selectedLists.get(0);
    Bundle arguments = new Bundle();
    arguments.putParcelable("category", category);
    arguments.putParcelable("list", list);
    openMoveListDialogFragment(arguments);
  }

  private void openMoveListDialogFragment(Bundle arguments) {
    MoveListDialogFragment moveListDialogFragment = new MoveListDialogFragment();
    moveListDialogFragment.setTargetFragment(this, 0);
    moveListDialogFragment.setArguments(arguments);
    moveListDialogFragment.show(getFragmentManager(), "MoveListDialogFragment");
  }

  private void openConfirmDeleteCategoryDialog() {
    Category category = selectedCategories.get(0);
    String onlineId = category.getCategoryOnlineId();
    String title = category.getTitle();
    Bundle arguments = new Bundle();
    arguments.putString("itemType", "category");
    arguments.putString("itemTitle", title);
    arguments.putString("onlineId", onlineId);
    openConfirmDeleteDialogFragment(arguments);
  }

  private void openConfirmDeleteCategoriesDialog() {
    Bundle arguments = new Bundle();
    arguments.putString("itemType", "category");
    arguments.putParcelableArrayList("itemsToDelete", selectedCategories);
    openConfirmDeleteDialogFragment(arguments);
  }

  private void openConfirmDeleteDialogFragment(Bundle arguments) {
    ConfirmDeleteDialogFragment confirmDeleteDialogFragment = new ConfirmDeleteDialogFragment();
    confirmDeleteDialogFragment.setTargetFragment(this, 0);
    confirmDeleteDialogFragment.setArguments(arguments);
    confirmDeleteDialogFragment.show(getFragmentManager(), "ConfirmDeleteDialogFragment");
  }

  private void openConfirmDeleteListDialog() {
    com.rolandvitezhu.todocloud.data.List list = selectedLists.get(0);
    String onlineId = list.getListOnlineId();
    String title = list.getTitle();
    Bundle arguments = new Bundle();
    arguments.putString("itemType", "list");
    arguments.putString("itemTitle", title);
    arguments.putString("onlineId", onlineId);
    openConfirmDeleteDialogFragment(arguments);
  }

  private void openConfirmDeleteListsDialog() {
    Bundle arguments = new Bundle();
    arguments.putString("itemType", "list");
    arguments.putParcelableArrayList("itemsToDelete", selectedLists);
    openConfirmDeleteDialogFragment(arguments);
  }

  private void openModifyCategoryDialogFragment() {
    Category category = selectedCategories.get(0);
    Bundle arguments = new Bundle();
    arguments.putParcelable("category", category);
    ModifyCategoryDialogFragment modifyCategoryDialogFragment = new ModifyCategoryDialogFragment();
    modifyCategoryDialogFragment.setTargetFragment(this, 0);
    modifyCategoryDialogFragment.setArguments(arguments);
    modifyCategoryDialogFragment.show(getFragmentManager(), "ModifyCategoryDialogFragment");
  }

  private void openModifyListDialog() {
    com.rolandvitezhu.todocloud.data.List list = selectedLists.get(0);
    Bundle arguments = new Bundle();
    arguments.putParcelable("list", list);
    openModifyListDialogFragment(arguments);
  }

  private void openModifyListDialogFragment(Bundle arguments) {
    ModifyListDialogFragment modifyListDialogFragment = new ModifyListDialogFragment();
    modifyListDialogFragment.setTargetFragment(this, 0);
    modifyListDialogFragment.setArguments(arguments);
    modifyListDialogFragment.show(getFragmentManager(), "ModifyListDialogFragment");
  }

  private void syncData() {
    dataSynchronizer.syncData();
  }

  private AdapterView.OnItemClickListener predefinedListItemClicked =
      new AdapterView.OnItemClickListener() {

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      if (!AppController.isActionModeEnabled()) {
        PredefinedList predefinedList = (PredefinedList) parent.getAdapter().getItem(
            position);
        listener.onClickPredefinedList(predefinedList);
      }
    }

  };

  private AdapterView.OnItemClickListener listItemClicked = new AdapterView.OnItemClickListener() {

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      com.rolandvitezhu.todocloud.data.List clickedList =
          (com.rolandvitezhu.todocloud.data.List) listAdapter.getItem(position);
      if (!AppController.isActionModeEnabled()) {
        listener.onClickList(clickedList);
      } else {
        handleItemSelection(position, clickedList);
      }
    }

    private void handleItemSelection(int position, com.rolandvitezhu.todocloud.data.List clickedList) {
      // Item checked state being set automatically on item click event. We should track
      // the changes only.
      if (ehlvList.isItemChecked(position)) {
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
      return ehlvList.getCheckedItemCount() +
          expandableHeightExpandableListView.getCheckedItemCount();
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
          ehlvList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
          ehlvList.setItemChecked(position, true);
          com.rolandvitezhu.todocloud.data.List selectedList =
              (com.rolandvitezhu.todocloud.data.List) ehlvList.getItemAtPosition(position);
          selectedLists.add(selectedList);
          expandableHeightExpandableListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
          actionMode.invalidate();
        }

      };

  private ExpandableListView.OnChildClickListener expLVChildClicked =
      new ExpandableListView.OnChildClickListener() {

        @Override
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                    int childPosition, long id) {
          com.rolandvitezhu.todocloud.data.List clickedList = (com.rolandvitezhu.todocloud.data.List)
              categoryAdapter.getChild(groupPosition, childPosition);
          long packedPosition = ExpandableListView.getPackedPositionForChild(
              groupPosition,
              childPosition
          );
          int position = parent.getFlatListPosition(packedPosition);
          if (!AppController.isActionModeEnabled()) {
            listener.onClickList(clickedList);
          } else {
            handleItemSelection(parent, clickedList, position);
          }

          return true;
        }

        private void handleItemSelection(ExpandableListView parent, com.rolandvitezhu.todocloud.data.List clickedList, int position) {
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
          expandableHeightExpandableListView.setItemChecked(position, !parent.isItemChecked(position));
        }

        private boolean isNoSelectedItems() {
          int checkedItemCount = getCheckedItemCount();
          return checkedItemCount == 0;
        }

        private int getCheckedItemCount() {
          return ehlvList.getCheckedItemCount() +
              expandableHeightExpandableListView.getCheckedItemCount();
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
          expandableHeightExpandableListView.setItemChecked(position, !parent.isItemChecked(position));
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
          return ehlvList.getCheckedItemCount() +
              expandableHeightExpandableListView.getCheckedItemCount();
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
        int position = expandableHeightExpandableListView.getFlatListPosition(packedPosition);
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
          expandableHeightExpandableListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
          expandableHeightExpandableListView.setItemChecked(position, true);
          com.rolandvitezhu.todocloud.data.List clickedList = (com.rolandvitezhu.todocloud.data.List)
              expandableHeightExpandableListView.getItemAtPosition(position);
          selectedListsInCategory.add(clickedList);
          ehlvList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
          actionMode.invalidate();
        }

        private void startActionModeWithCategory(int position) {
          actionModeStartedWithELV = true;
          listener.onStartActionMode(callback);
          expandableHeightExpandableListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
          expandableHeightExpandableListView.setItemChecked(position, true);
          Category clickedCategory = (Category) expandableHeightExpandableListView.getItemAtPosition(position);
          selectedCategories.add(clickedCategory);
          ehlvList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
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
  public void onCreateList(com.rolandvitezhu.todocloud.data.List list) {
    createListInLocalDatabase(list);
    updateListAdapter();
  }

  private void createListInLocalDatabase(com.rolandvitezhu.todocloud.data.List list) {
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
  public void onModifyList(com.rolandvitezhu.todocloud.data.List list, boolean isInCategory) {
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
  public void onCreateListInCategory(com.rolandvitezhu.todocloud.data.List list,
                                     String categoryOnlineId) {
    createListInCategoryInLocalDatabase(list, categoryOnlineId);
    updateCategoryAdapter();
    actionMode.finish();
  }

  private void createListInCategoryInLocalDatabase(
      com.rolandvitezhu.todocloud.data.List list,
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
  public void onMoveList(com.rolandvitezhu.todocloud.data.List list, String categoryOnlineId,
                         boolean isListNotInCategoryBeforeMove) {
    switch (isListNotInCategoryBeforeMove ?
        "isListNotInCategoryBeforeMove" : "isListInCategoryBeforeMove") {
      case "isListNotInCategoryBeforeMove":
        if (moveListOutsideCategory(categoryOnlineId)) {
          actionMode.finish();
        } else {
          moveListIntoCategory(list, categoryOnlineId);
          actionMode.finish();
        }
        break;
      case "isListInCategoryBeforeMove":
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

  private void moveListIntoCategory(
      com.rolandvitezhu.todocloud.data.List list,
      String categoryOnlineId
  ) {
    list.setCategoryOnlineId(categoryOnlineId);
    list.setDirty(true);
    dbLoader.updateList(list);
    updateListAdapter();
    updateCategoryAdapter();
  }

  private void moveListIntoAnotherCategory(
      com.rolandvitezhu.todocloud.data.List list,
      String categoryOnlineId
  ) {
    list.setCategoryOnlineId(categoryOnlineId);
    list.setDirty(true);
    dbLoader.updateList(list);
    updateCategoryAdapter();
  }

  private void moveListOutsideCategory(com.rolandvitezhu.todocloud.data.List list) {
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
    syncData();
  }

  @Override
  public void onSoftDelete(String onlineId, String itemType) {
    switch (itemType) {
      case "list":
        dbLoader.softDeleteListAndRelatedTodos(onlineId);
        updateListAdapter();
        actionMode.finish();
        break;
      case "listInCategory":
        dbLoader.softDeleteListAndRelatedTodos(onlineId);
        updateCategoryAdapter();
        actionMode.finish();
        break;
      case "category":
        dbLoader.softDeleteCategoryAndListsAndTodos(onlineId);
        updateCategoryAdapter();
        actionMode.finish();
        break;
    }
  }

  @Override
  public void onSoftDelete(ArrayList itemsToDelete, String itemType) {
    switch (itemType) {
      case "list":
        ArrayList<com.rolandvitezhu.todocloud.data.List> lists = itemsToDelete;
        for (com.rolandvitezhu.todocloud.data.List list:lists) {
          dbLoader.softDeleteListAndRelatedTodos(list.getListOnlineId());
        }
        updateListAdapter();
        actionMode.finish();
        break;
      case "listInCategory":
        ArrayList<com.rolandvitezhu.todocloud.data.List> listsInCategory = itemsToDelete;
        for (com.rolandvitezhu.todocloud.data.List list:listsInCategory) {
          dbLoader.softDeleteListAndRelatedTodos(list.getListOnlineId());
        }
        updateCategoryAdapter();
        actionMode.finish();
        break;
      case "category":
        ArrayList<Category> categories = itemsToDelete;
        for (Category category:categories) {
          dbLoader.softDeleteCategoryAndListsAndTodos(category.getCategoryOnlineId());
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
  public void onFinishSyncListData() {
    updateListAdapter();
    updateCategoryAdapter();
  }

  @Override
  public void onFinishSyncCategoryData() {
    updateCategoryAdapter();
  }

  @Override
  public void onFinishSyncData() {
    swipeRefreshLayout.setRefreshing(false);
  }

  @Override
  public void onSyncError(String errorMessage) {
    showErrorMessage(errorMessage);
  }

  private void showErrorMessage(String errorMessage) {
    if (errorMessage.contains("failed to connect")) {
      showFailedToConnectError();
    } else {
      showAnErrorOccurredError();
    }
  }

  private void showFailedToConnectError() {
    // Android Studio hotswap/coldswap may cause getView == null
    if (getView() != null) {
      Snackbar snackbar = Snackbar.make(
          coordinatorLayout,
          R.string.all_failedtoconnect,
          Snackbar.LENGTH_LONG
      );
      AppController.showWhiteTextSnackbar(snackbar);
    }
  }

  private void showAnErrorOccurredError() {
    Snackbar snackbar = Snackbar.make(
        coordinatorLayout,
        R.string.all_anerroroccurred,
        Snackbar.LENGTH_LONG
    );
    AppController.showWhiteTextSnackbar(snackbar);
  }

  public interface IMainListFragment {
    void onClickPredefinedList(PredefinedList predefinedList);
    void onClickList(com.rolandvitezhu.todocloud.data.List list);
    void onLogout();
    void onStartActionMode(ActionMode.Callback callback);
    void onPrepareNavigationHeader();
    void onSearchActionItemClick();
  }

}

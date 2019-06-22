package com.rolandvitezhu.todocloud.ui.activity.main.fragment;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import com.github.clans.fab.FloatingActionMenu;
import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.customcomponent.ExpandableHeightExpandableListView;
import com.rolandvitezhu.todocloud.customcomponent.ExpandableHeightListView;
import com.rolandvitezhu.todocloud.data.Category;
import com.rolandvitezhu.todocloud.data.PredefinedList;
import com.rolandvitezhu.todocloud.data.User;
import com.rolandvitezhu.todocloud.datastorage.DbConstants;
import com.rolandvitezhu.todocloud.datastorage.DbLoader;
import com.rolandvitezhu.todocloud.datastorage.asynctask.UpdateViewModelTask;
import com.rolandvitezhu.todocloud.datasynchronizer.DataSynchronizer;
import com.rolandvitezhu.todocloud.fragment.ConfirmDeleteDialogFragment;
import com.rolandvitezhu.todocloud.fragment.CreateCategoryDialogFragment;
import com.rolandvitezhu.todocloud.fragment.CreateListDialogFragment;
import com.rolandvitezhu.todocloud.fragment.CreateListInCategoryDialogFragment;
import com.rolandvitezhu.todocloud.fragment.ModifyCategoryDialogFragment;
import com.rolandvitezhu.todocloud.fragment.ModifyListDialogFragment;
import com.rolandvitezhu.todocloud.fragment.MoveListDialogFragment;
import com.rolandvitezhu.todocloud.helper.OnlineIdGenerator;
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity;
import com.rolandvitezhu.todocloud.ui.activity.main.adapter.CategoryAdapter;
import com.rolandvitezhu.todocloud.ui.activity.main.adapter.ListAdapter;
import com.rolandvitezhu.todocloud.ui.activity.main.adapter.PredefinedListAdapter;
import com.rolandvitezhu.todocloud.viewmodel.CategoriesViewModel;
import com.rolandvitezhu.todocloud.viewmodel.ListsViewModel;
import com.rolandvitezhu.todocloud.viewmodel.PredefinedListsViewModel;
import com.rolandvitezhu.todocloud.viewmodel.UserViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.disposables.CompositeDisposable;

public class MainListFragment extends ListFragment implements
    SwipeRefreshLayout.OnRefreshListener,
    DataSynchronizer.OnSyncDataListener {

  private final String TAG = getClass().getSimpleName();

  private CompositeDisposable disposable = new CompositeDisposable();

  @Inject
  DbLoader dbLoader;
  @Inject
  DataSynchronizer dataSynchronizer;
  @Inject
  PredefinedListAdapter predefinedListAdapter;
  @Inject
  CategoryAdapter categoryAdapter;
  @Inject
  ListAdapter listAdapter;

  @BindView(R.id.expandableheightlistview_mainlist_predefinedlist)
  ExpandableHeightListView ehlvPredefinedList;
  @BindView(R.id.expandableheightexpandablelistview_mainlist_category)
  ExpandableHeightExpandableListView expandableHeightExpandableListView;
  @BindView(R.id.expandableheightlistview_mainlist_list)
  ExpandableHeightListView ehlvList;

  @BindView(R.id.swiperefreshlayout_mainlist)
  SwipeRefreshLayout swipeRefreshLayout;
  @BindView(R.id.coordinatorlayout_mainlist)
  CoordinatorLayout coordinatorLayout;

  @BindView(R.id.scrollview_mainlist)
  ScrollView scrollView;

  @BindView(R.id.main_fam)
  FloatingActionMenu fam;

  private ActionMode actionMode;
  private boolean actionModeStartedWithELV;

  private ArrayList<Category> selectedCategories = new ArrayList<>();
  private ArrayList<com.rolandvitezhu.todocloud.data.List> selectedListsInCategory = new ArrayList<>();
  private ArrayList<com.rolandvitezhu.todocloud.data.List> selectedLists = new ArrayList<>();

  private CategoriesViewModel categoriesViewModel;
  private ListsViewModel listsViewModel;
  private PredefinedListsViewModel predefinedListsViewModel;
  private UserViewModel userViewModel;

  Unbinder unbinder;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    ((AppController) getActivity().getApplication()).getAppComponent().inject(this);

    categoriesViewModel = ViewModelProviders.of(getActivity()).get(CategoriesViewModel.class);
    listsViewModel = ViewModelProviders.of(getActivity()).get(ListsViewModel.class);
    predefinedListsViewModel = ViewModelProviders.of(getActivity()).get(PredefinedListsViewModel.class);
    userViewModel = ViewModelProviders.of(getActivity()).get(UserViewModel.class);

    categoriesViewModel.getCategories().observe(
        this,
        new Observer<HashMap<Category, List<com.rolandvitezhu.todocloud.data.List>>>() {

          @Override
          public void onChanged(
              @Nullable HashMap<Category,
                  List<com.rolandvitezhu.todocloud.data.List>> hmCategories) {
            categoryAdapter.update(hmCategories);
            categoryAdapter.notifyDataSetChanged();
          }
        }
    );
    listsViewModel.getLists().observe(
        this, new Observer<List<com.rolandvitezhu.todocloud.data.List>>() {

      @Override
      public void onChanged(@Nullable List<com.rolandvitezhu.todocloud.data.List> lists) {
        listAdapter.update(lists);
        listAdapter.notifyDataSetChanged();
      }
    });
    predefinedListsViewModel.getPredefinedLists().observe(
        this, new Observer<List<PredefinedList>>() {

          @Override
          public void onChanged(@Nullable List<PredefinedList> predefinedLists) {
            predefinedListAdapter.update(predefinedLists);
            predefinedListAdapter.notifyDataSetChanged();
          }
        }
    );
    userViewModel.getUser().observe(
        this, new Observer<User>() {

          @Override
          public void onChanged(@Nullable User user) {
            ((MainActivity)MainListFragment.this.getActivity()).updateNavigationHeader();
          }
        }
    );

    ((MainActivity)MainListFragment.this.getActivity()).onPrepareNavigationHeader();

    updatePredefinedListsViewModel();
    updateCategoriesViewModel();
    updateListsViewModel();

    dataSynchronizer.setOnSyncDataListener(this);

    dataSynchronizer.syncData(disposable);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_mainlist, null);
    unbinder = ButterKnife.bind(this, view);

    preparePredefinedList();
    prepareExpandableListView();
    prepareList();
    prepareSwipeRefreshLayout(view);

    return view;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
    disposable.clear();
  }

  private void prepareSwipeRefreshLayout(View view) {
    setScrollViewSwipeRefreshBehavior(view);
    swipeRefreshLayout.setOnRefreshListener(this);
  }

  private void setScrollViewSwipeRefreshBehavior(View view) {
    scrollView.getViewTreeObserver().addOnScrollChangedListener(
        new ViewTreeObserver.OnScrollChangedListener() {

          @Override
          public void onScrollChanged() {
              try {
                int scrollY = scrollView.getScrollY();
                if (shouldSwipeRefresh(scrollY))
                  swipeRefreshLayout.setEnabled(true);
                else
                  swipeRefreshLayout.setEnabled(false);
              } catch (NullPointerException e) {
                // ScrollView nor SwipeRefreshLayout doesn't exists already.
              }
          }

          private boolean shouldSwipeRefresh(int scrollY) {
            return scrollY == 0 && !AppController.isActionModeEnabled();
          }

        });
  }

  private void prepareList() {
    ehlvList.setExpanded(true);
    ehlvList.setAdapter(listAdapter);
    ehlvList.setOnItemClickListener(listItemClicked);
    ehlvList.setOnItemLongClickListener(listItemLongClicked);
  }

  private void prepareExpandableListView() {
    expandableHeightExpandableListView.setExpanded(true);
    expandableHeightExpandableListView.setAdapter(categoryAdapter);
    expandableHeightExpandableListView.setOnChildClickListener(expLVChildClicked);
    expandableHeightExpandableListView.setOnGroupClickListener(expLVGroupClicked);
    applyExpLVLongClickEvents();
  }

  private void applyExpLVLongClickEvents() {
    expandableHeightExpandableListView.setOnCreateContextMenuListener(expLVCategoryContextMenuListener);
  }

  private void preparePredefinedList() {
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
        ((MainActivity)MainListFragment.this.getActivity()).onSearchActionItemClick();
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

  private void updatePredefinedListsViewModel() {
    UpdateViewModelTask updateViewModelTask = new UpdateViewModelTask(predefinedListsViewModel, getActivity());
    updateViewModelTask.execute();
  }

  private void updateCategoriesViewModel() {
    UpdateViewModelTask updateViewModelTask = new UpdateViewModelTask(categoriesViewModel, getActivity());
    updateViewModelTask.execute();
  }

  private void updateListsViewModel() {
    UpdateViewModelTask updateViewModelTask = new UpdateViewModelTask(listsViewModel, getActivity());
    updateViewModelTask.execute();
  }

  private void openCreateListInCategoryDialogFragment() {
    categoriesViewModel.setCategory(selectedCategories.get(0));

    CreateListInCategoryDialogFragment createListInCategoryDialogFragment = new CreateListInCategoryDialogFragment();
    createListInCategoryDialogFragment.setTargetFragment(this, 0);
    createListInCategoryDialogFragment.show(getFragmentManager(), "CreateListInCategoryDialogFragment");
  }

  private void openModifyListInCategoryDialog() {
    listsViewModel.setList(selectedListsInCategory.get(0));
    listsViewModel.setIsInCategory(true);
    openModifyListDialogFragment();
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
    categoriesViewModel.setCategory(category);
    listsViewModel.setList(list);

    openMoveListDialogFragment();
  }

  private void openMoveListIntoAnotherCategoryDialog() {
    Category category = new Category(getString(R.string.movelist_spinneritemlistnotincategory));
    com.rolandvitezhu.todocloud.data.List list = selectedLists.get(0);
    categoriesViewModel.setCategory(category);
    listsViewModel.setList(list);

    openMoveListDialogFragment();
  }

  private void openMoveListDialogFragment() {
    MoveListDialogFragment moveListDialogFragment = new MoveListDialogFragment();
    moveListDialogFragment.setTargetFragment(this, 0);
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
    categoriesViewModel.setCategory(category);

    ModifyCategoryDialogFragment modifyCategoryDialogFragment = new ModifyCategoryDialogFragment();
    modifyCategoryDialogFragment.setTargetFragment(this, 0);
    modifyCategoryDialogFragment.show(getFragmentManager(), "ModifyCategoryDialogFragment");
  }

  private void openModifyListDialog() {
    listsViewModel.setList(selectedLists.get(0));
    openModifyListDialogFragment();
  }

  private void openModifyListDialogFragment() {
    ModifyListDialogFragment modifyListDialogFragment = new ModifyListDialogFragment();
    modifyListDialogFragment.setTargetFragment(this, 0);
    modifyListDialogFragment.show(getFragmentManager(), "ModifyListDialogFragment");
  }

  private void syncData() {
    dataSynchronizer.syncData(disposable);
  }

  private AdapterView.OnItemClickListener predefinedListItemClicked =
      new AdapterView.OnItemClickListener() {

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      if (!AppController.isActionModeEnabled()) {
        PredefinedList predefinedList = (PredefinedList) parent.getAdapter().getItem(
            position);
        ((MainActivity)MainListFragment.this.getActivity()).onClickPredefinedList(predefinedList);
      }
    }

  };

  private AdapterView.OnItemClickListener listItemClicked = new AdapterView.OnItemClickListener() {

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      com.rolandvitezhu.todocloud.data.List clickedList =
          (com.rolandvitezhu.todocloud.data.List) listAdapter.getItem(position);
      if (!AppController.isActionModeEnabled()) {
        ((MainActivity)MainListFragment.this.getActivity()).onClickList(clickedList);
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
      return getCheckedItemCount() == 0;
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
          ((MainActivity)MainListFragment.this.getActivity()).onStartActionMode(callback);
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
            ((MainActivity)MainListFragment.this.getActivity()).onClickList(clickedList);
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
          return getCheckedItemCount() == 0;
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
          ((MainActivity)MainListFragment.this.getActivity()).onStartActionMode(callback);
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
          ((MainActivity)MainListFragment.this.getActivity()).onStartActionMode(callback);
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

  public void onCreateCategory() {
    createCategoryInLocalDatabase(categoriesViewModel.getCategory());
    updateCategoriesViewModel();
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

  public void onModifyCategory() {
    Category category = categoriesViewModel.getCategory();
    category.setDirty(true);
    dbLoader.updateCategory(category);
    updateCategoriesViewModel();
    actionMode.finish();
  }

  public void onCreateList() {
    createListInLocalDatabase(listsViewModel.getList());
    updateListsViewModel();
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

  public void onModifyList() {
    com.rolandvitezhu.todocloud.data.List list = listsViewModel.getList();
    list.setDirty(true);
    dbLoader.updateList(list);

    if (listsViewModel.isInCategory())
      updateCategoriesViewModel();
    else
      updateListsViewModel();

    actionMode.finish();
  }

  public void onCreateListInCategory() {
    createListInCategoryInLocalDatabase(
        listsViewModel.getList(),
        categoriesViewModel.getCategory().getCategoryOnlineId()
        );
    updateCategoriesViewModel();
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

  public void onMoveList(boolean isListNotInCategoryBeforeMove) {
    String categoryOnlineId = categoriesViewModel.getCategory().getCategoryOnlineId();
    com.rolandvitezhu.todocloud.data.List list = listsViewModel.getList();

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
    updateListsViewModel();
    updateCategoriesViewModel();
  }

  private void moveListIntoAnotherCategory(
      com.rolandvitezhu.todocloud.data.List list,
      String categoryOnlineId
  ) {
    list.setCategoryOnlineId(categoryOnlineId);
    list.setDirty(true);
    dbLoader.updateList(list);
    updateCategoriesViewModel();
  }

  private void moveListOutsideCategory(com.rolandvitezhu.todocloud.data.List list) {
    list.setCategoryOnlineId(null);
    list.setDirty(true);
    dbLoader.updateList(list);
    updateCategoriesViewModel();
    updateListsViewModel();
  }

  private boolean moveListOutsideCategory(String categoryOnlineId) {
    return categoryOnlineId == null;
  }

  @Override
  public void onRefresh() {
    syncData();
  }

  public void onSoftDelete(String onlineId, String itemType) {
    switch (itemType) {
      case "list":
        dbLoader.softDeleteListAndRelatedTodos(onlineId);
        updateListsViewModel();
        actionMode.finish();
        break;
      case "listInCategory":
        dbLoader.softDeleteListAndRelatedTodos(onlineId);
        updateCategoriesViewModel();
        actionMode.finish();
        break;
      case "category":
        dbLoader.softDeleteCategoryAndListsAndTodos(onlineId);
        updateCategoriesViewModel();
        actionMode.finish();
        break;
    }
  }

  public void onSoftDelete(ArrayList itemsToDelete, String itemType) {
    switch (itemType) {
      case "list":
        ArrayList<com.rolandvitezhu.todocloud.data.List> lists = itemsToDelete;
        for (com.rolandvitezhu.todocloud.data.List list:lists) {
          dbLoader.softDeleteListAndRelatedTodos(list.getListOnlineId());
        }
        updateListsViewModel();
        actionMode.finish();
        break;
      case "listInCategory":
        ArrayList<com.rolandvitezhu.todocloud.data.List> listsInCategory = itemsToDelete;
        for (com.rolandvitezhu.todocloud.data.List list:listsInCategory) {
          dbLoader.softDeleteListAndRelatedTodos(list.getListOnlineId());
        }
        updateCategoriesViewModel();
        actionMode.finish();
        break;
      case "category":
        ArrayList<Category> categories = itemsToDelete;
        for (Category category:categories) {
          dbLoader.softDeleteCategoryAndListsAndTodos(category.getCategoryOnlineId());
        }
        updateCategoriesViewModel();
        actionMode.finish();
        break;
    }
  }

  @Override
  public void onFinishSyncListData() {
    updateListsViewModel();
    updateCategoriesViewModel();
  }

  @Override
  public void onFinishSyncCategoryData() {
    updateCategoriesViewModel();
  }

  @Override
  public void onFinishSyncData() {
    try {
      swipeRefreshLayout.setRefreshing(false);
    } catch (NullPointerException e) {
      // SwipeRefreshLayout doesn't already exists.
    }
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
    try {
      Snackbar snackbar = Snackbar.make(
          coordinatorLayout,
          R.string.all_failedtoconnect,
          Snackbar.LENGTH_LONG
      );
      AppController.showWhiteTextSnackbar(snackbar);
    } catch (NullPointerException e) {
      // Snackbar or coordinatorLayout doesn't exists already.
    }
  }

  private void showAnErrorOccurredError() {
    try {
      Snackbar snackbar = Snackbar.make(
          coordinatorLayout,
          R.string.all_anerroroccurred,
          Snackbar.LENGTH_LONG
      );
      AppController.showWhiteTextSnackbar(snackbar);
    } catch (NullPointerException e) {
      // Snackbar or coordinatorLayout doesn't exists already.
    }
  }

  @OnClick(R.id.main_fab_create_category)
  public void onFabCreateCategoryClick(View view) {
    openCreateCategoryDialogFragment();
    fam.close(true);
  }

  @OnClick(R.id.main_fab_create_list)
  public void onFabCreateListClick(View view) {
    openCreateListDialogFragment();
    fam.close(true);
  }

}

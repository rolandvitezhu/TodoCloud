package com.example.todocloud.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.view.ActionMode;
import android.util.Log;
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

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.example.todocloud.R;
import com.example.todocloud.adapter.CategoryAdapter;
import com.example.todocloud.adapter.ListAdapter;
import com.example.todocloud.adapter.PredefinedListAdapter;
import com.example.todocloud.app.AppConfig;
import com.example.todocloud.app.AppController;
import com.example.todocloud.customcomponent.ExpandableHeightExpandableListView;
import com.example.todocloud.customcomponent.ExpandableHeightListView;
import com.example.todocloud.data.Category;
import com.example.todocloud.data.PredefinedListItem;
import com.example.todocloud.data.Todo;
import com.example.todocloud.datastorage.DbConstants;
import com.example.todocloud.datastorage.DbLoader;
import com.example.todocloud.datastorage.asynctask.UpdateAdapterTask;
import com.example.todocloud.datasynchronizer.TodoDataSynchronizer;
import com.example.todocloud.helper.OnlineIdGenerator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainListFragment extends ListFragment implements
    CategoryCreateFragment.ICategoryCreateFragment, CategoryModifyFragment.ICategoryModifyFragment,
    ListCreateFragment.IListCreateFragment, ListModifyFragment.IListModifyFragment,
    ListInCategoryCreateFragment.IListInCategoryCreateFragment,
    ListMoveFragment.IListMoveFragment, SwipeRefreshLayout.OnRefreshListener,
    ConfirmDeleteDialogFragment.IConfirmDeleteDialogFragment, LogoutFragment.ILogoutFragment,
    TodoDataSynchronizer.OnSyncTodoDataListener {

  private static final String TAG = MainListFragment.class.getSimpleName();

  private DbLoader dbLoader;

  private PredefinedListAdapter predefinedListAdapter;
  private CategoryAdapter categoryAdapter;
  private ListAdapter listAdapter;

  private TodoDataSynchronizer todoDataSynchronizer;

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

  private void updateTodosInLocalDatabase(ArrayList<Todo> todos) {
    for (Todo todo : todos) {
      boolean exists = dbLoader.isTodoExists(todo.getTodoOnlineId());
      if (!exists) {
        dbLoader.createTodo(todo);
      } else {
        dbLoader.updateTodo(todo);
      }
    }
  }

  private void updateListsInLocalDatabase(ArrayList<com.example.todocloud.data.List> lists) {
    for (com.example.todocloud.data.List list : lists) {
      boolean exists = dbLoader.isListExists(list.getListOnlineId());
      if (!exists) {
        dbLoader.createList(list);
      } else {
        dbLoader.updateList(list);
      }
    }
  }

  private void updateCategoriesInLocalDatabase(ArrayList<Category> categories) {
    for (Category category : categories) {
      boolean exists = dbLoader.isCategoryExists(category.getCategoryOnlineId());
      if (!exists) {
        dbLoader.createCategory(category);
      } else {
        dbLoader.updateCategory(category);
      }
    }
  }

  @NonNull
  private String prepareGetTodosUrl() {
    int end = AppConfig.URL_GET_TODOS.lastIndexOf(":");
    return AppConfig.URL_GET_TODOS.substring(0, end)
        + dbLoader.getTodoRowVersion();
  }

  private void getLists() {
    String tag_string_request = "request_get_lists";

    String url = prepareGetListsUrl();
    StringRequest getListsRequest = new StringRequest(
        Request.Method.GET,
        url,
        new Response.Listener<String>() {

          @Override
          public void onResponse(String response) {
            Log.d(TAG, "Get Lists Response: " + response);
            try {
              JSONObject jsonResponse = new JSONObject(response);
              boolean error = jsonResponse.getBoolean("error");

              if (!error) {
                ArrayList<com.example.todocloud.data.List> lists = getLists(jsonResponse);
                if (!lists.isEmpty()) {
                  updateListsInLocalDatabase(lists);
                }

                getCategories();

              } else {
                String message = jsonResponse.getString("message");
                Log.d(TAG, "Error Message: " + message);
              }
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }

          @NonNull
          private ArrayList<com.example.todocloud.data.List> getLists(JSONObject jsonResponse) throws JSONException {
            JSONArray jsonLists = jsonResponse.getJSONArray("lists");
            ArrayList<com.example.todocloud.data.List> lists = new ArrayList<>();

            for (int i = 0; i < jsonLists.length(); i++) {
              JSONObject jsonList = jsonLists.getJSONObject(i);
              com.example.todocloud.data.List list =
                  new com.example.todocloud.data.List(jsonList);
              lists.add(list);
            }
            return lists;
          }

        },
        new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError error) {
            String errorMessage = error.getMessage();
            Log.e(TAG, "Get Lists Error: " + errorMessage);
            if (errorMessage != null) {
              showErrorMessage(errorMessage);
            }
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

        }
    ) {

      @Override
      public Map<String, String> getHeaders() throws AuthFailureError {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("authorization", dbLoader.getApiKey());
        return headers;
      }

    };

    AppController.getInstance().addToRequestQueue(getListsRequest, tag_string_request);
  }

  @NonNull
  private String prepareGetListsUrl() {
    int end = AppConfig.URL_GET_LISTS.lastIndexOf(":");
    return AppConfig.URL_GET_LISTS.substring(0, end)
        + dbLoader.getListRowVersion();
  }

  private void getCategories() {
    String tag_string_request = "request_get_categories";

    String url = prepareGetCategoriesUrl();
    StringRequest getCategoriesRequest = new StringRequest(
        Request.Method.GET,
        url,
        new Response.Listener<String>() {

          @Override
          public void onResponse(String response) {
            Log.d(TAG, "Get Categories Response: " + response);
            try {
              JSONObject jsonResponse = new JSONObject(response);
              boolean error = jsonResponse.getBoolean("error");

              if (!error) {
                ArrayList<Category> categories = getCategories(jsonResponse);
                if (!categories.isEmpty()) {
                  updateCategoriesInLocalDatabase(categories);
                }

                updateTodos();
              } else {
                String message = jsonResponse.getString("message");
                Log.d(TAG, "Error Message: " + message);
              }
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }

          @NonNull
          private ArrayList<Category> getCategories(JSONObject jsonResponse) throws JSONException {
            JSONArray jsonCategories = jsonResponse.getJSONArray("categories");
            ArrayList<Category> categories = new ArrayList<>();

            for (int i = 0; i < jsonCategories.length(); i++) {
              JSONObject jsonCategory = jsonCategories.getJSONObject(i);
              Category category = new Category(jsonCategory);
              categories.add(category);
            }
            return categories;
          }

        },
        new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError error) {
            String errorMessage = error.getMessage();
            Log.e(TAG, "Get Categories Error: " + errorMessage);
            if (errorMessage != null) {
              showErrorMessage(errorMessage);
            }
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

        }
    ) {

      @Override
      public Map<String, String> getHeaders() throws AuthFailureError {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("authorization", dbLoader.getApiKey());
        return headers;
      }

    };

    AppController.getInstance().addToRequestQueue(getCategoriesRequest, tag_string_request);
  }

  @NonNull
  private String prepareGetCategoriesUrl() {
    int end = AppConfig.URL_GET_CATEGORIES.lastIndexOf(":");
    return AppConfig.URL_GET_CATEGORIES.substring(0, end) +
        dbLoader.getCategoryRowVersion();
  }

  private void updateTodos() {
    ArrayList<Todo> todosToUpdate = dbLoader.getTodosToUpdate();

    if (!todosToUpdate.isEmpty()) {
      String tag_json_object_request = "request_update_todo";
      for (final Todo todoToUpdate : todosToUpdate) {
        JSONObject jsonRequest = new JSONObject();
        try {
          putTodoData(todoToUpdate, jsonRequest);
        } catch (JSONException e) {
          e.printStackTrace();
        }

        JsonObjectRequest updateTodosRequest = new JsonObjectRequest(
            JsonObjectRequest.Method.PUT,
            AppConfig.URL_UPDATE_TODO,
            jsonRequest,
            new Response.Listener<JSONObject>() {

              @Override
              public void onResponse(JSONObject response) {
                Log.d(TAG, "Update Todo Response: " + response);
                try {
                  boolean error = response.getBoolean("error");

                  if (!error) {
                    makeTodoUpToDate(response);
                  } else {
                    String message = response.getString("message");
                    Log.d(TAG, "Error Message: " + message);
                  }
                } catch (JSONException e) {
                  e.printStackTrace();
                }
              }

              private void makeTodoUpToDate(JSONObject response) throws JSONException {
                todoToUpdate.setRowVersion(response.getInt("row_version"));
                todoToUpdate.setDirty(false);
                dbLoader.updateTodo(todoToUpdate);
              }

            },
            new Response.ErrorListener() {

              @Override
              public void onErrorResponse(VolleyError error) {
                String errorMessage = error.getMessage();
                Log.e(TAG, "Update Todo Error: " + errorMessage);
                if (errorMessage != null) {
                  showErrorMessage(errorMessage);
                }
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

            }
        ) {

          @Override
          public Map<String, String> getHeaders() throws AuthFailureError {
            HashMap<String, String> headers = new HashMap<>();
            headers.put("authorization", dbLoader.getApiKey());
            return headers;
          }

        };

        AppController.getInstance().addToRequestQueue(updateTodosRequest, tag_json_object_request);
      }
    }
    updateLists();
  }

  private void putTodoData(Todo todoData, JSONObject jsonRequest) throws JSONException {
    jsonRequest.put("todo_online_id", todoData.getTodoOnlineId().trim());
    if (todoData.getListOnlineId() != null) {
      jsonRequest.put("list_online_id", todoData.getListOnlineId().trim());
    } else {
      jsonRequest.put("list_online_id", "");
    }
    jsonRequest.put("title", todoData.getTitle().trim());
    jsonRequest.put("priority", todoData.isPriority() ? 1 : 0);
    jsonRequest.put("due_date", todoData.getDueDate().trim());
    if (todoData.getReminderDateTime() != null) {
      jsonRequest.put("reminder_datetime", todoData.getReminderDateTime().trim());
    } else {
      jsonRequest.put("reminder_datetime", "");
    }
    if (todoData.getDescription() != null) {
      jsonRequest.put("description", todoData.getDescription().trim());
    } else {
      jsonRequest.put("description", "");
    }
    jsonRequest.put("completed", todoData.isCompleted() ? 1 : 0);
    jsonRequest.put("deleted", todoData.getDeleted() ? 1 : 0);
  }

  private void updateLists() {
    ArrayList<com.example.todocloud.data.List> listsToUpdate = dbLoader.getListsToUpdate();

    if (!listsToUpdate.isEmpty()) {
      String tag_json_object_request = "request_update_list";
      for (final com.example.todocloud.data.List listToUpdate : listsToUpdate) {
        JSONObject jsonRequest = new JSONObject();
        try {
          putListData(listToUpdate, jsonRequest);
        } catch (JSONException e) {
          e.printStackTrace();
        }

        JsonObjectRequest updateListsRequest = new JsonObjectRequest(
            JsonObjectRequest.Method.PUT,
            AppConfig.URL_UPDATE_LIST,
            jsonRequest,
            new Response.Listener<JSONObject>() {

              @Override
              public void onResponse(JSONObject response) {
                Log.d(TAG, "Update List Response: " + response);
                try {
                  boolean error = response.getBoolean("error");

                  if (!error) {
                    makeListUpToDate(response);
                  } else {
                    String message = response.getString("message");
                    Log.d(TAG, "Error Message: " + message);
                  }

                } catch (JSONException e) {
                  e.printStackTrace();
                }
              }

              private void makeListUpToDate(JSONObject response) throws JSONException {
                listToUpdate.setRowVersion(response.getInt("row_version"));
                listToUpdate.setDirty(false);
                dbLoader.updateList(listToUpdate);
              }

            },
            new Response.ErrorListener() {

              @Override
              public void onErrorResponse(VolleyError error) {
                String errorMessage = error.getMessage();
                Log.e(TAG, "Update List Error: " + errorMessage);
                if (errorMessage != null) {
                  showErrorMessage(errorMessage);
                }
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

            }
        ) {

          @Override
          public Map<String, String> getHeaders() throws AuthFailureError {
            HashMap<String, String> headers = new HashMap<>();
            headers.put("authorization", dbLoader.getApiKey());
            return headers;
          }

        };

        AppController.getInstance().addToRequestQueue(updateListsRequest, tag_json_object_request);
      }
    }
    updateCategories();
  }

  private void putListData(
      com.example.todocloud.data.List listData,
      JSONObject jsonRequest
  ) throws JSONException {
    jsonRequest.put("list_online_id", listData.getListOnlineId().trim());
    if (listData.getCategoryOnlineId() != null) {
      jsonRequest.put("category_online_id", listData.getCategoryOnlineId().trim());
    } else {
      jsonRequest.put("category_online_id", "");
    }
    jsonRequest.put("title", listData.getTitle().trim());
    jsonRequest.put("deleted", listData.getDeleted() ? 1 : 0);
  }

  private void updateCategories() {
    ArrayList<Category> categoriesToUpdate = dbLoader.getCategoriesToUpdate();

    if (!categoriesToUpdate.isEmpty()) {
      String tag_json_object_request = "request_update_category";
      for (final Category categoryToUpdate : categoriesToUpdate) {
        JSONObject jsonRequest = new JSONObject();
        try {
          putCategoryData(categoryToUpdate, jsonRequest);
        } catch (JSONException e) {
          e.printStackTrace();
        }

        JsonObjectRequest updateCategoriesRequest = new JsonObjectRequest(
            JsonObjectRequest.Method.PUT,
            AppConfig.URL_UPDATE_CATEGORY,
            jsonRequest,
            new Response.Listener<JSONObject>() {

              @Override
              public void onResponse(JSONObject response) {
                Log.d(TAG, "Update Category Response: " + response);
                try {
                  boolean error = response.getBoolean("error");

                  if (!error) {
                    makeCategoryUpToDate(response);
                  } else {
                    String message = response.getString("message");
                    Log.d(TAG, "Error Message: " + message);
                  }

                } catch (JSONException e) {
                  e.printStackTrace();
                }
              }

              private void makeCategoryUpToDate(JSONObject response) throws JSONException {
                categoryToUpdate.setRowVersion(response.getInt("row_version"));
                categoryToUpdate.setDirty(false);
                dbLoader.updateCategory(categoryToUpdate);
              }

            },
            new Response.ErrorListener() {

              @Override
              public void onErrorResponse(VolleyError error) {
                String errorMessage = error.getMessage();
                Log.e(TAG, "Update Category Error: " + errorMessage);
                if (errorMessage != null) {
                  showErrorMessage(errorMessage);
                }
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

            }
        ) {

          @Override
          public Map<String, String> getHeaders() throws AuthFailureError {
            HashMap<String, String> headers = new HashMap<>();
            headers.put("authorization", dbLoader.getApiKey());
            return headers;
          }

        };

        AppController.getInstance().addToRequestQueue(updateCategoriesRequest, tag_json_object_request);
      }
    }
    insertTodos();
  }

  private void putCategoryData(
      Category categoryData,
      JSONObject jsonRequest
  ) throws JSONException {
    jsonRequest.put("category_online_id", categoryData.getCategoryOnlineId().trim());
    jsonRequest.put("title", categoryData.getTitle().trim());
    jsonRequest.put("deleted", categoryData.getDeleted() ? 1 : 0);
  }

  private void insertTodos() {
    ArrayList<Todo> todosToInsert = dbLoader.getTodosToInsert();

    if (!todosToInsert.isEmpty()) {
      String tag_json_object_request = "request_insert_todo";
      for (final Todo todoToInsert : todosToInsert) {

        JSONObject jsonRequest = new JSONObject();
        try {
          putTodoData(todoToInsert, jsonRequest);
        } catch (JSONException e) {
          e.printStackTrace();
        }

        JsonObjectRequest insertTodosRequest = new JsonObjectRequest(
            JsonObjectRequest.Method.POST,
            AppConfig.URL_INSERT_TODO,
            jsonRequest,
            new Response.Listener<JSONObject>() {

              @Override
              public void onResponse(JSONObject response) {
                Log.d(TAG, "Insert Todo Response: " + response);
                try {
                  boolean error = response.getBoolean("error");

                  if (!error) {
                    makeTodoUpToDate(response);
                  } else {
                    String message = response.getString("message");
                    Log.d(TAG, "Error Message: " + message);
                  }

                } catch (JSONException e) {
                  e.printStackTrace();
                }
              }

              private void makeTodoUpToDate(JSONObject response) throws JSONException {
                todoToInsert.setRowVersion(response.getInt("row_version"));
                todoToInsert.setDirty(false);
                dbLoader.updateTodo(todoToInsert);
              }

            },
            new Response.ErrorListener() {

              @Override
              public void onErrorResponse(VolleyError error) {
                String errorMessage = error.getMessage();
                Log.e(TAG, "Insert Todo Error: " + errorMessage);
                if (errorMessage != null) {
                  showErrorMessage(errorMessage);
                }
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

            }
        ) {

          @Override
          public Map<String, String> getHeaders() throws AuthFailureError {
            HashMap<String, String> headers = new HashMap<>();
            headers.put("authorization", dbLoader.getApiKey());
            return headers;
          }

        };

        AppController.getInstance().addToRequestQueue(insertTodosRequest, tag_json_object_request);
      }
    }
    insertLists();
  }

  private void insertLists() {
    ArrayList<com.example.todocloud.data.List> listsToInsert = dbLoader.getListsToInsert();

    if (!listsToInsert.isEmpty()) {
      String tag_json_object_request = "request_insert_list";
      int requestsCount = listsToInsert.size();
      int currentRequest = 1;
      boolean lastRequestProcessed = false;
      for (final com.example.todocloud.data.List listToInsert : listsToInsert) {
        if (currentRequest++ == requestsCount) {
          lastRequestProcessed = true;
        }

        JSONObject jsonRequest = new JSONObject();
        try {
          putListData(listToInsert, jsonRequest);
        } catch (JSONException e) {
          e.printStackTrace();
        }

        final boolean LAST_REQUEST_PROCESSED = lastRequestProcessed;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            JsonObjectRequest.Method.POST,
            AppConfig.URL_INSERT_LIST,
            jsonRequest,
            new Response.Listener<JSONObject>() {

              @Override
              public void onResponse(JSONObject response) {
                Log.d(TAG, "Insert List Response: " + response);
                try {
                  boolean error = response.getBoolean("error");

                  if (!error) {
                    makeListUpToDate(response);
                    if (LAST_REQUEST_PROCESSED) {
                      updateListAdapter();
                    }
                  } else {
                    String message = response.getString("message");
                    Log.d(TAG, "Error Message: " + message);
                    if (LAST_REQUEST_PROCESSED) {
                      updateListAdapter();
                    }
                  }
                } catch (JSONException e) {
                  e.printStackTrace();
                  if (LAST_REQUEST_PROCESSED) {
                    updateListAdapter();
                  }
                }
              }

              private void makeListUpToDate(JSONObject response) throws JSONException {
                listToInsert.setRowVersion(response.getInt("row_version"));
                listToInsert.setDirty(false);
                dbLoader.updateList(listToInsert);
              }

            },
            new Response.ErrorListener() {

              @Override
              public void onErrorResponse(VolleyError error) {
                String errorMessage = error.getMessage();
                Log.e(TAG, "Insert List Error: " + errorMessage);
                if (errorMessage != null) {
                  showErrorMessage(errorMessage);
                }
                if (LAST_REQUEST_PROCESSED) {
                  updateListAdapter();
                }
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

            }
        ) {

          @Override
          public Map<String, String> getHeaders() throws AuthFailureError {
            HashMap<String, String> headers = new HashMap<>();
            headers.put("authorization", dbLoader.getApiKey());
            return headers;
          }

        };

        AppController.getInstance().addToRequestQueue(jsonObjectRequest, tag_json_object_request);
      }
    } else {
      updateListAdapter();
    }
    insertCategories();
  }

  private void insertCategories() {
    ArrayList<Category> categoriesToInsert = dbLoader.getCategoriesToInsert();

    if (!categoriesToInsert.isEmpty()) {
      String tag_json_object_request = "request_insert_category";
      int requestsCount = categoriesToInsert.size();
      int currentRequest = 1;
      boolean lastRequestProcessed = false;
      for (final Category categoryToInsert : categoriesToInsert) {
        if (currentRequest++ == requestsCount) {
          lastRequestProcessed = true;
        }

        JSONObject jsonRequest = new JSONObject();
        try {
          putCategoryData(categoryToInsert, jsonRequest);
        } catch (JSONException e) {
          e.printStackTrace();
        }

        final boolean LAST_REQUEST_PROCESSED = lastRequestProcessed;
        JsonObjectRequest insertCategoriesRequest = new JsonObjectRequest(
            JsonObjectRequest.Method.POST,
            AppConfig.URL_INSERT_CATEGORY,
            jsonRequest,
            new Response.Listener<JSONObject>() {

              @Override
              public void onResponse(JSONObject response) {
                Log.d(TAG, "Insert Category Response: " + response);
                try {
                  boolean error = response.getBoolean("error");

                  if (!error) {
                    makeCategoryUpToDate(response);
                    if (LAST_REQUEST_PROCESSED) {
                      updateCategoryAdapter();
                    }
                  } else {
                    String message = response.getString("message");
                    Log.d(TAG, "Error Message: " + message);
                    if (LAST_REQUEST_PROCESSED) {
                      updateCategoryAdapter();
                    }
                  }
                } catch (JSONException e) {
                  e.printStackTrace();
                  if (LAST_REQUEST_PROCESSED) {
                    updateCategoryAdapter();
                  }
                }
              }

              private void makeCategoryUpToDate(JSONObject response) throws JSONException {
                categoryToInsert.setRowVersion(response.getInt("row_version"));
                categoryToInsert.setDirty(false);
                dbLoader.updateCategory(categoryToInsert);
              }

            },
            new Response.ErrorListener() {

              @Override
              public void onErrorResponse(VolleyError error) {
                String errorMessage = error.getMessage();
                Log.e(TAG, "Insert Category Error: " + errorMessage);
                if (errorMessage != null) {
                  showErrorMessage(errorMessage);
                }
                if (LAST_REQUEST_PROCESSED) {
                  updateCategoryAdapter();
                }
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

            }
        ) {

          @Override
          public Map<String, String> getHeaders() throws AuthFailureError {
            HashMap<String, String> headers = new HashMap<>();
            headers.put("authorization", dbLoader.getApiKey());
            return headers;
          }

        };

        AppController.getInstance().addToRequestQueue(insertCategoriesRequest, tag_json_object_request);
      }
    } else {
      updateCategoryAdapter();
    }
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
  public void onGetTodos(ArrayList<Todo> todos) {
    if (!todos.isEmpty()) {
      updateTodosInLocalDatabase(todos);
    }
    getLists();
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

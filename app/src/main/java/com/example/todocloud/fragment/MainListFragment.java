package com.example.todocloud.fragment;

import android.content.Context;
import android.os.Bundle;
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
    DeleteDialogFragment.IDeleteFragment, LogoutFragment.ILogoutFragment {

  private static final String TAG = MainListFragment.class.getSimpleName();

  private DbLoader dbLoader;

  private PredefinedListAdapter predefinedListAdapter;
  private CategoryAdapter categoryAdapter;
  private ListAdapter listAdapter;

  private SwipeRefreshLayout swipeRefreshLayout;
  private CoordinatorLayout coordinatorLayout;
  private ScrollView scrollView;

  private IMainListFragment listener;

  private ExpandableHeightExpandableListView expandableListView;
  private ExpandableHeightListView listView;

  private ActionMode actionMode;
  private boolean actionModeStartedWithELV;

  private ArrayList<Category> categories = new ArrayList<>();
  private ArrayList<com.example.todocloud.data.List> listsInCategory = new ArrayList<>();
  private ArrayList<com.example.todocloud.data.List> lists = new ArrayList<>();

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
    listener.setNavigationHeader();

    // Adapterek frissítése.
    updatePredefinedListAdapter();
    updateCategoryAdapter();
    updateListAdapter();
    getTodos();
  }

  // Létrehozza a View-t, ami 2 ListView-ból és egy ExpandableListView-ból áll.
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View combinedListView = inflater.inflate(R.layout.main_list, null);
    swipeRefreshLayout = (SwipeRefreshLayout)
        combinedListView.findViewById(R.id.swipe_refresh_layout);
    coordinatorLayout = (CoordinatorLayout) combinedListView.findViewById(R.id.coordinatorLayout);
    scrollView = (ScrollView) combinedListView.findViewById(R.id.scroll_view);

    // Beállítja a PredefinedList ListView-ját.
    final ExpandableHeightListView predefinedListListView =
        (ExpandableHeightListView) combinedListView.findViewById(R.id.lvPredefinedList);
    predefinedListListView.setExpanded(true);
    predefinedListListView.setAdapter(predefinedListAdapter);
    predefinedListListView.setOnItemClickListener(predefinedListItemClicked);

    // Beállítja a kategóriákat, azon belül listákat tartalmazó ExpandableListView-t.
    expandableListView = (ExpandableHeightExpandableListView) combinedListView.findViewById(R.id.explvCategory);
    expandableListView.setExpanded(true);
    expandableListView.setAdapter(categoryAdapter);
    expandableListView.setOnChildClickListener(expLvChildClicked);
    expandableListView.setOnGroupClickListener(expLvGroupClicked);
    expandableListView.setOnCreateContextMenuListener(expLvCategoryContextMenu);

    // Beállítja a kategória nélküli listák ListView-ját (alsó ListView).
    listView = (ExpandableHeightListView) combinedListView.findViewById(R.id.lvList);
    listView.setExpanded(true);
    listView.setAdapter(listAdapter);
    listView.setOnItemClickListener(listItemClicked);
    listView.setOnItemLongClickListener(listItemLongClicked);

    // A SwipeRefreshLayout megfelelő kezelését segíti elő.
    scrollView.getViewTreeObserver().addOnScrollChangedListener(
        new ViewTreeObserver.OnScrollChangedListener() {

          @Override
          public void onScrollChanged() {
            int scrollY = scrollView.getScrollY();
            if (scrollY == 0 && !AppController.isActionModeEnabled())
              swipeRefreshLayout.setEnabled(true);
            else {
              swipeRefreshLayout.setEnabled(false);
            }
          }

    });

    swipeRefreshLayout.setOnRefreshListener(this);
    return combinedListView;
  }

  private ActionMode.Callback callback =
      new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
          AppController.setActionModeEnabled(true);
          actionMode = mode;

          // A lentebb taglalt hibakezelésre csak akkor van szükség, ha az ActionMode-ot
          // ExpandableListView-ról indították.
          if (actionModeStartedWithELV) {
            // ActionMode indítása következtében kijelölődik az az elem, amin kiváltódott az
            // ActionMode indításához szükséges ContextClick. Ilyenkor a kijelzőt elengedve kivál-
            // tódhat (az esetek többségében ki is váltódik) egy nem kívánatos Click esemény, ezt
            // kerüli el az alábbi OnTouchListener.
            expandableListView.setOnTouchListener(new View.OnTouchListener() {

              @Override
              public boolean onTouch(View v, MotionEvent event) {
                // Amíg be nem következik a nem kívánatos Click esemény keltéséhez szükséges
                // ACTION_UP, vagy nem keltődik az azt ellehetetlenítő ACTION_CANCEL, addig ezen
                // onTouch esemény true visszatérési értékének köszönhetően nem keltődhet új Click
                // esemény.
                if (event.getAction() == MotionEvent.ACTION_UP ||
                    event.getAction() == MotionEvent.ACTION_CANCEL)
                  expandableListView.setOnTouchListener(null);
                return true;
              }

            });
            actionModeStartedWithELV = false;
          }

          return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

          int checkedItemCount = listView.getCheckedItemCount() +
              expandableListView.getCheckedItemCount();
          actionMode.setTitle(
              checkedItemCount + " " + getString(R.string.selected));

          // Menu beállítása.
          if (categories.size() == 1 && listsInCategory.size() == 0 && lists.size() == 0) {
            // Group.
            menu.clear();
            mode.getMenuInflater().inflate(R.menu.group, menu);
          } else if (categories.size() == 0 && listsInCategory.size() == 1 && lists.size() == 0) {
            // Child.
            menu.clear();
            mode.getMenuInflater().inflate(R.menu.child, menu);
          } else if (categories.size() == 0 && listsInCategory.size() == 0 && lists.size() == 1) {
            // Item.
            menu.clear();
            mode.getMenuInflater().inflate(R.menu.item, menu);
          } else if (categories.size() > 1 && listsInCategory.size() == 0 && lists.size() == 0) {
            // Many Group.
            menu.clear();
            mode.getMenuInflater().inflate(R.menu.many_group, menu);
          } else if (categories.size() == 0 && listsInCategory.size() > 1 && lists.size() == 0) {
            // Many Child.
            menu.clear();
            mode.getMenuInflater().inflate(R.menu.many_child, menu);
          } else if (categories.size() == 0 && listsInCategory.size() == 0 && lists.size() > 1) {
            // Many Item.
            menu.clear();
            mode.getMenuInflater().inflate(R.menu.many_item, menu);
          } else if (categories.size() > 0 && listsInCategory.size() > 0 && lists.size() == 0) {
            // Group and Child.
            menu.clear();
          } else if (categories.size() > 0 && listsInCategory.size() == 0 && lists.size() > 0) {
            // Group and Item.
            menu.clear();
          } else if (categories.size() == 0 && listsInCategory.size() > 0 && lists.size() > 0) {
            // Child and Item.
            menu.clear();
          } else if (categories.size() > 0 && listsInCategory.size() > 0 && lists.size() > 0) {
            // Group, Child and Item.
            menu.clear();
          }

          return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

          if (categories.size() == 1 && listsInCategory.size() == 0 && lists.size() == 0) {
            // Group.
            switch (item.getItemId()) {
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
          } else if (categories.size() == 0 && listsInCategory.size() == 1 && lists.size() == 0) {
            // Child.
            switch (item.getItemId()) {
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
          } else if (categories.size() == 0 && listsInCategory.size() == 0 && lists.size() == 1) {
            // Item.
            switch (item.getItemId()) {
              case R.id.itemModify:
                modifyList();
                break;
              case R.id.itemDelete:
                deleteList();
                break;
              case R.id.itemMove:
                moveListIntoCategory();
                break;
            }
          } else if (categories.size() > 1 && listsInCategory.size() == 0 && lists.size() == 0) {
            // Many Group.
            if (item.getItemId() == R.id.itemDelete)
              deleteCategories();
          } else if (categories.size() == 0 && listsInCategory.size() > 1 && lists.size() == 0) {
            // Many Child.
            if (item.getItemId() == R.id.itemDelete)
              deleteListsInCategory();
          } else if (categories.size() == 0 && listsInCategory.size() == 0 && lists.size() > 1) {
            // Many Item.
            if (item.getItemId() == R.id.itemDelete)
              deleteLists();
          } else if (categories.size() > 0 && listsInCategory.size() > 0 && lists.size() == 0) {
            // Group and Child.

          } else if (categories.size() > 0 && listsInCategory.size() == 0 && lists.size() > 0) {
            // Group and Item.

          } else if (categories.size() == 0 && listsInCategory.size() > 0 && lists.size() > 0) {
            // Child and Item.

          } else if (categories.size() > 0 && listsInCategory.size() > 0 && lists.size() > 0) {
            // Group, Child and Item.

          }

          return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

          for (int i = 0; i < listAdapter.getCount(); i++) {
            listView.setItemChecked(i, false);
          }

          // A 0. elemtől az utolsó látható elemig kell mennünk. Láthatónak számít az összes Group
          // elem és a nyitott Group-ok Child elemei. Ha a képernyőn mindezektől függetlenül nem
          // látszik az adott elem, az ezen logika szerint láthatónak számít.
          for (int i = 0; i <= expandableListView.getLastVisiblePosition(); i++) {
            expandableListView.setItemChecked(i, false);
          }

          categories.clear();
          listsInCategory.clear();
          lists.clear();

          AppController.setActionModeEnabled(false);
          expandableListView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
          listView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
        }

  };

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.main_options_menu, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    switch (item.getItemId()) {
      case R.id.itemCreateCategory:
        // Kategória létrehozása.
        CategoryCreateFragment categoryCreateFragment = new CategoryCreateFragment();
        categoryCreateFragment.setTargetFragment(this, 0);
        categoryCreateFragment.show(getFragmentManager(), "CategoryCreateFragment");
        break;
      case R.id.itemCreateList:
        // Lista létrehozása.
        ListCreateFragment listCreateFragment = new ListCreateFragment();
        listCreateFragment.setTargetFragment(this, 0);
        listCreateFragment.show(getFragmentManager(), "ListCreateFragment");
        break;
    }

    return super.onOptionsItemSelected(item);
  }

  /**
   * Frissíti a PredefinedListAdapter-t.
   */
  private void updatePredefinedListAdapter() {
    predefinedListAdapter = new PredefinedListAdapter(new ArrayList<PredefinedListItem>());
    UpdateAdapterTask updateAdapterTask = new UpdateAdapterTask(dbLoader, predefinedListAdapter);
    updateAdapterTask.execute();
  }

  /**
   * Frissíti a CategoryAdapter-t.
   */
  private void updateCategoryAdapter() {
    if (categoryAdapter == null) {
      categoryAdapter = new CategoryAdapter(new ArrayList<Category>(),
          new HashMap<Category, List<com.example.todocloud.data.List>>());
    }
    UpdateAdapterTask updateAdapterTask = new UpdateAdapterTask(dbLoader, categoryAdapter);
    updateAdapterTask.execute();
  }

  /**
   * Frissíti a ListAdapter-t.
   */
  private void updateListAdapter() {
    if (listAdapter == null) {
      listAdapter = new ListAdapter(new ArrayList<com.example.todocloud.data.List>());
    }
    UpdateAdapterTask updateAdapterTask = new UpdateAdapterTask(dbLoader, listAdapter);
    updateAdapterTask.execute();
  }

  /**
   * ListInCategoryCreateFragment-et nyit meg.
   */
  private void openListInCategoryCreateFragment() {
    ListInCategoryCreateFragment listInCategoryCreateFragment = new ListInCategoryCreateFragment();
    listInCategoryCreateFragment.setTargetFragment(this, 0);
    String categoryOnlineId = categories.get(0).getCategoryOnlineId();
    Bundle bundle = new Bundle();
    bundle.putString("categoryOnlineId", categoryOnlineId);
    listInCategoryCreateFragment.setArguments(bundle);
    listInCategoryCreateFragment.show(getFragmentManager(), "ListInCategoryCreateFragment");
  }

  /**
   * ListModifyFragment-et nyit meg.
   */
  private void modifyListInCategory() {
    com.example.todocloud.data.List list = listsInCategory.get(0);
    ListModifyFragment listModifyFragment = new ListModifyFragment();
    listModifyFragment.setTargetFragment(this, 0);
    Bundle bundle = new Bundle();
    bundle.putParcelable("list", list);
    bundle.putBoolean("isInCategory", true);
    listModifyFragment.setArguments(bundle);
    listModifyFragment.show(getFragmentManager(), "ListModifyFragment");
  }

  /**
   * DeleteDialogFragment-et nyit meg, Category-hez rendelt List-t törléséhez.
   */
  private void deleteListInCategory() {
    com.example.todocloud.data.List list = listsInCategory.get(0);
    String onlineId = list.getListOnlineId();
    String title = list.getTitle();
    DeleteDialogFragment deleteDialogFragment = new DeleteDialogFragment();
    deleteDialogFragment.setTargetFragment(this, 0);
    Bundle bundle = new Bundle();
    bundle.putString("type", "listInCategory");
    bundle.putString("title", title);
    bundle.putString("onlineId", onlineId);
    deleteDialogFragment.setArguments(bundle);
    deleteDialogFragment.show(getFragmentManager(), "DeleteDialogFragment");
  }

  /**
   * DeleteDialogFragment-et nyit meg, Category-hez rendelt List-ek törléséhez.
   */
  private void deleteListsInCategory() {
    DeleteDialogFragment deleteDialogFragment = new DeleteDialogFragment();
    deleteDialogFragment.setTargetFragment(this, 0);
    Bundle bundle = new Bundle();
    bundle.putString("type", "listInCategory");
    bundle.putParcelableArrayList("items", listsInCategory);
    deleteDialogFragment.setArguments(bundle);
    deleteDialogFragment.show(getFragmentManager(), "DeleteDialogFragment");
  }

  /**
   * ListMoveFragment-et nyit meg, Category-hez tartozó List áthelyezéséhez.
   */
  private void moveListInCategory() {
    com.example.todocloud.data.List list = listsInCategory.get(0);
    Category category = dbLoader.getCategoryByCategoryOnlineId(list.getCategoryOnlineId());
    ListMoveFragment listMoveFragment = new ListMoveFragment();
    listMoveFragment.setTargetFragment(this, 0);
    Bundle bundle = new Bundle();
    bundle.putParcelable("category", category);
    bundle.putParcelable("list", list);
    listMoveFragment.setArguments(bundle);
    listMoveFragment.show(getFragmentManager(), "ListMoveFragment");
  }

  /**
   * ListMoveFragment-et nyit meg, Category-hoz nem tartozó List áthelyezéséhez.
   */
  private void moveListIntoCategory() {
    Category category = new Category("Kategórián kívül");
    com.example.todocloud.data.List list = lists.get(0);
    ListMoveFragment listMoveFragment = new ListMoveFragment();
    listMoveFragment.setTargetFragment(this, 0);
    Bundle bundle = new Bundle();
    bundle.putParcelable("category", category);
    bundle.putParcelable("list", list);
    listMoveFragment.setArguments(bundle);
    listMoveFragment.show(getFragmentManager(), "ListMoveFragment");
  }

  /**
   * DeleteDialogFragment-et nyit meg, Category törléséhez.
   */
  private void deleteCategory() {
    Category category = categories.get(0);
    String onlineId = category.getCategoryOnlineId();
    String title = category.getTitle();
    DeleteDialogFragment deleteDialogFragment = new DeleteDialogFragment();
    deleteDialogFragment.setTargetFragment(this, 0);
    Bundle bundle = new Bundle();
    bundle.putString("type", "category");
    bundle.putString("title", title);
    bundle.putString("onlineId", onlineId);
    deleteDialogFragment.setArguments(bundle);
    deleteDialogFragment.show(getFragmentManager(), "DeleteDialogFragment");
  }

  /**
   * DeleteDialogFragment-et nyit meg, a kijelölt Category-k törléséhez.
   */
  private void deleteCategories() {
    DeleteDialogFragment deleteDialogFragment = new DeleteDialogFragment();
    deleteDialogFragment.setTargetFragment(this, 0);
    Bundle bundle = new Bundle();
    bundle.putString("type", "category");
    bundle.putParcelableArrayList("items", categories);
    deleteDialogFragment.setArguments(bundle);
    deleteDialogFragment.show(getFragmentManager(), "DeleteDialogFragment");
  }

  /**
   * DeleteDialogFragment-et nyit meg, a kijelölt List törléséhez.
   */
  private void deleteList() {
    com.example.todocloud.data.List list = lists.get(0);
    String onlineId = list.getListOnlineId();
    String title = list.getTitle();
    DeleteDialogFragment deleteDialogFragment = new DeleteDialogFragment();
    deleteDialogFragment.setTargetFragment(this, 0);
    Bundle bundle = new Bundle();
    bundle.putString("type", "list");
    bundle.putString("title", title);
    bundle.putString("onlineId", onlineId);
    deleteDialogFragment.setArguments(bundle);
    deleteDialogFragment.show(getFragmentManager(), "DeleteDialogFragment");
  }

  /**
   * DeleteDialogFragment-et nyit meg, a kijelölt List-ek törléséhez.
   */
  private void deleteLists() {
    DeleteDialogFragment deleteDialogFragment = new DeleteDialogFragment();
    deleteDialogFragment.setTargetFragment(this, 0);
    Bundle bundle = new Bundle();
    bundle.putString("type", "list");
    bundle.putParcelableArrayList("items", lists);
    deleteDialogFragment.setArguments(bundle);
    deleteDialogFragment.show(getFragmentManager(), "DeleteDialogFragment");
  }

  /**
   * CategoryModifyFragment-et nyit meg.
   */
  private void modifyCategory() {
    Category category = categories.get(0);
    CategoryModifyFragment categoryModifyFragment = new CategoryModifyFragment();
    categoryModifyFragment.setTargetFragment(this, 0);
    Bundle bundle = new Bundle();
    bundle.putParcelable("category", category);
    categoryModifyFragment.setArguments(bundle);
    categoryModifyFragment.show(getFragmentManager(), "CategoryModifyFragment");
  }

  /**
   * ListModifyFragment-et nyit meg.
   */
  private void modifyList() {
    com.example.todocloud.data.List list = lists.get(0);
    ListModifyFragment listModifyFragment = new ListModifyFragment();
    listModifyFragment.setTargetFragment(this, 0);
    Bundle bundle = new Bundle();
    bundle.putParcelable("list", list);
    listModifyFragment.setArguments(bundle);
    listModifyFragment.show(getFragmentManager(), "ListModifyFragment");
  }

  /**
   * Frissíti a helyi adatbázist a megadott Todo-kkal.
   * @param todos A megadott Todo-k.
   */
  private void updateTodosInLocalDb(ArrayList<Todo> todos) {
    for (Todo todo : todos) {
      boolean exists = dbLoader.isTodoExists(todo.getTodoOnlineId());
      if (!exists) {
        dbLoader.createTodo(todo);
      } else {
        dbLoader.updateTodo(todo);
      }
    }
  }

  /**
   * Frissíti a helyi adatbázist a megadott List-ekkel.
   * @param lists A megadott List-ek.
   */
  private void updateListsInLocalDb(ArrayList<com.example.todocloud.data.List> lists) {
    for (com.example.todocloud.data.List list : lists) {
      boolean exists = dbLoader.isListExists(list.getListOnlineId());
      if (!exists) {
        dbLoader.createList(list);
      } else {
        dbLoader.updateList(list);
      }
    }
  }

  /**
   * Frissíti a helyi adatbázist a megadott Category-kkel.
   * @param categories A megadott Category-k.
   */
  private void updateCategoriesInLocalDb(ArrayList<Category> categories) {
    for (Category category : categories) {
      boolean exists = dbLoader.isCategoryExists(category.getCategoryOnlineId());
      if (!exists) {
        dbLoader.createCategory(category);
      } else {
        dbLoader.updateCategory(category);
      }
    }
  }

  /**
   * A kliens aktuális sorverziója alapján lekéri a szerverről a frissítendő sorokat. Ezek között
   * szerepel olyan, ami még nem létezik a kliens adatbázisában és olyan, ami létezik, de módosí-
   * tandó. A lekért sorokat a kliens az adatbázisba beszúrja/frissíti, majd frissíti az adaptert.
   */
  private void getTodos() {

    String tag_string_request = "request_get_todos";

    // URL összeállítása.
    int end = AppConfig.URL_GET_TODOS.lastIndexOf(":");
    String URL = AppConfig.URL_GET_TODOS.substring(0, end) +
        dbLoader.getTodoRowVersion();

    StringRequest stringRequest = new StringRequest(Request.Method.GET, URL,
        new Response.Listener<String>() {

          @Override
          public void onResponse(String response) {
            Log.d(TAG, "Get Todos Response: " + response);
            try {
              JSONObject jsonObject = new JSONObject(response);
              boolean error = jsonObject.getBoolean("error");

              if (!error) {

                JSONArray jaTodos = jsonObject.getJSONArray("todos");
                ArrayList<Todo> todos = new ArrayList<>();

                for (int i = 0; i < jaTodos.length(); i++) {
                  JSONObject joTodo = jaTodos.getJSONObject(i);
                  Todo todo = new Todo();
                  todo.setTodoOnlineId(joTodo.getString("todo_online_id"));
                  todo.setUserOnlineId(joTodo.getString("user_online_id"));
                  todo.setListOnlineId(joTodo.getString("list_online_id"));
                  todo.setTitle(joTodo.getString("title"));
                  todo.setPriority(joTodo.getInt("priority") != 0);
                  todo.setDueDate(joTodo.getString("due_date"));
                  todo.setReminderDateTime(joTodo.getString("reminder_datetime"));
                  todo.setDescription(joTodo.getString("description"));
                  todo.setCompleted(joTodo.getInt("completed") != 0);
                  todo.setRowVersion(joTodo.getInt("row_version"));
                  todo.setDeleted(joTodo.getInt("deleted") != 0);
                  todo.setDirty(false);
                  todos.add(todo);
                }

                if (!todos.isEmpty()) {
                  updateTodosInLocalDb(todos);
                }

                getLists();

              } else {
                Log.d(TAG, "Error Message: " + jsonObject.getString("message"));
              }
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }

        }, new Response.ErrorListener() {

      @Override
      public void onErrorResponse(VolleyError error) {
        String message = error.getMessage();
        Log.e(TAG, "Get Todos Error: " + message);
        if (message != null) {
          if (message.contains("failed to connect")) {
            // Sikertelen kapcsolódás.
            if (getView() != null)
              // Hotswap/Coldswap esetén olyan View-n is meghívódhat a Snackbar, amelyik nem
              // látható.
              AppController.setStyleAndShowSnackbar(
                  Snackbar.make(coordinatorLayout,
                      R.string.failed_to_connect, Snackbar.LENGTH_LONG)
              );
          }
        }
      }

    }) {

      @Override
      public Map<String, String> getHeaders() throws AuthFailureError {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("authorization", dbLoader.getApiKey());
        return headers;
      }

    };

    AppController.getInstance().addToRequestQueue(stringRequest, tag_string_request);

  };

  /**
   * A kliens aktuális sorverziója alapján lekéri a szerverről a frissítendő sorokat. Ezek között
   * szerepel olyan, ami még nem létezik a kliens adatbázisában és olyan, ami létezik, de módosí-
   * tandó. A lekért sorokat a kliens az adatbázisba beszúrja/frissíti, majd frissíti az adaptert.
   */
  private void getLists() {

    String tag_string_request = "request_get_lists";

    // URL összeállítása.
    int end = AppConfig.URL_GET_LISTS.lastIndexOf(":");
    String URL = AppConfig.URL_GET_LISTS.substring(0, end) +
        dbLoader.getListRowVersion();

    StringRequest stringRequest = new StringRequest(Request.Method.GET, URL,
        new Response.Listener<String>() {

          @Override
          public void onResponse(String response) {
            Log.d(TAG, "Get Lists Response: " + response);
            try {
              JSONObject jsonObject = new JSONObject(response);
              boolean error = jsonObject.getBoolean("error");

              if (!error) {

                JSONArray jaLists = jsonObject.getJSONArray("lists");
                ArrayList<com.example.todocloud.data.List> lists = new ArrayList<>();

                for (int i = 0; i < jaLists.length(); i++) {
                  JSONObject joList = jaLists.getJSONObject(i);
                  com.example.todocloud.data.List list = new com.example.todocloud.data.List();
                  list.setListOnlineId(joList.getString("list_online_id"));
                  list.setUserOnlineId(joList.getString("user_online_id"));
                  list.setCategoryOnlineId(joList.getString("category_online_id"));
                  list.setTitle(joList.getString("title"));
                  list.setRowVersion(joList.getInt("row_version"));
                  list.setDeleted(joList.getInt("deleted") != 0);
                  list.setDirty(false);
                  lists.add(list);
                }

                if (!lists.isEmpty()) {
                  updateListsInLocalDb(lists);
                }

                getCategories();

              } else {
                Log.d(TAG, "Error Message: " + jsonObject.getString("message"));
              }
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }

        }, new Response.ErrorListener() {

      @Override
      public void onErrorResponse(VolleyError error) {
        String message = error.getMessage();
        Log.e(TAG, "Get Lists Error: " + message);
        if (message != null) {
          if (message.contains("failed to connect")) {
            // Sikertelen kapcsolódás.
            if (getView() != null)
              // Hotswap/Coldswap esetén olyan View-n is meghívódhat a Snackbar, amelyik nem
              // látható.
              AppController.setStyleAndShowSnackbar(
                  Snackbar.make(coordinatorLayout,
                      R.string.failed_to_connect, Snackbar.LENGTH_LONG)
              );
          }
        }
      }

    }) {

      @Override
      public Map<String, String> getHeaders() throws AuthFailureError {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("authorization", dbLoader.getApiKey());
        return headers;
      }

    };

    AppController.getInstance().addToRequestQueue(stringRequest, tag_string_request);

  }

  /**
   * A kliens aktuális sorverziója alapján lekéri a szerverről a frissítendő sorokat. Ezek között
   * szerepel olyan, ami még nem létezik a kliens adatbázisában és olyan, ami létezik, de módosí-
   * tandó. A lekért sorokat a kliens az adatbázisba beszúrja/frissíti, majd frissíti az adaptert.
   */
  private void getCategories() {

    String tag_string_request = "request_get_categories";

    // URL összeállítása.
    int end = AppConfig.URL_GET_CATEGORIES.lastIndexOf(":");
    String URL = AppConfig.URL_GET_CATEGORIES.substring(0, end) +
        dbLoader.getCategoryRowVersion();

    StringRequest stringRequest = new StringRequest(Request.Method.GET, URL,
        new Response.Listener<String>() {

          @Override
          public void onResponse(String response) {
            Log.d(TAG, "Get Categories Response: " + response);
            try {
              JSONObject jsonObject = new JSONObject(response);
              boolean error = jsonObject.getBoolean("error");

              if (!error) {

                JSONArray jaCategories = jsonObject.getJSONArray("categories");
                ArrayList<Category> categories = new ArrayList<>();

                for (int i = 0; i < jaCategories.length(); i++) {
                  JSONObject joCategory = jaCategories.getJSONObject(i);
                  Category category = new Category();
                  category.setCategoryOnlineId(joCategory.getString("category_online_id"));
                  category.setUserOnlineId(joCategory.getString("user_online_id"));
                  category.setTitle(joCategory.getString("title"));
                  category.setRowVersion(joCategory.getInt("row_version"));
                  category.setDeleted(joCategory.getInt("deleted") != 0);
                  category.setDirty(false);
                  categories.add(category);
                }

                if (!categories.isEmpty()) {
                  updateCategoriesInLocalDb(categories);
                }

                updateTodos();

              } else {
                Log.d(TAG, "Error Message: " + jsonObject.getString("message"));
              }
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }

        }, new Response.ErrorListener() {

      @Override
      public void onErrorResponse(VolleyError error) {
        String message = error.getMessage();
        Log.e(TAG, "Get Categories Error: " + message);
        if (message != null) {
          if (message.contains("failed to connect")) {
            // Sikertelen kapcsolódás.
            if (getView() != null)
              // Hotswap/Coldswap esetén olyan View-n is meghívódhat a Snackbar, amelyik nem
              // látható.
              AppController.setStyleAndShowSnackbar(
                  Snackbar.make(coordinatorLayout,
                      R.string.failed_to_connect, Snackbar.LENGTH_LONG)
              );
          }
        }
      }

    }) {

      @Override
      public Map<String, String> getHeaders() throws AuthFailureError {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("authorization", dbLoader.getApiKey());
        return headers;
      }

    };

    AppController.getInstance().addToRequestQueue(stringRequest, tag_string_request);

  }

  /**
   * Feltölti a szerverre az összes frissítendő sort a helyi adatbázisból.
   */
  private void updateTodos() {

    ArrayList<Todo> todos = dbLoader.getUpdatableTodos();

    if (!todos.isEmpty()) {
      for (final Todo todo : todos) {

        String tag_json_object_request = "request_update_todo";

        JSONObject jsonRequest = new JSONObject();
        try {
          jsonRequest.put("todo_online_id", todo.getTodoOnlineId().trim());
          if (todo.getListOnlineId() != null) {
            jsonRequest.put("list_online_id", todo.getListOnlineId().trim());
          } else {
            jsonRequest.put("list_online_id", "");
          }
          jsonRequest.put("title", todo.getTitle().trim());
          jsonRequest.put("priority", todo.isPriority() ? 1 : 0);
          jsonRequest.put("due_date", todo.getDueDate().trim());
          if (todo.getReminderDateTime() != null) {
            jsonRequest.put("reminder_datetime", todo.getReminderDateTime().trim());
          } else {
            jsonRequest.put("reminder_datetime", "");
          }
          if (todo.getDescription() != null) {
            jsonRequest.put("description", todo.getDescription().trim());
          } else {
            jsonRequest.put("description", "");
          }
          jsonRequest.put("completed", todo.getCompleted() ? 1 : 0);
          jsonRequest.put("deleted", todo.getDeleted() ? 1 : 0);
        } catch (JSONException e) {
          e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(JsonObjectRequest.Method.PUT,
            AppConfig.URL_UPDATE_TODO, jsonRequest, new Response.Listener<JSONObject>() {

          @Override
          public void onResponse(JSONObject response) {
            Log.d(TAG, "Update Todo Response: " + response);

            try {
              boolean error = response.getBoolean("error");

              if (!error) {
                todo.setRowVersion(response.getInt("row_version"));
                todo.setDirty(false);
                dbLoader.updateTodo(todo);
              } else {
                Log.d(TAG, "Error Message: " + response.getString("message"));
              }
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }

        }, new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError error) {
            String message = error.getMessage();
            Log.e(TAG, "Update Todo Error: " + message);
            if (message != null) {
              if (message.contains("failed to connect")) {
                // Sikertelen kapcsolódás.
                if (getView() != null)
                  // Hotswap/Coldswap esetén olyan View-n is meghívódhat a Snackbar, amelyik nem
                  // látható.
                  AppController.setStyleAndShowSnackbar(
                      Snackbar.make(coordinatorLayout,
                          R.string.failed_to_connect, Snackbar.LENGTH_LONG)
                  );
              }
            }
          }

        }) {

          @Override
          public Map<String, String> getHeaders() throws AuthFailureError {
            HashMap<String, String> headers = new HashMap<>();
            headers.put("authorization", dbLoader.getApiKey());
            return headers;
          }

        };

        AppController.getInstance().addToRequestQueue(jsonObjectRequest, tag_json_object_request);

      }
    }
    updateLists();

  }

  /**
   * Feltölti a szerverre az összes frissítendő sort a helyi adatbázisból.
   */
  private void updateLists() {

    ArrayList<com.example.todocloud.data.List> lists = dbLoader.getUpdatableLists();

    if (!lists.isEmpty()) {
      for (final com.example.todocloud.data.List list : lists) {

        String tag_json_object_request = "request_update_list";

        JSONObject jsonRequest = new JSONObject();
        try {
          jsonRequest.put("list_online_id", list.getListOnlineId().trim());
          if (list.getCategoryOnlineId() != null) {
            jsonRequest.put("category_online_id", list.getCategoryOnlineId().trim());
          } else {
            jsonRequest.put("category_online_id", "");
          }
          jsonRequest.put("title", list.getTitle().trim());
          jsonRequest.put("deleted", list.getDeleted() ? 1 : 0);
        } catch (JSONException e) {
          e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(JsonObjectRequest.Method.PUT,
            AppConfig.URL_UPDATE_LIST, jsonRequest, new Response.Listener<JSONObject>() {

          @Override
          public void onResponse(JSONObject response) {
            Log.d(TAG, "Update List Response: " + response);

            try {
              boolean error = response.getBoolean("error");

              if (!error) {
                list.setRowVersion(response.getInt("row_version"));
                list.setDirty(false);
                dbLoader.updateList(list);
              } else {
                Log.d(TAG, "Error Message: " + response.getString("message"));
              }

            } catch (JSONException e) {
              e.printStackTrace();
            }
          }

        }, new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError error) {
            String message = error.getMessage();
            Log.e(TAG, "Update List Error: " + message);
            if (message != null) {
              if (message.contains("failed to connect")) {
                // Sikertelen kapcsolódás.
                if (getView() != null)
                  // Hotswap/Coldswap esetén olyan View-n is meghívódhat a Snackbar, amelyik nem
                  // látható.
                  AppController.setStyleAndShowSnackbar(
                      Snackbar.make(coordinatorLayout,
                          R.string.failed_to_connect, Snackbar.LENGTH_LONG)
                  );
              }
            }
          }

        }) {

          @Override
          public Map<String, String> getHeaders() throws AuthFailureError {
            HashMap<String, String> headers = new HashMap<>();
            headers.put("authorization", dbLoader.getApiKey());
            return headers;
          }

        };

        AppController.getInstance().addToRequestQueue(jsonObjectRequest, tag_json_object_request);

      }
    }
    updateCategories();

  }

  /**
   * Feltölti a szerverre az összes frissítendő sort a helyi adatbázisból.
   */
  private void updateCategories() {

    ArrayList<Category> categories = dbLoader.getUpdatableCategories();

    if (!categories.isEmpty()) {
      for (final Category category : categories) {

        String tag_json_object_request = "request_update_category";

        JSONObject jsonRequest = new JSONObject();
        try {
          jsonRequest.put("category_online_id", category.getCategoryOnlineId().trim());
          jsonRequest.put("title", category.getTitle().trim());
          jsonRequest.put("deleted", category.getDeleted() ? 1 : 0);
        } catch (JSONException e) {
          e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(JsonObjectRequest.Method.PUT,
            AppConfig.URL_UPDATE_CATEGORY, jsonRequest, new Response.Listener<JSONObject>() {

          @Override
          public void onResponse(JSONObject response) {
            Log.d(TAG, "Update Category Response: " + response);

            try {
              boolean error = response.getBoolean("error");

              if (!error) {
                category.setRowVersion(response.getInt("row_version"));
                category.setDirty(false);
                dbLoader.updateCategory(category);
              } else {
                Log.d(TAG, "Error Message: " + response.getString("message"));
              }

            } catch (JSONException e) {
              e.printStackTrace();
            }
          }

        }, new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError error) {
            String message = error.getMessage();
            Log.e(TAG, "Update Category Error: " + message);
            if (message != null) {
              if (message.contains("failed to connect")) {
                // Sikertelen kapcsolódás.
                if (getView() != null)
                  // Hotswap/Coldswap esetén olyan View-n is meghívódhat a Snackbar, amelyik nem
                  // látható.
                  AppController.setStyleAndShowSnackbar(
                      Snackbar.make(coordinatorLayout,
                          R.string.failed_to_connect, Snackbar.LENGTH_LONG)
                  );
              }
            }
          }

        }) {

          @Override
          public Map<String, String> getHeaders() throws AuthFailureError {
            HashMap<String, String> headers = new HashMap<>();
            headers.put("authorization", dbLoader.getApiKey());
            return headers;
          }

        };

        AppController.getInstance().addToRequestQueue(jsonObjectRequest, tag_json_object_request);

      }
    }
    insertTodos();

  }

  /**
   * Feltölti a szerverre az összes beszúrandó sort a helyi adatbázisból.
   */
  private void insertTodos() {

    ArrayList<Todo> todos = dbLoader.getInsertableTodos();

    if (!todos.isEmpty()) {
      int i = 1;
      for (final Todo todo : todos) {
        boolean lastTodo = false;
        if (i++ == todos.size()) {
          // Az utolsó kérés feldolgozását követően frissül az adapter.
          lastTodo = true;
        }

        String tag_json_object_request = "request_insert_todo";

        JSONObject jsonRequest = new JSONObject();
        try {
          jsonRequest.put("todo_online_id", todo.getTodoOnlineId().trim());
          if (todo.getListOnlineId() != null) {
            jsonRequest.put("list_online_id", todo.getListOnlineId().trim());
          } else {
            jsonRequest.put("list_online_id", "");
          }
          jsonRequest.put("title", todo.getTitle().trim());
          jsonRequest.put("priority", todo.isPriority() ? 1 : 0);
          jsonRequest.put("due_date", todo.getDueDate().trim());
          if (todo.getReminderDateTime() != null) {
            jsonRequest.put("reminder_datetime", todo.getReminderDateTime().trim());
          } else {
            jsonRequest.put("reminder_datetime", "");
          }
          if (todo.getDescription() != null) {
            jsonRequest.put("description", todo.getDescription().trim());
          } else {
            jsonRequest.put("description", "");
          }
          jsonRequest.put("completed", todo.getCompleted() ? 1 : 0);
          jsonRequest.put("deleted", todo.getDeleted() ? 1 : 0);
        } catch (JSONException e) {
          e.printStackTrace();
        }

        final boolean finalLastTodo = lastTodo;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(JsonObjectRequest.Method.POST,
            AppConfig.URL_INSERT_TODO, jsonRequest, new Response.Listener<JSONObject>() {

          @Override
          public void onResponse(JSONObject response) {
            Log.d(TAG, "Insert Todo Response: " + response);
            try {
              boolean error = response.getBoolean("error");

              if (!error) {
                todo.setRowVersion(response.getInt("row_version"));
                todo.setDirty(false);
                dbLoader.updateTodo(todo);
                if (finalLastTodo) {
                  // updateTodoAdapter();
                }
              } else {
                Log.d(TAG, "Error Message: " + response.getString("message"));
                if (finalLastTodo) {
                  // updateTodoAdapter();
                }
              }

            } catch (JSONException e) {
              e.printStackTrace();
              if (finalLastTodo) {
                // updateTodoAdapter();
              }
            }
          }

        }, new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError error) {
            String message = error.getMessage();
            Log.e(TAG, "Insert Todo Error: " + message);
            if (message != null) {
              if (message.contains("failed to connect")) {
                // Sikertelen kapcsolódás.
                if (getView() != null)
                  // Hotswap/Coldswap esetén olyan View-n is meghívódhat a Snackbar, amelyik nem
                  // látható.
                  AppController.setStyleAndShowSnackbar(
                      Snackbar.make(coordinatorLayout,
                          R.string.failed_to_connect, Snackbar.LENGTH_LONG)
                  );
              }
            }
            if (finalLastTodo) {
              // updateTodoAdapter();
            }
          }

        }) {

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
      // Ha nem szükséges kérést végrehajtani, akkor frissül az adapter.
      // updateTodoAdapter();
    }
    insertLists();

  }

  /**
   * Feltölti a szerverre az összes beszúrandó sort a helyi adatbázisból.
   */
  private void insertLists() {

    ArrayList<com.example.todocloud.data.List> lists = dbLoader.getInsertableLists();

    if (!lists.isEmpty()) {
      int i = 1;
      for (final com.example.todocloud.data.List list : lists) {
        boolean lastList = false;
        if (i++ == lists.size()) {
          // Az utolsó kérés feldolgozását követően frissül az adapter.
          lastList = true;
        }

        String tag_json_object_request = "request_insert_list";

        JSONObject jsonRequest = new JSONObject();
        try {
          jsonRequest.put("list_online_id", list.getListOnlineId().trim());
          if (list.getCategoryOnlineId() != null) {
            jsonRequest.put("category_online_id", list.getCategoryOnlineId().trim());
          } else {
            jsonRequest.put("category_online_id", "");
          }
          jsonRequest.put("title", list.getTitle().trim());
          jsonRequest.put("deleted", list.getDeleted() ? 1 : 0);
        } catch (JSONException e) {
          e.printStackTrace();
        }

        final boolean finalLastList = lastList;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(JsonObjectRequest.Method.POST,
            AppConfig.URL_INSERT_LIST, jsonRequest, new Response.Listener<JSONObject>() {

          @Override
          public void onResponse(JSONObject response) {
            Log.d(TAG, "Insert List Response: " + response);
            try {
              boolean error = response.getBoolean("error");

              if (!error) {
                list.setRowVersion(response.getInt("row_version"));
                list.setDirty(false);
                dbLoader.updateList(list);
                if (finalLastList) {
                  updateListAdapter();
                }
              } else {
                Log.d(TAG, "Error Message: " + response.getString("message"));
                if (finalLastList) {
                  updateListAdapter();
                }
              }
            } catch (JSONException e) {
              e.printStackTrace();
              if (finalLastList) {
                updateListAdapter();
              }
            }
          }

        }, new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError error) {
            String message = error.getMessage();
            Log.e(TAG, "Insert List Error: " + message);
            if (message != null) {
              if (message.contains("failed to connect")) {
                // Sikertelen kapcsolódás.
                if (getView() != null)
                  // Hotswap/Coldswap esetén olyan View-n is meghívódhat a Snackbar, amelyik nem
                  // látható.
                  AppController.setStyleAndShowSnackbar(
                      Snackbar.make(coordinatorLayout,
                          R.string.failed_to_connect, Snackbar.LENGTH_LONG)
                  );
              }
            }
            if (finalLastList) {
              updateListAdapter();
            }
          }

        }) {

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
      // Ha nem szükséges kérést végrehajtani, akkor frissül az adapter.
      updateListAdapter();
    }
    insertCategories();

  }

  /**
   * Feltölti a szerverre az összes beszúrandó sort a helyi adatbázisból.
   */
  private void insertCategories() {

    ArrayList<Category> categories = dbLoader.getInsertableCategories();

    if (!categories.isEmpty()) {
      int i = 1;
      for (final Category category : categories) {
        boolean lastCategory = false;
        if (i++ == categories.size()) {
          // Az utolsó kérés feldolgozását követően frissül az adapter.
          lastCategory = true;
        }

        String tag_json_object_request = "request_insert_category";

        JSONObject jsonRequest = new JSONObject();
        try {
          jsonRequest.put("category_online_id", category.getCategoryOnlineId().trim());
          jsonRequest.put("title", category.getTitle().trim());
          jsonRequest.put("deleted", category.getDeleted() ? 1 : 0);
        } catch (JSONException e) {
          e.printStackTrace();
        }

        final boolean finalLastCategory = lastCategory;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(JsonObjectRequest.Method.POST,
            AppConfig.URL_INSERT_CATEGORY, jsonRequest, new Response.Listener<JSONObject>() {

          @Override
          public void onResponse(JSONObject response) {
            Log.d(TAG, "Insert Category Response: " + response);
            try {
              boolean error = response.getBoolean("error");

              if (!error) {
                category.setRowVersion(response.getInt("row_version"));
                category.setDirty(false);
                dbLoader.updateCategory(category);
                if (finalLastCategory) {
                  updateCategoryAdapter();
                }
              } else {
                Log.d(TAG, "Error Message: " + response.getString("message"));
                if (finalLastCategory) {
                  updateCategoryAdapter();
                }
              }
            } catch (JSONException e) {
              e.printStackTrace();
              if (finalLastCategory) {
                updateCategoryAdapter();
              }
            }
          }

        }, new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError error) {
            String message = error.getMessage();
            Log.e(TAG, "Insert Category Error: " + message);
            if (message != null) {
              if (message.contains("failed to connect")) {
                // Sikertelen kapcsolódás.
                if (getView() != null)
                  // Hotswap/Coldswap esetén olyan View-n is meghívódhat a Snackbar, amelyik nem
                  // látható.
                  AppController.setStyleAndShowSnackbar(
                      Snackbar.make(coordinatorLayout,
                          R.string.failed_to_connect, Snackbar.LENGTH_LONG)
                  );
              }
            }
            if (finalLastCategory) {
              updateCategoryAdapter();
            }
          }

        }) {

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
      // Ha nem szükséges kérést végrehajtani, akkor frissül az adapter.
      updateCategoryAdapter();
    }

  }

  /**
   * Elvégez minden szinkronizációhoz szükséges műveletet.
   */
  private void sync() {
    // Ha a getTodos nem futott le, akkor ne futtassuk az updateUsers és insertUsers-t sem! Ellen-
    // kező esetben olyan row_version lesz a kliensben, ami nagyobb, mint a kliensre még le nem
    // kért soroké, így azokat nem fogja lekérni.
    getTodos();
    swipeRefreshLayout.setRefreshing(false);
  }

  /**
   * Az előre definiált lista elemén történő kattintás eseménykezelője (többek között tennivaló
   * listákat nyit meg).
   */
  private AdapterView.OnItemClickListener predefinedListItemClicked =
      new AdapterView.OnItemClickListener() {

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      if (!AppController.isActionModeEnabled()) {
        PredefinedListItem predefinedListItem = (PredefinedListItem) parent.getAdapter().getItem(
            position);
        listener.onItemSelected(predefinedListItem);
      }
    }

  };

  /**
   * Az egyéni listák elemein történő kattintás eseménykezelője (tökkek között tennivaló listákat
   * nyit meg).
   */
  private AdapterView.OnItemClickListener listItemClicked = new AdapterView.OnItemClickListener() {

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      if (!AppController.isActionModeEnabled()) {
        // Todo módosítása.
        listener.onItemSelected((com.example.todocloud.data.List) listAdapter.getItem(position));
      } else {
        if (listView.isItemChecked(position)) {
          lists.add((com.example.todocloud.data.List) listView.getItemAtPosition(position));
        } else {
          lists.remove(listView.getItemAtPosition(position));
        }

        // ActionMode-hoz tartozó ActionBar beállítása.
        actionMode.invalidate();

        // Ha az utolsó kiválasztott elemet is kiválasztatlanná tesszük, akkor ActionMode
        // kikapcsolása.
        int checkedItemCount = listView.getCheckedItemCount() +
            expandableListView.getCheckedItemCount();
        if (checkedItemCount == 0) {
          if (actionMode != null)
            actionMode.finish();
        }
      }
    }

  };

  /**
   * A List elemein történő LongClick eseménykezelője. Az ActionMode-ot indítja el, ha szükséges.
   */
  private AdapterView.OnItemLongClickListener listItemLongClicked =
      new AdapterView.OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
          if (!AppController.isActionModeEnabled()) {
            listener.startActionMode(callback);
            listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
            listView.setItemChecked(position, true);
            lists.add((com.example.todocloud.data.List) listView.getItemAtPosition(position));

            expandableListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
            actionMode.invalidate();
          }
          return true;
        }

      };

  /**
   * Kategória lista elemén történő kattintás eseménykezelője (többek között tennivaló listákat
   * nyit meg).
   */
  private ExpandableListView.OnChildClickListener expLvChildClicked =
      new ExpandableListView.OnChildClickListener() {

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                int childPosition, long id) {
      int position = parent.getFlatListPosition(
          ExpandableListView.getPackedPositionForChild(groupPosition, childPosition));
      if (!AppController.isActionModeEnabled()) {
        // List megnyitása.
        listener.onItemSelected((com.example.todocloud.data.List)
            categoryAdapter.getChild(groupPosition, childPosition));
      } else {
        expandableListView.setItemChecked(position, !parent.isItemChecked(position));
        if (parent.isItemChecked(position)) {
          listsInCategory.add((com.example.todocloud.data.List) parent.getItemAtPosition(position));
        } else {
          listsInCategory.remove(parent.getItemAtPosition(position));
        }

        // ActionMode-hoz tartozó ActionBar beállítása.
        actionMode.invalidate();

        // Ha az utolsó kiválasztott elemet is kiválasztatlanná tesszük, akkor
        // ActionMode kikapcsolása.
        int checkedItemCount = listView.getCheckedItemCount() +
            expandableListView.getCheckedItemCount();
        if (checkedItemCount == 0) {
          if (actionMode != null)
            actionMode.finish();
        }
      }
      return true;
    }

  };

  /**
   * Kategórián történő kattintás eseménykezelője.
   */
  private ExpandableListView.OnGroupClickListener expLvGroupClicked =
      new ExpandableListView.OnGroupClickListener() {

        @Override
        public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
          int position = parent.getFlatListPosition(
              ExpandableListView.getPackedPositionForGroup(groupPosition));
          if (!AppController.isActionModeEnabled()) {
            // Category nyitása/zárása.
            if (!parent.isGroupExpanded(groupPosition)) {
              parent.expandGroup(groupPosition);
            } else {
              parent.collapseGroup(groupPosition);
            }
          } else {
            expandableListView.setItemChecked(position, !parent.isItemChecked(position));
            if (parent.isItemChecked(position)) {
              categories.add((Category) parent.getItemAtPosition(position));
            } else {
              categories.remove(parent.getItemAtPosition(position));
            }

            // ActionMode-hoz tartozó ActionBar beállítása.
            actionMode.invalidate();

            // Ha az utolsó kiválasztott elemet is kiválasztatlanná tesszük, akkor
            // ActionMode kikapcsolása.
            int checkedItemCount = listView.getCheckedItemCount() +
                expandableListView.getCheckedItemCount();
            if (checkedItemCount == 0) {
              if (actionMode != null)
                actionMode.finish();
            }
          }
          return true;
        }

      };

  /**
   * Az ExpandableListView Group-jain és Child-jain történő ActionMode hívások eseménykezelője.
   */
  private ExpandableListView.OnCreateContextMenuListener expLvCategoryContextMenu =
      new ExpandableListView.OnCreateContextMenuListener() {

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {

      if (!AppController.isActionModeEnabled()) {
        ExpandableListView.ExpandableListContextMenuInfo info =
            (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
        int position = expandableListView.getFlatListPosition(info.packedPosition);

        if (ExpandableListView.getPackedPositionType(info.packedPosition)
            == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {

          // Category.
          actionModeStartedWithELV = true;
          listener.startActionMode(callback);
          expandableListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
          expandableListView.setItemChecked(position, true);
          categories.add((Category) expandableListView.getItemAtPosition(position));

          listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
          actionMode.invalidate();

        } else if (ExpandableListView.getPackedPositionType(info.packedPosition)
            == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {

          // List.
          actionModeStartedWithELV = true;
          listener.startActionMode(callback);
          expandableListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
          expandableListView.setItemChecked(position, true);
          listsInCategory.add(
              (com.example.todocloud.data.List) expandableListView.getItemAtPosition(position));

          listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
          actionMode.invalidate();

        }
      }
    }

  };

  /**
   * A megadott Category-t felveszi az adatbázisba és frissíti a View-t.
   * @param category A megadott Category.
   */
  @Override
  public void onCategoryCreated(Category category) {
    category.setUserOnlineId(dbLoader.getUserOnlineId());
    category.set_id(dbLoader.createCategory(category));
    category.setCategoryOnlineId(OnlineIdGenerator.generateOnlineId(getActivity(),
        DbConstants.Category.DATABASE_TABLE, category.get_id(), dbLoader.getApiKey()));
    dbLoader.updateCategory(category);
    updateCategoryAdapter();
  }

  /**
   * A megadott Category-t módosítja.
   * @param category A megadott Category.
   */
  @Override
  public void onCategoryModified(Category category) {
    category.setDirty(true);
    dbLoader.updateCategory(category);
    updateCategoryAdapter();
    actionMode.finish();
  }

  /**
   * A megadott List-et felveszi az adatbázisba és frissíti a View-t.
   * @param list A megadott List.
   */
  @Override
  public void onListCreated(com.example.todocloud.data.List list) {
    list.setUserOnlineId(dbLoader.getUserOnlineId());
    list.set_id(dbLoader.createList(list));
    list.setListOnlineId(OnlineIdGenerator.generateOnlineId(getActivity(),
        DbConstants.List.DATABASE_TABLE, list.get_id(), dbLoader.getApiKey()));
    dbLoader.updateList(list);
    updateListAdapter();
  }

  /**
   * A megadott List-et módosítja attól függően, hogy az Category-hez rendelt-e.
   * @param list A megadott List.
   * @param isInCategory A megadott List Category-hez rendelt-e vagy nem.
   */
  @Override
  public void onListModified(com.example.todocloud.data.List list, boolean isInCategory) {
    list.setDirty(true);
    dbLoader.updateList(list);
    // Ha Category-hez rendelt List-et módosítunk, akkor a listAdapter helyett a categoryAdapter-t
    // kell frissítenünk.
    if (isInCategory) {
      updateCategoryAdapter();
    } else {
      updateListAdapter();
    }
    actionMode.finish();
  }

  // Felvesz egy List-et, a megadott Category-hoz rendelten.

  /**
   * A megadott List-et felveszi az adatbázisba, az adott Category-hoz rendelten, majd frissíti a
   * View-t.
   * @param categoryOnlineId Az adott Category categoryOnlineId-je.
   * @param list A megadott List.
   */
  @Override
  public void onListInCategoryCreated(String categoryOnlineId,
                                      com.example.todocloud.data.List list) {
    list.setUserOnlineId(dbLoader.getUserOnlineId());
    list.setCategoryOnlineId(categoryOnlineId);
    list.set_id(dbLoader.createList(list));
    list.setListOnlineId(OnlineIdGenerator.generateOnlineId(getActivity(),
        DbConstants.List.DATABASE_TABLE, list.get_id(), dbLoader.getApiKey()));
    dbLoader.updateList(list);
    updateCategoryAdapter();
    actionMode.finish();
  }

  /**
   * A megadott List-et rendeli hozzá, az adott Category-hez vagy megszünteti a List, Category-hez
   * rendeltségét.
   * @param list A megadott List.
   * @param categoryOnlineId Az adott Category categoryOnlineId-je.
   * @param isListToCategory A megadott List a művelet előtt Category-hez rendelt volt-e vagy nem.
   */
  @Override
  public void onListMoved(com.example.todocloud.data.List list, String categoryOnlineId,
                          boolean isListToCategory) {
    switch (isListToCategory ? 1 : 0) {
      case 0: // A List a művelet előtt Category-hez rendelt volt.
        // Ha a categoryOnlineId null, akkor a Category-hez rendelt List hozzárendelését
        // szüntetjük meg, egyébként Category-hez rendelt List-et rendel másik Category-hez.
        if (categoryOnlineId == null) {
          list.setCategoryOnlineId(null);
          list.setDirty(true);
          dbLoader.updateList(list);
          updateCategoryAdapter();
          updateListAdapter();
          actionMode.finish();
        } else {
          list.setCategoryOnlineId(categoryOnlineId);
          list.setDirty(true);
          dbLoader.updateList(list);
          updateCategoryAdapter();
          actionMode.finish();
        }
        break;
      case 1: // A List a művelet előtt nem volt Category-hez rendelve. Category-hez rendelendő.
        // Ha a Category-hez nem rendelt List-et akarjuk Category-hez nem rendeltté tenni, akkor
        // ne történjen semmi.
        if (categoryOnlineId == null) {
          actionMode.finish();
          return;
        }
        list.setCategoryOnlineId(categoryOnlineId);
        list.setDirty(true);
        dbLoader.updateList(list);
        updateListAdapter();
        updateCategoryAdapter();
        actionMode.finish();
        break;
    }
  }

  @Override
  public void onRefresh() {
    sync();
  }

  /**
   * Törli a megadott típusú objektumot, a megadott onlineId alapján.
   * @param onlineId Az objektum azonosítója.
   * @param type Az objektum típusa.
   */
  @Override
  public void onDelete(String onlineId, String type) {
    if (type != null) {
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
  }

  /**
   * Törli a megadott típusú objektumokat.
   * @param items A megadott objektumokat tartalmazó tömb.
   * @param type A megadott objektumok típusa.
   */
  @Override
  public void onDelete(ArrayList items, String type) {
    if (type != null) {
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
  }

  /**
   * Végrehajtja a kijelentkezést.
   */
  @Override
  public void onLogout() {
    listener.onLogout();
  }

  /**
   * Interfész a MainActivity-vel történő kommunikációra.
   */
  public interface IMainListFragment {
    void onItemSelected(PredefinedListItem predefinedListItem);
    void onItemSelected(com.example.todocloud.data.List list);
    void onLogout();
    void startActionMode(ActionMode.Callback callback);
    void openSettings();
    void setNavigationHeader();
  }

}

package com.rolandvitezhu.todocloud.ui.activity.main.fragment;

import android.app.Dialog;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.SearchView;

import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.data.PredefinedList;
import com.rolandvitezhu.todocloud.data.Todo;
import com.rolandvitezhu.todocloud.datastorage.DbLoader;
import com.rolandvitezhu.todocloud.datastorage.asynctask.UpdateViewModelTask;
import com.rolandvitezhu.todocloud.listener.RecyclerViewOnItemTouchListener;
import com.rolandvitezhu.todocloud.receiver.ReminderSetter;
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity;
import com.rolandvitezhu.todocloud.ui.activity.main.adapter.TodoAdapter;
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.ConfirmDeleteDialogFragment;
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.PredefinedListsViewModel;
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.SearchListsViewModel;
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.TodosViewModel;

import java.util.ArrayList;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class SearchFragment extends Fragment {

  @Inject
  DbLoader dbLoader;
  @Inject
  TodoAdapter todoAdapter;

  @BindView(R.id.recyclerview_search)
  RecyclerView recyclerView;

  private SearchView searchView;

  private ActionMode actionMode;

  private TodosViewModel todosViewModel;
  private SearchListsViewModel searchListsViewModel;
  private PredefinedListsViewModel predefinedListsViewModel;

  Unbinder unbinder;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);

    ((AppController) getActivity().getApplication()).getAppComponent().inject(this);

    todosViewModel = ViewModelProviders.of(getActivity()).get(TodosViewModel.class);
    searchListsViewModel = ViewModelProviders.of(SearchFragment.this.getActivity()).get(SearchListsViewModel.class);
    predefinedListsViewModel = ViewModelProviders.of(SearchFragment.this.getActivity()).get(PredefinedListsViewModel.class);

    todosViewModel.getTodos().observe(
        this,
        new Observer<ArrayList<Todo>>() {

          @Override
          public void onChanged(@Nullable ArrayList<Todo> todos) {
            todoAdapter.update(todos);
            todoAdapter.notifyDataSetChanged();
          }
        }
    );
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState
  ) {
    View view = inflater.inflate(R.layout.fragment_search, container, false);
    unbinder = ButterKnife.bind(this, view);

    prepareRecyclerView(view);
    applyClickEvents();
    applySwipeToDismiss();

    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    ((MainActivity)getActivity()).onSetActionBarTitle("");
    prepareSearchViewAfterModifyTodo();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
  }

  private void prepareRecyclerView(View view) {
    RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(
        getContext().getApplicationContext()
    );
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.setAdapter(todoAdapter);
  }

  private void applyClickEvents() {
    recyclerView.addOnItemTouchListener(new RecyclerViewOnItemTouchListener(
        getContext().getApplicationContext(),
        recyclerView,
        new RecyclerViewOnItemTouchListener.OnClickListener() {

          @Override
          public void onClick(View childView, int childViewAdapterPosition) {
            if (!isActionMode()) {
              hideSoftInput();
              openModifyTodoFragment(childViewAdapterPosition);
            } else {
              todoAdapter.toggleSelection(childViewAdapterPosition);

              if (areSelectedItems()) {
                actionMode.invalidate();
              } else {
                actionMode.finish();
              }
            }
          }

          @Override
          public void onLongClick(View childView, int childViewAdapterPosition) {
            if (!isActionMode()) {
              ((MainActivity)SearchFragment.this.getActivity()).onStartActionMode(callback);
              todoAdapter.toggleSelection(childViewAdapterPosition);
              actionMode.invalidate();
            }
          }

        }
        )
    );
  }

  private void applySwipeToDismiss() {
    ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(
        0, ItemTouchHelper.START
    ) {

      @Override
      public boolean onMove(
          RecyclerView recyclerView,
          RecyclerView.ViewHolder viewHolder,
          RecyclerView.ViewHolder target
      ) {
        return false;
      }

      @Override
      public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        Todo swipedTodo = getSwipedTodo(viewHolder);
        int swipedTodoAdapterPosition = viewHolder.getAdapterPosition();
        openConfirmDeleteTodosDialog(swipedTodo, swipedTodoAdapterPosition);
      }

      @Override
      public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int swipeFlags;
        if (AppController.isActionMode()) swipeFlags = 0;
        else swipeFlags = ItemTouchHelper.START;
        return makeMovementFlags(0, swipeFlags);
      }

    };
    ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
    itemTouchHelper.attachToRecyclerView(recyclerView);
  }

  private Todo getSwipedTodo(RecyclerView.ViewHolder viewHolder) {
    int swipedTodoAdapterPosition = viewHolder.getAdapterPosition();
    return todoAdapter.getTodo(swipedTodoAdapterPosition);
  }

  private boolean areSelectedItems() {
    return todoAdapter.getSelectedItemCount() > 0;
  }

  private boolean isActionMode() {
    return actionMode != null;
  }

  private void openModifyTodoFragment(int childViewAdapterPosition) {
    Todo todo = todoAdapter.getTodo(childViewAdapterPosition);
    todosViewModel.setTodo(todo);

    ((MainActivity)getActivity()).openModifyTodoFragment(this);
  }

  private void prepareSearchViewAfterModifyTodo() {
    if (searchView != null && recyclerView != null) {
      searchView.post(new Runnable() {
        @Override
        public void run() {
          restoreQueryTextState();
          recyclerView.requestFocusFromTouch();
          searchView.clearFocus();
          hideSoftInput();
        }
      });
    }
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    restoreQueryTextState();
  }

  private void restoreQueryTextState() {
    if (searchView != null)
      searchView.setQuery(searchListsViewModel.getQueryText(), false);
  }

  private ActionMode.Callback callback = new ActionMode.Callback() {

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
      setActionMode(mode);
      mode.getMenuInflater().inflate(R.menu.layout_appbar_search, menu);
      preventTypeIntoSearchView();

      return true;
    }

    private void preventTypeIntoSearchView() {
      if (searchView != null && recyclerView != null) {
        recyclerView.requestFocusFromTouch();
      }
      hideSoftInput();
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
      String title = prepareTitle();
      actionMode.setTitle(title);

      return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
      int menuItemId = item.getItemId();

      switch (menuItemId) {
        case R.id.menuitem_layoutappbarsearch_delete:
          openConfirmDeleteTodosDialog();
          break;
      }

      return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
      todoAdapter.clearSelection();
      setActionMode(null);
    }

    private String prepareTitle() {
      int selectedItemCount = todoAdapter.getSelectedItemCount();
      String title = selectedItemCount + " " + getString(R.string.all_selected);
      return title;
    }

  };

  private void setActionMode(ActionMode actionMode) {
    this.actionMode = actionMode;
    AppController.setActionMode(actionMode);
  }

  private void openConfirmDeleteTodosDialog() {
    ArrayList<Todo> selectedTodos = todoAdapter.getSelectedTodos();
    Bundle arguments = new Bundle();
    arguments.putString("itemType", "todo");
    arguments.putParcelableArrayList("itemsToDelete", selectedTodos);
    openConfirmDeleteDialogFragment(arguments);
  }

  private void openConfirmDeleteTodosDialog(Todo swipedTodo, int swipedTodoAdapterPosition) {
    ArrayList<Todo> selectedTodos = new ArrayList<>();
    selectedTodos.add(swipedTodo);
    Bundle arguments = new Bundle();
    arguments.putString("itemType", "todo");
    arguments.putParcelableArrayList("itemsToDelete", selectedTodos);
    openConfirmDeleteDialogFragment(arguments, swipedTodoAdapterPosition);
  }

  private void openConfirmDeleteDialogFragment(Bundle arguments) {
    ConfirmDeleteDialogFragment confirmDeleteDialogFragment = new ConfirmDeleteDialogFragment();
    confirmDeleteDialogFragment.setTargetFragment(this, 0);
    confirmDeleteDialogFragment.setArguments(arguments);
    confirmDeleteDialogFragment.show(getFragmentManager(), "ConfirmDeleteDialogFragment");
  }

  private void openConfirmDeleteDialogFragment(
      Bundle arguments, final int swipedTodoAdapterPosition
  ) {
    ConfirmDeleteDialogFragment confirmDeleteDialogFragment = new ConfirmDeleteDialogFragment();
    confirmDeleteDialogFragment.setTargetFragment(this, 0);
    confirmDeleteDialogFragment.setArguments(arguments);
    confirmDeleteDialogFragment.show(getFragmentManager(), "ConfirmDeleteDialogFragment");
    applyDismissEvents(swipedTodoAdapterPosition, confirmDeleteDialogFragment);
  }

  private void applyDismissEvents(
      final int swipedTodoAdapterPosition, ConfirmDeleteDialogFragment confirmDeleteDialogFragment
  ) {
    getFragmentManager().executePendingTransactions();
    Dialog confirmDeleteDialogFragmentDialog = confirmDeleteDialogFragment.getDialog();
    confirmDeleteDialogFragmentDialog.setOnDismissListener(
        new DialogInterface.OnDismissListener() {

          @Override
          public void onDismiss(DialogInterface dialog) {
            todoAdapter.notifyItemChanged(swipedTodoAdapterPosition);
          }

        }
    );
  }

  private void updateTodosViewModel() {
    UpdateViewModelTask updateViewModelTask = new UpdateViewModelTask(todosViewModel, getActivity());
    updateViewModelTask.execute();
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.fragment_search, menu);
    prepareSearchView(menu);
  }

  private void prepareSearchView(Menu menu) {
    MenuItem searchMenuItem = menu.findItem(R.id.menuitem_search);
    searchView = (SearchView) searchMenuItem.getActionView();
    SearchManager searchManager = (SearchManager) getActivity()
        .getSystemService(Context.SEARCH_SERVICE);
    SearchableInfo searchableInfo = searchManager.getSearchableInfo(
        getActivity().getComponentName()
    );
    searchView.setSearchableInfo(searchableInfo);
    searchView.setMaxWidth(Integer.MAX_VALUE);
    searchView.setIconified(false);
    searchView.setFocusable(true);
    searchView.requestFocusFromTouch();
    disableSearchViewCloseButton();
    removeSearchViewUnderline();
    removeSearchViewHintIcon();
    applyOnQueryTextEvents();
  }

  private void removeSearchViewUnderline() {
    int searchPlateId = searchView.getContext().getResources().getIdentifier(
        "android:id/search_plate", null, null
    );
    View searchPlate = searchView.findViewById(searchPlateId);
    if (searchPlate != null) {
      searchPlate.setBackgroundResource(0);
    }
  }

  private void removeSearchViewHintIcon() {
    if (searchView != null) {
      int searchMagIconId = searchView.getContext().getResources().getIdentifier(
          "android:id/search_mag_icon", null, null
      );
      View searchMagIcon = searchView.findViewById(searchMagIconId);
      if (searchMagIcon != null) {
        searchView.setIconifiedByDefault(false);
        searchMagIcon.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
      }
    }
  }

  private void disableSearchViewCloseButton() {
    searchView.setOnCloseListener(new SearchView.OnCloseListener() {
      @Override
      public boolean onClose() {
        return true;
      }
    });
  }

  private void applyOnQueryTextEvents() {
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

      @Override
      public boolean onQueryTextSubmit(String query) {
        preventToExecuteQueryTextSubmitTwice();
        return true;
      }

      private void preventToExecuteQueryTextSubmitTwice() {
        if (searchView != null) {
          searchView.clearFocus();
          hideSoftInput();
        }
      }

      @Override
      public boolean onQueryTextChange(String newText) {
        saveQueryTextState(newText);
        if (!newText.isEmpty()) {
          showSearchResults(newText);
        } else {
          clearSearchResults();
        }
        return true;
      }

      private void saveQueryTextState(String queryText) {
        searchListsViewModel.setQueryText(queryText);
      }

      private void showSearchResults(String newText) {
        setUpdateTodosViewModelObjects(newText);
        updateTodosViewModel();
      }

      private void clearSearchResults() {
        todoAdapter.clear();
      }

      private void setUpdateTodosViewModelObjects(String queryText) {
        String where = dbLoader.prepareSearchWhere(queryText);

        todosViewModel.setIsPredefinedList(true);
        predefinedListsViewModel.setPredefinedList(new PredefinedList("0", where));
      }

    });
  }

  private void hideSoftInput() {
    InputMethodManager inputMethodManager =
        (InputMethodManager) getActivity().getSystemService(
            Context.INPUT_METHOD_SERVICE
        );
    View currentlyFocusedView = getActivity().getCurrentFocus();
    if (currentlyFocusedView != null) {
      IBinder windowToken = currentlyFocusedView.getWindowToken();
      inputMethodManager.hideSoftInputFromWindow(windowToken, 0);
    }
  }

  private boolean isSetReminder(Todo todo) {
    return !todo.getReminderDateTime().equals("-1");
  }

  private boolean isNotCompleted(Todo todo) {
    return !todo.isCompleted();
  }

  private boolean shouldCreateReminderService(Todo todoToModify) {
    return isNotCompleted(todoToModify) && isNotDeleted(todoToModify);
  }

  private boolean isNotDeleted(Todo todo) {
    return !todo.getDeleted();
  }

  public void onSoftDelete(String onlineId, String itemType) {
    Todo todoToSoftDelete = dbLoader.getTodo(onlineId);
    dbLoader.softDeleteTodo(todoToSoftDelete);
    updateTodosViewModel();
    ReminderSetter.cancelReminderService(todoToSoftDelete);
    if (actionMode != null) {
      actionMode.finish();
    }
  }

  public void onSoftDelete(ArrayList itemsToDelete, String itemType) {
    ArrayList<Todo> todosToSoftDelete = itemsToDelete;
    for (Todo todoToSoftDelete:todosToSoftDelete) {
      dbLoader.softDeleteTodo(todoToSoftDelete);
      ReminderSetter.cancelReminderService(todoToSoftDelete);
    }
    updateTodosViewModel();
    if (actionMode != null) {
      actionMode.finish();
    }
  }

}

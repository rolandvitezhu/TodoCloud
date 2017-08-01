package com.example.todocloud.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
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

import com.example.todocloud.R;
import com.example.todocloud.adapter.TodoAdapter;
import com.example.todocloud.app.AppController;
import com.example.todocloud.data.Todo;
import com.example.todocloud.datastorage.DbConstants;
import com.example.todocloud.datastorage.DbLoader;
import com.example.todocloud.datastorage.asynctask.UpdateAdapterTask;
import com.example.todocloud.helper.OnlineIdGenerator;
import com.example.todocloud.listener.RecyclerViewOnItemTouchListener;
import com.example.todocloud.receiver.ReminderSetter;

import java.util.ArrayList;

public class TodoListFragment extends Fragment implements
    CreateTodoFragment.ICreateTodoFragment,
    ModifyTodoFragment.IModifyTodoFragment,
    ConfirmDeleteDialogFragment.IConfirmDeleteDialogFragment {

  private DbLoader dbLoader;
  private TodoAdapter todoAdapter;
  private RecyclerView recyclerView;
  private ITodoListFragment listener;
  private ActionMode actionMode;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (ITodoListFragment) context;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    dbLoader = new DbLoader();
    updateTodoAdapter();
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState
  ) {
    View view = inflater.inflate(R.layout.todo_list, container, false);
    prepareRecyclerView(view);
    applyClickEvents();
    applySwipeToDismiss();
    prepareFloatingActionButton(view);
    return view;
  }

  private void prepareFloatingActionButton(View view) {
    FloatingActionButton floatingActionButton =
        (FloatingActionButton) view.findViewById(R.id.floatingActionButton);
    floatingActionButton.setOnClickListener(floatingActionButtonClicked);
  }

  private void prepareRecyclerView(View view) {
    recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
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
              listener.onStartActionMode(callback);
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
    listener.onClickTodo(todo, this);
  }

  @Override
  public void onResume() {
    super.onResume();
    setActionBarTitle();
  }

  private void setActionBarTitle() {
    String title = getArguments().getString("title");
    if (title != null) {
      if (!getArguments().getBoolean("isPredefinedList")) { // List
        listener.onSetActionBarTitle(title);
      } else { // PredefinedList
        switch (title) {
          case "0":
            listener.onSetActionBarTitle(getString(R.string.MainListToday));
            break;
          case "1":
            listener.onSetActionBarTitle(getString(R.string.MainListNext7Days));
            break;
          case "2":
            listener.onSetActionBarTitle(getString(R.string.MainListAll));
            break;
          case "3":
            listener.onSetActionBarTitle(getString(R.string.MainListCompleted));
            break;
        }
      }
    }
  }

  private ActionMode.Callback callback = new ActionMode.Callback() {

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
      setActionMode(mode);
      mode.getMenuInflater().inflate(R.menu.todo, menu);

      return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
      String title = prepareTitle();
      actionMode.setTitle(title);

      return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
      int actionItemId = item.getItemId();

      switch (actionItemId) {
        case R.id.itemDelete:
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
      String title = selectedItemCount + " " + getString(R.string.selected);
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
    confirmDeleteDialogFragment.show(getChildFragmentManager(), "ConfirmDeleteDialogFragment");
    applyDismissEvents(swipedTodoAdapterPosition, confirmDeleteDialogFragment);
  }

  private void applyDismissEvents(
      final int swipedTodoAdapterPosition, ConfirmDeleteDialogFragment confirmDeleteDialogFragment
  ) {
    getChildFragmentManager().executePendingTransactions();
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

  private View.OnClickListener floatingActionButtonClicked = new View.OnClickListener() {

    @Override
    public void onClick(View v) {
      if (isActionMode()) actionMode.finish();
      listener.onOpenCreateTodoFragment(TodoListFragment.this);
    }

  };

  private void updateTodoAdapter() {
    if (todoAdapter == null) {
      todoAdapter = new TodoAdapter(dbLoader);
    }
    UpdateAdapterTask updateAdapterTask = new UpdateAdapterTask(dbLoader, todoAdapter);
    updateAdapterTask.execute(getArguments());
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.todo_options_menu, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int optionsItemId = item.getItemId();

    switch (optionsItemId) {
      case R.id.createTodo:
        listener.onOpenCreateTodoFragment(this);
        break;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onCreateTodo(Todo todo) {
    createTodoInLocalDatabase(todo);
    updateTodoAdapter();

    if (isSetReminder(todo) && isNotCompleted(todo)) {
      ReminderSetter.createReminderService(todo);
    }
  }

  private void createTodoInLocalDatabase(Todo todoToCreate) {
    Bundle arguments = getArguments();

    if (isPredefinedListCompleted(arguments)) {
      todoToCreate.setCompleted(true);
    }

    String listOnlineId = arguments.getString("listOnlineId");
    if (!isPredefinedList(listOnlineId)) {
      todoToCreate.setListOnlineId(listOnlineId);
    }

    todoToCreate.setUserOnlineId(dbLoader.getUserOnlineId());
    todoToCreate.set_id(dbLoader.createTodo(todoToCreate));
    String todoOnlineId = OnlineIdGenerator.generateOnlineId(
        DbConstants.Todo.DATABASE_TABLE,
        todoToCreate.get_id(),
        dbLoader.getApiKey()
    );
    todoToCreate.setTodoOnlineId(todoOnlineId);
    dbLoader.updateTodo(todoToCreate);
  }

  private boolean isPredefinedListCompleted(Bundle arguments) {
    String selectFromArguments = arguments.getString("selectFromDB");
    String selectPredefinedListCompleted =
        DbConstants.Todo.KEY_COMPLETED +
            "=" +
            1 +
            " AND " +
            DbConstants.Todo.KEY_USER_ONLINE_ID +
            "='" +
            dbLoader.getUserOnlineId() +
            "'" +
            " AND " +
            DbConstants.Todo.KEY_DELETED +
            "=" +
            0;

    return selectFromArguments != null && selectFromArguments.equals(selectPredefinedListCompleted);
  }

  private boolean isPredefinedList(String listOnlineId) {
    return listOnlineId == null;
  }

  private boolean isSetReminder(Todo todo) {
    return !todo.getReminderDateTime().equals("-1");
  }

  private boolean isNotCompleted(Todo todo) {
    return !todo.isCompleted();
  }

  @Override
  public void onModifyTodo(Todo todo) {
    dbLoader.updateTodo(todo);
    updateTodoAdapter();

    if (isSetReminder(todo)) {
      if (shouldCreateReminderService(todo)) {
        ReminderSetter.createReminderService(todo);
      }
    } else {
      ReminderSetter.cancelReminderService(todo);
    }
  }

  private boolean shouldCreateReminderService(Todo todoToModify) {
    return isNotCompleted(todoToModify) && isNotDeleted(todoToModify);
  }

  private boolean isNotDeleted(Todo todo) {
    return !todo.getDeleted();
  }

  @Override
  public void onSoftDelete(String onlineId, String itemType) {
    Todo todoToSoftDelete = dbLoader.getTodo(onlineId);
    dbLoader.softDeleteTodo(todoToSoftDelete);
    updateTodoAdapter();
    ReminderSetter.cancelReminderService(todoToSoftDelete);
    if (actionMode != null) {
      actionMode.finish();
    }
  }

  @Override
  public void onSoftDelete(ArrayList itemsToDelete, String itemType) {
    ArrayList<Todo> todosToSoftDelete = itemsToDelete;
    for (Todo todoToSoftDelete:todosToSoftDelete) {
      dbLoader.softDeleteTodo(todoToSoftDelete);
      ReminderSetter.cancelReminderService(todoToSoftDelete);
    }
    updateTodoAdapter();
    if (actionMode != null) {
      actionMode.finish();
    }
  }

  public interface ITodoListFragment {
    void onSetActionBarTitle(String actionBarTitle);
    void onStartActionMode(ActionMode.Callback callback);
    void onClickTodo(Todo todo, Fragment targetFragment);
    void onOpenCreateTodoFragment(TodoListFragment targetFragment);
  }

}

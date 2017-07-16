package com.example.todocloud.fragment;

import android.content.Context;
import android.content.Intent;
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
import com.example.todocloud.service.AlarmService;

import java.util.ArrayList;

public class TodoListFragment extends Fragment implements
    TodoCreateFragment.ITodoCreateFragment,
    TodoModifyFragment.ITodoModifyFragment,
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
    dbLoader = new DbLoader(getActivity());
    updateTodoAdapterTest();
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.todo_list, container, false);
    recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
    RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(
        getContext().getApplicationContext()
    );
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.setAdapter(todoAdapter);
    recyclerView.addOnItemTouchListener(new RecyclerViewOnItemTouchListener(
        getContext().getApplicationContext(),
        recyclerView,
        new RecyclerViewOnItemTouchListener.ClickListener() {

          @Override
          public void onClick(View childView, int childViewAdapterPosition) {
            if (!isActionMode()) {
              openTodoModifyFragment(childViewAdapterPosition);
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
              listener.startActionMode(callback);
              todoAdapter.toggleSelection(childViewAdapterPosition);
              actionMode.invalidate();
            }
          }

        }
        )
    );
    applySwipeToDismiss();
    FloatingActionButton floatingActionButton =
        (FloatingActionButton) view.findViewById(R.id.floatingActionButton);
    floatingActionButton.setOnClickListener(floatingActionButtonClicked);
    return view;
  }

  private void applySwipeToDismiss() {
    ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(
        0, ItemTouchHelper.END
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
        toggleCompleted(swipedTodo);
        updateTodo(swipedTodo);
        todoAdapter.removeTodoFromAdapter((TodoAdapter.ItemViewHolder) viewHolder);
        handleReminderService(swipedTodo);
      }

      @Override
      public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int swipeFlags;
        if (AppController.isActionMode()) swipeFlags = 0;
        else swipeFlags = ItemTouchHelper.END;
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

  private void toggleCompleted(Todo todo) {
    todo.setCompleted(!todo.isCompleted());
  }

  private void updateTodo(Todo todo) {
    todo.setDirty(true);
    dbLoader.updateTodo(todo);
  }

  private void handleReminderService(Todo todo) {
    if (isCompleted(todo)) {
      cancelReminderService(todo);
    } else if (isSetReminder(todo)) {
      createReminderService(todo);
    }
  }

  private Boolean isCompleted(Todo todo) {
    return todo.isCompleted();
  }

  private boolean areSelectedItems() {
    return todoAdapter.getSelectedItemCount() > 0;
  }

  private boolean isActionMode() {
    return actionMode != null;
  }

  private void openTodoModifyFragment(int childViewAdapterPosition) {
    Todo clickedTodo = todoAdapter.getTodo(childViewAdapterPosition);
    listener.onTodoClicked(clickedTodo, this);
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
        listener.setActionBarTitle(title);
      } else { // PredefinedList
        switch (title) {
          case "0":
            listener.setActionBarTitle(getString(R.string.MainListToday));
            break;
          case "1":
            listener.setActionBarTitle(getString(R.string.MainListNext7Days));
            break;
          case "2":
            listener.setActionBarTitle(getString(R.string.MainListAll));
            break;
          case "3":
            listener.setActionBarTitle(getString(R.string.MainListCompleted));
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
          confirmDeletion();
          break;
      }

      return true;
    }


    @Override
    public void onDestroyActionMode(ActionMode mode) {
      todoAdapter.clearSelection();
      setActionMode(null);
    }

  };

  private void setActionMode(ActionMode actionMode) {
    this.actionMode = actionMode;
    AppController.setActionMode(actionMode);
  }

  private String prepareTitle() {
    int selectedItemCount = todoAdapter.getSelectedItemCount();
    String title = selectedItemCount + " " + getString(R.string.selected);
    return title;
  }

  private void confirmDeletion() {
    ArrayList<Todo> selectedTodos = todoAdapter.getSelectedTodos();
    openConfirmDeleteDialogFragment(selectedTodos);
  }

  private void openConfirmDeleteDialogFragment(ArrayList<Todo> todosToDelete) {
    ConfirmDeleteDialogFragment confirmDeleteDialogFragment = new ConfirmDeleteDialogFragment();
    confirmDeleteDialogFragment.setTargetFragment(this, 0);
    Bundle arguments = new Bundle();
    arguments.putString("type", "todo");
    arguments.putParcelableArrayList("items", todosToDelete);
    confirmDeleteDialogFragment.setArguments(arguments);
    confirmDeleteDialogFragment.show(getFragmentManager(), "ConfirmDeleteDialogFragment");
  }

  private View.OnClickListener floatingActionButtonClicked = new View.OnClickListener() {

    @Override
    public void onClick(View v) {
      if (isActionMode()) actionMode.finish();
      listener.openTodoCreateFragment(TodoListFragment.this);
    }

  };

  private void updateTodoAdapterTest() {
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
        listener.openTodoCreateFragment(this);
        break;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onCreateTodo(Todo todoToCreate) {
    createTodoInLocalDatabase(todoToCreate);
    updateTodoAdapterTest();

    if (isSetReminder(todoToCreate) && isNotCompleted(todoToCreate)) {
      createReminderService(todoToCreate);
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
        getActivity(),
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

  private void createReminderService(Todo todo) {
    Context applicationContext = getActivity().getApplicationContext();
    Intent reminderService = new Intent(applicationContext, AlarmService.class);
    reminderService.putExtra("todo", todo);
    reminderService.setAction(AlarmService.CREATE);
    applicationContext.startService(reminderService);
  }

  @Override
  public void onModifyTodo(Todo todoToModify) {
    dbLoader.updateTodo(todoToModify);
    updateTodoAdapterTest();

    if (isSetReminder(todoToModify)) {
      if (shouldCreateReminderService(todoToModify)) {
        createReminderService(todoToModify);
      }
    } else {
      cancelReminderService(todoToModify);
    }
  }

  private boolean shouldCreateReminderService(Todo todoToModify) {
    return isNotCompleted(todoToModify) && isNotDeleted(todoToModify);
  }

  private boolean isNotDeleted(Todo todo) {
    return !todo.getDeleted();
  }

  private void cancelReminderService(Todo todo) {
    Context applicationContext = getActivity().getApplicationContext();
    Intent reminderService = new Intent(applicationContext, AlarmService.class);
    reminderService.putExtra("todo", todo);
    reminderService.setAction(AlarmService.CANCEL);
    applicationContext.startService(reminderService);
  }

  @Override
  public void onSoftDelete(String onlineId, String type) {
    Todo todoToSoftDelete = dbLoader.getTodo(onlineId);
    dbLoader.softDeleteTodo(todoToSoftDelete);
    updateTodoAdapterTest();
    cancelReminderService(todoToSoftDelete);
    actionMode.finish();
  }

  @Override
  public void onSoftDelete(ArrayList items, String type) {
    // Todo: Refactor the whole delete confirmation and deletion process. Rename the "items"
    // variable here and in the arguments also to "itemsToDelete".
    ArrayList<Todo> todosToSoftDelete = items;
    for (Todo todoToSoftDelete:todosToSoftDelete) {
      dbLoader.softDeleteTodo(todoToSoftDelete);
      cancelReminderService(todoToSoftDelete);
    }
    updateTodoAdapterTest();
    actionMode.finish();
  }

  public interface ITodoListFragment {
    void setActionBarTitle(String actionBarTitle);
    void startActionMode(ActionMode.Callback callback);
    void onTodoClicked(Todo clickedTodo, TodoListFragment targetFragment);
    void openTodoCreateFragment(TodoListFragment targetFragment);
  }

}

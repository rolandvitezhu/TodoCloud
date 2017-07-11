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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.example.todocloud.R;
import com.example.todocloud.adapter.TodoAdapterTest;
import com.example.todocloud.data.Todo;
import com.example.todocloud.datastorage.DbConstants;
import com.example.todocloud.datastorage.DbLoader;
import com.example.todocloud.datastorage.asynctask.UpdateAdapterTask;
import com.example.todocloud.helper.OnlineIdGenerator;
import com.example.todocloud.listener.RecyclerViewOnItemTouchListener;
import com.example.todocloud.service.AlarmService;

import java.util.ArrayList;

public class TodoListFragmentTest extends Fragment implements
    TodoCreateFragment.ITodoCreateFragment,
    TodoModifyFragment.ITodoModifyFragment,
    ConfirmDeleteDialogFragment.IConfirmDeleteDialogFragment {

  private DbLoader dbLoader;
  private TodoAdapterTest todoAdapterTest;
  private RecyclerView recyclerView;
  private ITodoListFragmentTest listener;
  private ActionMode actionMode;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (ITodoListFragmentTest) context;
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
    View view = inflater.inflate(R.layout.todo_list_test, container, false);
    recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
    RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(
        getContext().getApplicationContext()
    );
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.setAdapter(todoAdapterTest);
    recyclerView.addOnItemTouchListener(new RecyclerViewOnItemTouchListener(
        getContext().getApplicationContext(),
        recyclerView,
        new RecyclerViewOnItemTouchListener.ClickListener() {

          @Override
          public void onClick(View childView, int childViewAdapterPosition) {
            if (!isActionMode()) {
              openTodoModifyFragment(childViewAdapterPosition);
            } else {
              todoAdapterTest.toggleSelection(childViewAdapterPosition);

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
              todoAdapterTest.toggleSelection(childViewAdapterPosition);
              actionMode.invalidate();
            }
          }

        }
        )
    );
    FloatingActionButton floatingActionButton =
        (FloatingActionButton) view.findViewById(R.id.floatingActionButton);
    floatingActionButton.setOnClickListener(floatingActionButtonClicked);
    return view;
  }

  private boolean areSelectedItems() {
    return todoAdapterTest.getSelectedItemCount() > 0;
  }

  private boolean isActionMode() {
    return actionMode != null;
  }

  private void openTodoModifyFragment(int childViewAdapterPosition) {
    Todo clickedTodo = todoAdapterTest.getTodo(childViewAdapterPosition);
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
      actionMode = mode;
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
      todoAdapterTest.clearSelection();
      actionMode = null;
    }

  };

  private String prepareTitle() {
    int selectedItemCount = todoAdapterTest.getSelectedItemCount();
    String title = selectedItemCount + " " + getString(R.string.selected);
    return title;
  }

  private void confirmDeletion() {
    ArrayList<Todo> selectedTodos = todoAdapterTest.getSelectedTodos();
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
      listener.openTodoCreateFragment(TodoListFragmentTest.this);
    }

  };

  private void updateTodoAdapterTest() {
    if (todoAdapterTest == null) {
      todoAdapterTest = new TodoAdapterTest();
    }
    UpdateAdapterTask updateAdapterTask = new UpdateAdapterTask(dbLoader, todoAdapterTest);
    updateAdapterTask.execute(getArguments());
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.todo_test_options_menu, menu);
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
    return !todo.getCompleted();
  }

  private void createReminderService(Todo todo) {
    Intent reminderService = new Intent(getActivity(), AlarmService.class);
    reminderService.putExtra("todo", todo);
    reminderService.setAction(AlarmService.CREATE);
    getActivity().startService(reminderService);
  }

  @Override
  public void onModifyTodo(Todo todoToModify) {
    dbLoader.updateTodo(todoToModify);
    updateTodoAdapterTest();

    if (isSetReminder(todoToModify)) {
      if (isNotCompleted(todoToModify) && isNotDeleted(todoToModify)) {
        createReminderService(todoToModify);
      }
    } else {
      cancelReminderService(todoToModify);
    }
  }

  private boolean isNotDeleted(Todo todo) {
    return !todo.getDeleted();
  }

  private void cancelReminderService(Todo todo) {
    Intent reminderService = new Intent(getActivity(), AlarmService.class);
    reminderService.putExtra("todo", todo);
    reminderService.setAction(AlarmService.CANCEL);
    getActivity().startService(reminderService);
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

  public interface ITodoListFragmentTest {
    void setActionBarTitle(String actionBarTitle);
    void startActionMode(ActionMode.Callback callback);
    void onTodoClicked(Todo clickedTodo, TodoListFragmentTest targetFragment);
    void openTodoCreateFragment(TodoListFragmentTest targetFragment);
  }

}

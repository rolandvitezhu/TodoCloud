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
import java.util.List;

public class TodoListFragmentTest extends Fragment implements
    TodoCreateFragment.ITodoCreateFragment,
    TodoModifyFragment.ITodoModifyFragment {

  private DbLoader dbLoader;
  private TodoAdapterTest todoAdapterTest;
  private RecyclerView recyclerView;
  private ITodoListFragmentTest listener;

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
            openTodoModifyFragment(childViewAdapterPosition);
            // Uncomment the below code, if ActionMode is implemented and also delete the above
//            if (!isActionMode()) {
//              openTodoModifyFragment(position);
//            } else {
//              actionMode.invalidate();
//
//              if (isNoSelectedItems()) {
//                actionMode.finish();
//              }
//              // The selection of TodoListItems happening automatically, because the ActionMode is active
//              // and ChoiceMode == AbsListView.CHOICE_MODE_MULTIPLE
//            }
          }

          @Override
          public void onLongClick(View childView, int childViewAdapterPosition) {

          }

        }
        )
    );
    FloatingActionButton floatingActionButton =
        (FloatingActionButton) view.findViewById(R.id.floatingActionButton);
    floatingActionButton.setOnClickListener(floatingActionButtonClicked);
    return view;
  }

  private void openTodoModifyFragment(int childViewAdapterPosition) {
    Todo clickedTodo = todoAdapterTest.getItem(childViewAdapterPosition);
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

  private View.OnClickListener floatingActionButtonClicked = new View.OnClickListener() {

    @Override
    public void onClick(View v) {
      // Uncomment the below line, if ActionMode is implemented
      // if (isActionMode()) actionMode.finish();
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

  public interface ITodoListFragmentTest {
    void setActionBarTitle(String actionBarTitle);
    void startActionMode(ActionMode.Callback callback);
    void onTodoClicked(Todo clickedTodo, TodoListFragmentTest targetFragment);
    void openTodoCreateFragment(TodoListFragmentTest targetFragment);
  }

}

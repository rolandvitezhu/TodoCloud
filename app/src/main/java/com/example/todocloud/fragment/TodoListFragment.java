package com.example.todocloud.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ListFragment;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.todocloud.R;
import com.example.todocloud.adapter.TodoAdapter;
import com.example.todocloud.data.Todo;
import com.example.todocloud.datastorage.DbConstants;
import com.example.todocloud.datastorage.DbLoader;
import com.example.todocloud.datastorage.asynctask.UpdateAdapterTask;
import com.example.todocloud.fragment.TodoCreateFragment.ITodoCreateFragment;
import com.example.todocloud.fragment.TodoModifyFragment.ITodoModifyFragment;
import com.example.todocloud.helper.OnlineIdGenerator;
import com.example.todocloud.service.AlarmService;

import java.util.ArrayList;

public class TodoListFragment extends ListFragment implements ITodoCreateFragment,
    ITodoModifyFragment, ConfirmDeleteDialogFragment.IConfirmDeleteFragment {

	private DbLoader dbLoader;
  private TodoAdapter todoAdapter;
  private ITodoListFragment listener;
  private ActionMode actionMode;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (ITodoListFragment) context;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  setHasOptionsMenu(true);
	  dbLoader = new DbLoader(getActivity());
    updateTodoAdapter();
		setListAdapter(todoAdapter);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.todo_list, container, false);
    FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab);
    fab.setOnClickListener(fabClicked);
    return view;
  }

  @Override
  public void onStart() {
	  super.onStart();
	  getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

      @Override
      public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (!isActionMode()) {
          listener.startActionMode(callback);
          getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
          getListView().setItemChecked(position, true);
          actionMode.invalidate();
        }
        return true;
      }

    });
  }

  @Override
  public void onResume() {
    super.onResume();
    setActionBarTitle();
  }

  private boolean isActionMode() {
    return actionMode != null;
  }

  private void setActionBarTitle() {
    String title = getArguments().getString("title");
    if (title != null) {
      if (!getArguments().getBoolean("isPredefinedList")) { // List
        listener.setActionBarTitle(title);
      } else { // PredefinedList
        switch (title) {
          case "0":
            listener.setActionBarTitle(getString(R.string.itemMainListToday));
            break;
          case "1":
            listener.setActionBarTitle(getString(R.string.itemMainListNext7Days));
            break;
          case "2":
            listener.setActionBarTitle(getString(R.string.itemMainListAll));
            break;
          case "3":
            listener.setActionBarTitle(getString(R.string.itemMainListCompleted));
            break;
        }
      }
    }
  }

  private ActionMode.Callback callback = new ActionMode.Callback() {

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
      // TODO: Szüntesd meg az actionModeEnabled változó használatát a TodoListFragment-ben,
      // alkalmazz helyette egy új objektumot, amely tárolja statikus változóban az ActionMode
      // objektumot és legyen képes azt üressé tenni, illetve lekérdezni, hogy üres-e az. Szüntesd
      // meg a TodoListFragment osztály szintű actionMode változóját, mivel arra sincs már szükség.

      // Az alábbi 3 utasítást cserélni szükséges:
      // AppController.setActionModeEnabled(false)
      // AppController.setActionModeEnabled(true)
      // AppController.isActionModeEnabled()

      // Ezt követően teszteld a TodoListFragment-et az ActionMode-ra vonatkozóan.
      // Ha a teszt rendben zárul, tedd ezt a MainListFragment-tel is, majd ha a teszt ott is
      // sikerrel zárul, akkor töröld az AppController osztály actionMode változóját a hozzá
      // tartozó metódusokkal együtt.

      // Először teszteljük kizárólag az új objektumot Log üzenetek segítségével, ne használjuk
      // azt élesben.
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
      deselectItems();
      actionMode = null;
      getListView().setChoiceMode(AbsListView.CHOICE_MODE_NONE);
    }

  };

  private void deselectItems() {
    int itemCount = getListAdapter().getCount();
    for (int i = 0; i < itemCount; i++) {
      getListView().setItemChecked(i, false);
    }
  }

  private String prepareTitle() {
    int selectedItemCount = getListView().getCheckedItemCount();
    String title = selectedItemCount + " " + getString(R.string.selected);
    return title;
  }

  private void confirmDeletion() {
    ArrayList<Todo> selectedTodos = getSelectedTodos();
    openConfirmDeleteDialogFragment(selectedTodos);
  }

  private ArrayList<Todo> getSelectedTodos() {
    int itemCount = getListAdapter().getCount();
    ListView todoList = getListView();
    ArrayList<Todo> selectedTodos = new ArrayList<>();

    for (int i = 0; i < itemCount; i++) {
      if (todoList.isItemChecked(i)) {
        Todo todo = (Todo) todoList.getItemAtPosition(i);
        selectedTodos.add(todo);
      }
    }

    return selectedTodos;
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

	@Override
  public void onListItemClick(ListView l, View v, int position, long id) {
	  super.onListItemClick(l, v, position, id);
    if (!isActionMode()) {
      openTodoModifyFragment(position);
    } else {
      actionMode.invalidate();

      if (isNoSelectedItems()) {
        actionMode.finish();
      }
      // The selection of TodoListItems happening automatically, because the ActionMode is active
      // and ChoiceMode == AbsListView.CHOICE_MODE_MULTIPLE
    }
  }

  private boolean isNoSelectedItems() {
    return getListView().getCheckedItemCount() == 0;
  }

  private void openTodoModifyFragment(int position) {
    Todo clickedTodo = todoAdapter.getItem(position);
    listener.onTodoClicked(clickedTodo, this);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	  inflater.inflate(R.menu.todo_options_menu, menu);
  }

	@Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int optionsItemId = item.getItemId();

    switch (optionsItemId) {
      case R.id.itemCreateTodo:
        listener.openTodoCreateFragment(this);
        break;
    }

	  return super.onOptionsItemSelected(item);
  }

  public void updateTodoAdapter() {
    if (todoAdapter == null) {
      todoAdapter = new TodoAdapter(dbLoader, getActivity());
    }
    UpdateAdapterTask updateAdapterTask = new UpdateAdapterTask(dbLoader, todoAdapter);
    updateAdapterTask.execute(getArguments());
  }

  private View.OnClickListener fabClicked = new View.OnClickListener() {

    @Override
    public void onClick(View v) {
      if (isActionMode()) actionMode.finish();
      listener.openTodoCreateFragment(TodoListFragment.this);
    }

  };

  @Override
  public void onCreateTodo(Todo todoToCreate) {
    createTodoInLocalDatabase(todoToCreate);
    updateTodoAdapter();

    if (isSetReminder(todoToCreate) && isNotCompleted(todoToCreate)) {
      createReminderService(todoToCreate);
    }
  }

  @Override
  public void onModifyTodo(Todo todoToModify) {
	  dbLoader.updateTodo(todoToModify);
    updateTodoAdapter();

    if (isSetReminder(todoToModify)) {
      if (isNotCompleted(todoToModify) && isNotDeleted(todoToModify)) {
        createReminderService(todoToModify);
      }
    } else {
      cancelReminderService(todoToModify);
    }
  }

  @Override
  public void onSoftDelete(String onlineId, String type) {
    Todo todoToSoftDelete = dbLoader.getTodo(onlineId);
    dbLoader.softDeleteTodo(todoToSoftDelete);
    updateTodoAdapter();
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
    updateTodoAdapter();
    actionMode.finish();
  }

  private void createReminderService(Todo todo) {
    Intent reminderService = new Intent(getActivity(), AlarmService.class);
    reminderService.putExtra("todo", todo);
    reminderService.setAction(AlarmService.CREATE);
    getActivity().startService(reminderService);
  }

  private boolean isNotCompleted(Todo todo) {
    return !todo.getCompleted();
  }

  private boolean isSetReminder(Todo todo) {
    return !todo.getReminderDateTime().equals("-1");
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

  private boolean isPredefinedList(String listOnlineId) {
    return listOnlineId == null;
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

  private boolean isNotDeleted(Todo todo) {
    return !todo.getDeleted();
  }

  private void cancelReminderService(Todo todo) {
    Intent reminderService = new Intent(getActivity(), AlarmService.class);
    reminderService.putExtra("todo", todo);
    reminderService.setAction(AlarmService.CANCEL);
    getActivity().startService(reminderService);
  }

  public interface ITodoListFragment {
    void setActionBarTitle(String title);
    void startActionMode(ActionMode.Callback callback);
    void onTodoClicked(Todo clickedTodo, TodoListFragment targetFragment);
    void openTodoCreateFragment(TodoListFragment targetFragment);
  }

}

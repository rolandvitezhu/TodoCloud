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
    ITodoModifyFragment, ConfirmDeleteDialogFragment.IDeleteFragment {

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

      int checkedItemCount = getListView().getCheckedItemCount();
      actionMode.setTitle(
          checkedItemCount + " " + getString(R.string.selected));

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
      for (int i = 0; i < getListAdapter().getCount(); i++) {
        getListView().setItemChecked(i, false);
      }
      actionMode = null;
      getListView().setChoiceMode(AbsListView.CHOICE_MODE_NONE);
    }

  };

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
      // Todo módosítása.
      Todo todo = todoAdapter.getItem(position);
      listener.onTodoClicked(todo, this);
    } else {
      // ActionMode-hoz tartozó ActionBar beállítása.
      actionMode.invalidate();

      // Ha az utolsó kiválasztott elemet is kiválasztatlanná tesszük, akkor
      // ActionMode kikapcsolása.
      if (getListView().getCheckedItemCount() == 0) {
        actionMode.finish();
      }
      // TodoListItem kijelölése (Automatikusan történik, mivel az ActionMode aktív és a
      // ChoiceMode == AbsListView.CHOICE_MODE_MULTIPLE).
    }
  }

	@Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	  inflater.inflate(R.menu.todo_options_menu, menu);
  }

	@Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.itemCreateTodo) {
      // TodoCreateFragment megnyitása.
      listener.openTodoCreateFragment(this);
    }
	  return super.onOptionsItemSelected(item);
  }

  /**
   * Frissíti a TodoAdapter-t.
   */
  public void updateTodoAdapter() {
    if (todoAdapter == null) {
      todoAdapter = new TodoAdapter(new ArrayList<Todo>(), dbLoader, getActivity());
    }
    UpdateAdapterTask updateAdapterTask = new UpdateAdapterTask(dbLoader, todoAdapter);
    updateAdapterTask.execute(getArguments());
  }

  private View.OnClickListener fabClicked = new View.OnClickListener() {

    @Override
    public void onClick(View v) {
      actionMode.finish();
      listener.openTodoCreateFragment(TodoListFragment.this);
    }

  };

	/**
   * Létrehozza a megadott Todo-t.
   * @param todo A megadott Todo.
   */
	@Override
  public void onTodoCreated(Todo todo) {
    // Ha az "Elvégzettek" listán veszünk fel tennivalót, akkor az elvégzett lesz.
    // noinspection ConstantConditions
    if (getArguments().getString("selectFromDB") != null &&
    getArguments().getString("selectFromDB").equals(DbConstants.Todo.KEY_COMPLETED + "=" + 1 +
        " AND " + DbConstants.Todo.KEY_USER_ONLINE_ID + "='" + dbLoader.getUserOnlineId() + "'")) {
      todo.setCompleted(true);
    }
    String listOnlineId = getArguments().getString("listOnlineId");
    // Ha a listOnlineId == null, akkor valamelyik fő lista lett megnyitva, egyébként pedig
    // valamelyik egyedi lista, e szerint vesszük fel az adatbázisba az új tennivalót.
    if (listOnlineId == null) {
      todo.setUserOnlineId(dbLoader.getUserOnlineId());
      todo.set_id(dbLoader.createTodo(todo));
      todo.setTodoOnlineId(OnlineIdGenerator.generateOnlineId(getActivity(),
          DbConstants.Todo.DATABASE_TABLE, todo.get_id(), dbLoader.getApiKey()));
      dbLoader.updateTodo(todo);
    } else {
      todo.setUserOnlineId(dbLoader.getUserOnlineId());
      todo.setListOnlineId(listOnlineId);
      todo.set_id(dbLoader.createTodo(todo));
      todo.setTodoOnlineId(OnlineIdGenerator.generateOnlineId(getActivity(),
          DbConstants.Todo.DATABASE_TABLE, todo.get_id(), dbLoader.getApiKey()));
      dbLoader.updateTodo(todo);
    }
    updateTodoAdapter();
		todoAdapter.notifyDataSetChanged();

    // Emlékeztető élesítése, feltéve hogy lett beállítva és a todo nem elvégzett.
    if (!todo.getReminderDateTime().equals("-1") && !todo.getCompleted()) {
      Intent service = new Intent(getActivity(), AlarmService.class);
      service.putExtra("todo", todo);
      service.setAction(AlarmService.CREATE);
      getActivity().startService(service);
    }
  }

	/**
   * Módosítja a megadott Todo-t.
   * @param todo A megadott Todo.
   */
	@Override
  public void onTodoModified(Todo todo) {
	  dbLoader.updateTodo(todo);
    updateTodoAdapter();
    todoAdapter.notifyDataSetChanged();

    // Emlékeztető élesítése, feltéve hogy lett beállítva.
    // Egyébként emlékeztető törlése.
    if (!todo.getReminderDateTime().equals("-1")) {
      if (!todo.getCompleted() && !todo.getDeleted()) {
        // Emlékeztető élesítése, ha a Todo nem elvégzett és nem törölt.
        Intent service = new Intent(getActivity(), AlarmService.class);
        service.putExtra("todo", todo);
        service.setAction(AlarmService.CREATE);
        getActivity().startService(service);
      }
    } else {
      // Emlékeztető törlése.
      Intent service = new Intent(getActivity(), AlarmService.class);
      service.putExtra("todo", todo);
      service.setAction(AlarmService.CANCEL);
      getActivity().startService(service);
    }
  }

  /**
   * Törli a megadott onlineId-hoz tartozó Todo-t.
   * @param onlineId A törlendő Todo onlineId-ja.
   */
  @Override
  public void onDelete(String onlineId, String type) {
    Todo todo = dbLoader.getTodo(onlineId);
    dbLoader.softDeleteTodo(onlineId);
    updateTodoAdapter();

    // Emlékeztető törlése.
    Intent service = new Intent(getActivity(), AlarmService.class);
    service.putExtra("todo", todo);
    service.setAction(AlarmService.CANCEL);
    getActivity().startService(service);

    actionMode.finish();
  }

  /**
   * Törli a megadott Todo-kat.
   * @param items A megadott Todo-kat tartalmazó ArrayList.
   */
  @Override
  public void onDelete(ArrayList items, String type) {
    // Todo: Refactor the whole delete confirmation and deletion process. Rename the "items"
    // variable here and in the arguments also to "itemsToDelete".
    ArrayList<Todo> todos = items;
    for (Todo todo:todos) {
      dbLoader.softDeleteTodo(todo.getTodoOnlineId());

      // Emlékeztető törlése.
      Intent service = new Intent(getActivity(), AlarmService.class);
      service.putExtra("todo", todo);
      service.setAction(AlarmService.CANCEL);
      getActivity().startService(service);
    }
    updateTodoAdapter();

    actionMode.finish();
  }

  /**
   * Interfész a MainActivity-vel történő kommunikációra.
   */
  public interface ITodoListFragment {
    void setActionBarTitle(String title);
    void startActionMode(ActionMode.Callback callback);
    void onTodoClicked(Todo todo, TodoListFragment todoListFragment);
    void openTodoCreateFragment(TodoListFragment targetFragment);
  }

}

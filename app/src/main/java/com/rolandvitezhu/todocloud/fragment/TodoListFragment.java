package com.rolandvitezhu.todocloud.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
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

import com.github.clans.fab.FloatingActionButton;
import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.adapter.TodoAdapter;
import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.data.Todo;
import com.rolandvitezhu.todocloud.datastorage.DbConstants;
import com.rolandvitezhu.todocloud.datastorage.DbLoader;
import com.rolandvitezhu.todocloud.datastorage.asynctask.UpdateAdapterTask;
import com.rolandvitezhu.todocloud.dialog.SortTodoListDialog;
import com.rolandvitezhu.todocloud.helper.OnlineIdGenerator;
import com.rolandvitezhu.todocloud.listener.RecyclerViewOnItemTouchListener;
import com.rolandvitezhu.todocloud.receiver.ReminderSetter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class TodoListFragment extends Fragment implements
    CreateTodoFragment.ICreateTodoFragment,
    ModifyTodoFragment.IModifyTodoFragment,
    ConfirmDeleteDialogFragment.IConfirmDeleteDialogFragment,
    SortTodoListDialog.Presenter {

  @Inject
  DbLoader dbLoader;
  @Inject
  TodoAdapter todoAdapter;

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

    ((AppController) getActivity().getApplication()).getAppComponent().inject(this);
    todoAdapter.clear();
    updateTodoAdapter();
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState
  ) {
    View view = inflater.inflate(R.layout.fragment_todolist, container, false);
    prepareRecyclerView(view);
    applyClickEvents();
    applySwipeToDismiss();
    prepareFloatingActionButton(view);
    return view;
  }

  private void prepareFloatingActionButton(View view) {
    FloatingActionButton floatingActionButton =
        (FloatingActionButton) view.findViewById(R.id.floatingactionbutton_todolist);
    floatingActionButton.setOnClickListener(floatingActionButtonClicked);
  }

  private void prepareRecyclerView(View view) {
    recyclerView = (RecyclerView) view.findViewById(R.id.recyclerview_todolist);
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
        List<Todo> todos = todoAdapter.getTodos();
        int fromPosition = viewHolder.getAdapterPosition();
        int toPosition = target.getAdapterPosition();
        if (fromPosition < toPosition) {
          for (int i = fromPosition; i < toPosition; i++) {
            swapItems(todos, i, i + 1);
          }
        } else {
          for (int i = fromPosition; i > toPosition; i--) {
            swapItems(todos, i, i - 1);
          }
        }
        todoAdapter.notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());

//        if (target.getAdapterPosition() < viewHolder.getAdapterPosition()) {
//          todos.get(target.getAdapterPosition())
//        }

//        // TODO: implement reoder/persist reorder
//        // 0. DONE  Persist, when change the item's position value - set the item's dirty value to true
//        // 1. Order in an AsyncTask
//        // 2. The items, that changed it's position value should have a position value that is
//        // halfway between the previous and the next item's position values.
//        // 3. Implement more efficient order algorithm
//        boolean didOrderLastTime = false;
//        do {
//          didOrderLastTime = false;
//          for (int i = 0; i < todos.size()-1; i++) {
//            // Order, if should
//            if (todos.get(i).getPosition() >= todos.get(i+1).getPosition()) {
//              todos.get(i).setPosition(todos.get(i).getPosition() - 51);
//              todos.get(i).setDirty(true);
//              dbLoader.updateTodo(todos.get(i));
//              didOrderLastTime = true;
//            }
//          }
//        } while (didOrderLastTime); // If it's ordered correctly, then the ordering process is done

//        AsyncTask persistReorderAsyncTask = new AsyncTask() {
//          @Override
//          protected Object doInBackground(Object[] objects) {
//
//
//            return null;
//          }
//        }.execute();

        return true;
      }

      @Override
      public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        Todo swipedTodo = getSwipedTodo(viewHolder);
        int swipedTodoAdapterPosition = viewHolder.getAdapterPosition();
        openConfirmDeleteTodosDialog(swipedTodo, swipedTodoAdapterPosition);
      }

      @Override
      public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags;
        int swipeFlags;
        if (AppController.isActionMode()) {
          dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
          swipeFlags = 0;
        }
        else {
          dragFlags = 0;
          swipeFlags = ItemTouchHelper.START;
        }
        return makeMovementFlags(dragFlags, swipeFlags);
      }

      @Override
      public boolean isLongPressDragEnabled() {
        return false;
      }
    };
    ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
    itemTouchHelper.attachToRecyclerView(recyclerView);
    todoAdapter.setItemTouchHelper(itemTouchHelper);
  }

  private void swapItems(List<Todo> todos, int fromPosition, int toPosition) {
    // Change todo position values
    Todo todoFrom = todos.get(fromPosition);
    Todo todoTo = todos.get(toPosition);
    int tempTodoToPosition = todoFrom.getPosition();
    todoFrom.setPosition(todoTo.getPosition());
    todoTo.setPosition(tempTodoToPosition);
    todoFrom.setDirty(true);
    todoTo.setDirty(true);
    dbLoader.updateTodo(todoFrom);
    dbLoader.updateTodo(todoTo);
    Collections.swap(todos, fromPosition, toPosition);
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
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.fragment_todolist, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int menuItemId = item.getItemId();

    switch (menuItemId) {
      case R.id.menuitem_todolist_sort:
        new SortTodoListDialog(this.getActivity(), this);
        break;
    }

    return super.onOptionsItemSelected(item);
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
            listener.onSetActionBarTitle(getString(R.string.all_today));
            break;
          case "1":
            listener.onSetActionBarTitle(getString(R.string.all_next7days));
            break;
          case "2":
            listener.onSetActionBarTitle(getString(R.string.all_all));
            break;
          case "3":
            listener.onSetActionBarTitle(getString(R.string.all_completed));
            break;
        }
      }
    }
  }

  private ActionMode.Callback callback = new ActionMode.Callback() {

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
      setActionMode(mode);
      mode.getMenuInflater().inflate(R.menu.layout_appbar_todolist, menu);

      return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
      String title = prepareTitle();
      actionMode.setTitle(title);
      todoAdapter.notifyDataSetChanged();

      return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
      int actionItemId = item.getItemId();

      switch (actionItemId) {
        case R.id.menuitem_layoutappbartodolist_delete:
          openConfirmDeleteTodosDialog();
          break;
      }

      return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
      todoAdapter.clearSelection();
      setActionMode(null);
      todoAdapter.notifyDataSetChanged();
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

  private View.OnClickListener floatingActionButtonClicked = new View.OnClickListener() {

    @Override
    public void onClick(View v) {
      if (isActionMode()) actionMode.finish();
      listener.onOpenCreateTodoFragment(TodoListFragment.this);
    }

  };

  private void updateTodoAdapter() {
    UpdateAdapterTask updateAdapterTask = new UpdateAdapterTask(todoAdapter);
    updateAdapterTask.execute(getArguments());
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

  @Override
  public void onSortByDueDatePushed() {
    new SortAsyncTask(todoAdapter).execute(SortAsyncTask.SORT_BY_DUE_DATE);
  }

  @Override
  public void onSortByPriorityPushed() {
    new SortAsyncTask(todoAdapter).execute(SortAsyncTask.SORT_BY_PRIORITY);
  }

  private static class SortAsyncTask extends AsyncTask<Integer, Long, Void> {

    public static final int SORT_BY_DUE_DATE = 1001;
    public static final int SORT_BY_PRIORITY = 1002;
    
    private WeakReference<TodoAdapter> todoAdapterWeakReference;

    SortAsyncTask(TodoAdapter context) {
      todoAdapterWeakReference = new WeakReference<>(context);
    }

    @Override
    protected Void doInBackground(Integer... params) {
      switch (params[0]) {
        case SORT_BY_DUE_DATE:
          todoAdapterWeakReference.get().sortByDueDate();
          break;
        case SORT_BY_PRIORITY:
          todoAdapterWeakReference.get().sortByPriority();
          break;
      }

      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      super.onPostExecute(aVoid);
      TodoAdapter todoAdapter = todoAdapterWeakReference.get();
      if (todoAdapter == null) return;

      todoAdapter.notifyDataSetChanged();
    }
  }

  public interface ITodoListFragment {
    void onSetActionBarTitle(String actionBarTitle);
    void onStartActionMode(ActionMode.Callback callback);
    void onClickTodo(Todo todo, Fragment targetFragment);
    void onOpenCreateTodoFragment(TodoListFragment targetFragment);
  }

}

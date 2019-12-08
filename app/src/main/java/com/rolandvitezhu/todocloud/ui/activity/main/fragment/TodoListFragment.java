package com.rolandvitezhu.todocloud.ui.activity.main.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.data.Todo;
import com.rolandvitezhu.todocloud.datastorage.DbLoader;
import com.rolandvitezhu.todocloud.datastorage.asynctask.UpdateViewModelTask;
import com.rolandvitezhu.todocloud.listener.RecyclerViewOnItemTouchListener;
import com.rolandvitezhu.todocloud.receiver.ReminderSetter;
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity;
import com.rolandvitezhu.todocloud.ui.activity.main.adapter.TodoAdapter;
import com.rolandvitezhu.todocloud.ui.activity.main.dialog.SortTodoListDialog;
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.ConfirmDeleteDialogFragment;
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.ListsViewModel;
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.PredefinedListsViewModel;
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.TodosViewModel;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class TodoListFragment extends Fragment implements SortTodoListDialog.Presenter {

  @Inject
  DbLoader dbLoader;
  @Inject
  TodoAdapter todoAdapter;

  @BindView(R.id.recyclerview_todolist)
  RecyclerView recyclerView;

  private ActionMode actionMode;

  private TodosViewModel todosViewModel;
  private ListsViewModel listsViewModel;
  private PredefinedListsViewModel predefinedListsViewModel;

  Unbinder unbinder;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);

    ((AppController) getActivity().getApplication()).getAppComponent().inject(this);

    todosViewModel = ViewModelProviders.of(getActivity()).get(TodosViewModel.class);
    listsViewModel = ViewModelProviders.of(getActivity()).get(ListsViewModel.class);
    predefinedListsViewModel = ViewModelProviders.of(getActivity()).get(PredefinedListsViewModel.class);

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

    updateTodosViewModel();
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState
  ) {
    View view = inflater.inflate(R.layout.fragment_todolist, container, false);
    unbinder = ButterKnife.bind(this, view);

    prepareRecyclerView(view);
    applyClickEvents();
    applySwipeToDismissAndDragToReorder();

    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    setActionBarTitle();
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
              ((MainActivity)TodoListFragment.this.getActivity()).onStartActionMode(callback);
              todoAdapter.toggleSelection(childViewAdapterPosition);
              actionMode.invalidate();
            }
          }

        }
        )
    );
  }

  private void applySwipeToDismissAndDragToReorder() {
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
    dbLoader.fixTodoPositions();
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
    todosViewModel.setTodo(todo);

    ((MainActivity)getActivity()).openModifyTodoFragment(this);
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
        new SortTodoListDialog(getActivity(), this);
        break;
    }

    return super.onOptionsItemSelected(item);
  }

  private void setActionBarTitle() {
    String actionBarTitle = "";

    if (!todosViewModel.isPredefinedList()) // List
      actionBarTitle = listsViewModel.getList().getTitle();
    else // PredefinedList
      actionBarTitle = predefinedListsViewModel.getPredefinedList().getTitle();

    ((MainActivity)getActivity()).onSetActionBarTitle(actionBarTitle);
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

  private void updateTodosViewModel() {
    UpdateViewModelTask updateViewModelTask = new UpdateViewModelTask(todosViewModel, getActivity());
    updateViewModelTask.execute();
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

  @Override
  public void onSortByDueDatePushed() {
    new SortAsyncTask(todoAdapter).execute(SortAsyncTask.SORT_BY_DUE_DATE);
  }

  @Override
  public void onSortByPriorityPushed() {
    new SortAsyncTask(todoAdapter).execute(SortAsyncTask.SORT_BY_PRIORITY);
  }

  @OnClick(R.id.floatingactionbutton_todolist)
  public void onFABClick(View view) {
    if (isActionMode()) actionMode.finish();
    ((MainActivity)TodoListFragment.this.getActivity()).
        onOpenCreateTodoFragment(TodoListFragment.this);
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

}

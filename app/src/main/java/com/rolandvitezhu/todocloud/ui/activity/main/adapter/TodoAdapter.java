package com.rolandvitezhu.todocloud.ui.activity.main.adapter;

import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.data.Todo;
import com.rolandvitezhu.todocloud.datastorage.DbLoader;
import com.rolandvitezhu.todocloud.helper.SharedPreferencesHelper;
import com.rolandvitezhu.todocloud.receiver.ReminderSetter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.ItemViewHolder> {

  @Inject
  DbLoader dbLoader;

  private List<Todo> todos;
  private ItemTouchHelper itemTouchHelper;

  public TodoAdapter() {
    AppController.getInstance().getAppComponent().inject(this);
    todos = new ArrayList<>();
  }

  public void update(ArrayList<Todo> todos) {
    this.todos.clear();

    // Order todo list items ascending by position value
    Collections.sort(todos, new Comparator<Todo>() {
      @Override
      public int compare(Todo o1, Todo o2) {
        return o1.getPosition() - o2.getPosition();
      }
    });

    this.todos.addAll(todos);
  }

  public void sortByDueDate() {
    ArrayList<Todo> originalTodos = new ArrayList<>();

    boolean isSortByDueDateAsc = SharedPreferencesHelper.getPreference(
        SharedPreferencesHelper.PREFERENCE_NAME_SORT,
        SharedPreferencesHelper.KEY_SORT_BY_DUE_DATE_ASC
    );

    // Deep copy todos
    for (int i = 0; i < todos.size(); i++) {
      originalTodos.add(new Todo(todos.get(i)));
    }

    if (isSortByDueDateAsc) {
      // Order todo list items ascending by due date
      Collections.sort(todos, new Comparator<Todo>() {
        @Override
        public int compare(Todo o1, Todo o2) {
          return o1.getDueDate() > o2.getDueDate() ? 1 : (o1.getDueDate() < o2.getDueDate()) ? -1 : 0;
        }
      });
    } else {
      // Order todo list items descending by due date
      Collections.sort(todos, new Comparator<Todo>() {
        @Override
        public int compare(Todo o1, Todo o2) {
          return o1.getDueDate() > o2.getDueDate() ? -1 : (o1.getDueDate() < o2.getDueDate()) ? 1 : 0;
        }
      });
    }

    // Invert pref value
    SharedPreferencesHelper.setBooleanPreference(
        SharedPreferencesHelper.PREFERENCE_NAME_SORT,
        SharedPreferencesHelper.KEY_SORT_BY_DUE_DATE_ASC,
        !isSortByDueDateAsc
    );

    // Fix position values
    for (int i = 0; i < todos.size(); i++) {
      int rightPosition = originalTodos.get(i).getPosition();
      todos.get(i).setPosition(rightPosition);
      dbLoader.updateTodo(todos.get(i));
    }
    dbLoader.fixTodoPositions();
  }

  public void sortByPriority() {
    ArrayList<Todo> originalTodos = new ArrayList<>();

    boolean isSortByPriority = SharedPreferencesHelper.getPreference(
        SharedPreferencesHelper.PREFERENCE_NAME_SORT,
        SharedPreferencesHelper.KEY_SORT_BY_PRIORITY
    );

    // Deep copy todos
    for (int i = 0; i < todos.size(); i++) {
      originalTodos.add(new Todo(todos.get(i)));
    }

    if (isSortByPriority) {
      // Order todo list items by priority
      Collections.sort(todos, new Comparator<Todo>() {
        @Override
        public int compare(Todo o1, Todo o2) {
          return !o1.isPriority() && o2.isPriority() ? 1 : (o1.isPriority() && !o2.isPriority()) ? -1 : 0;
        }
      });
    } else {
      // Order todo list items by not priority
      Collections.sort(todos, new Comparator<Todo>() {
        @Override
        public int compare(Todo o1, Todo o2) {
          return o1.isPriority() && !o2.isPriority() ? 1 : (!o1.isPriority() && o2.isPriority()) ? -1 : 0;
        }
      });
    }

    // Invert pref value
    SharedPreferencesHelper.setBooleanPreference(
        SharedPreferencesHelper.PREFERENCE_NAME_SORT,
        SharedPreferencesHelper.KEY_SORT_BY_PRIORITY,
        !isSortByPriority
    );

    // Fix position values
    for (int i = 0; i < todos.size(); i++) {
      int rightPosition = originalTodos.get(i).getPosition();
      todos.get(i).setPosition(rightPosition);
      dbLoader.updateTodo(todos.get(i));
    }
    dbLoader.fixTodoPositions();
  }

  public void clear() {
    todos.clear();
    notifyDataSetChanged();
  }

  public void setItemTouchHelper(ItemTouchHelper itemTouchHelper) {
    this.itemTouchHelper = itemTouchHelper;
  }

  public List<Todo> getTodos() {
    return todos;
  }

  @Override
  public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View itemView = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.item_todo, parent, false);

    return new ItemViewHolder(itemView);
  }

  @Override
  public void onBindViewHolder(final ItemViewHolder holder, int position) {
    final Todo todo = todos.get(position);

    holder.cbCompleted.setChecked(todo.isCompleted());
    holder.tvTitle.setText(todo.getTitle());
    holder.tvDueDate.setText(todo.getFormattedDueDateForListItem());
    holder.ivPriority.setVisibility(todo.isPriority() ? View.VISIBLE : View.GONE);
    holder.ivDragHandle.setVisibility(AppController.isActionMode() ? View.VISIBLE : View.GONE);

    holder.ivDragHandle.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
          itemTouchHelper.startDrag(holder);
        }

        return false;
      }
    });

    holder.itemView.setActivated(todos.get(position).isSelected());
    holder.cbCompleted.setOnTouchListener(new View.OnTouchListener() {

      @Override
      public boolean onTouch(View v, MotionEvent event) {
        if (shouldHandleCheckBoxTouchEvent(event, holder)) {
          toggleCompleted(todo);
          updateTodo(todo);
          removeTodoFromAdapter(holder);
          handleReminderService(todo);
        }

        return true;
      }

    });
  }

  private void toggleCompleted(Todo todo) {
    todo.setCompleted(!todo.isCompleted());
  }

  private void updateTodo(Todo todo) {
    todo.setDirty(true);
    dbLoader.updateTodo(todo);
    dbLoader.fixTodoPositions();
  }

  public void removeTodoFromAdapter(ItemViewHolder holder) {
    int todoAdapterPosition = holder.getAdapterPosition();
    todos.remove(todoAdapterPosition);
    notifyItemRemoved(todoAdapterPosition);
  }

  private void handleReminderService(Todo todo) {
    if (todo.isCompleted()) {
      ReminderSetter.cancelReminderService(todo);
    } else if (isSetReminder(todo)) {
      ReminderSetter.createReminderService(todo);
    }
  }

  private boolean shouldHandleCheckBoxTouchEvent(MotionEvent event, ItemViewHolder holder) {
    // To reproduce "holder.getAdapterPosition() == -1", do the following: select 1 todo and
    // touch it's CheckBox.
    return !AppController.isActionMode()
        && event.getAction() == MotionEvent.ACTION_UP
        && holder.getAdapterPosition() != -1;
  }

  private boolean isSetReminder(Todo todo) {
    return !todo.getReminderDateTime().equals("-1");
  }

  @Override
  public int getItemCount() {
    return todos.size();
  }

  public Todo getTodo(int position) {
    return todos.get(position);
  }

  public void toggleSelection(int position) {
    if (isNotSelected(position)) {
      todos.get(position).setSelected(true);
    } else {
      todos.get(position).setSelected(false);
    }
    notifyItemChanged(position);
  }

  public void clearSelection() {
    for (Todo todo : todos) {
      todo.setSelected(false);
    }

    notifyDataSetChanged();
  }

  public int getSelectedItemCount() {
    int selectedItemCount = 0;
    for (Todo todo : todos) {
      if (todo.isSelected())
        selectedItemCount++;
    }
    return selectedItemCount;
  }

  public ArrayList<Todo> getSelectedTodos() {
    Todo selectedTodo;
    ArrayList<Todo> selectedTodos = new ArrayList<>(getSelectedItemCount());
    for (Todo todo : todos) {
      if (todo.isSelected()) selectedTodos.add(todo);
    }
    return selectedTodos;
  }

  private boolean isNotSelected(int position) {
    return !todos.get(position).isSelected();
  }

  public class ItemViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.checkbox_todo_completed)
    AppCompatCheckBox cbCompleted;
    @BindView(R.id.textview_todo_title)
    TextView tvTitle;
    @BindView(R.id.textview_todo_duedate)
    TextView tvDueDate;
    @BindView(R.id.imageview_todo_priority)
    ImageView ivPriority;
    @BindView(R.id.imageview_todo_draghandle)
    ImageView ivDragHandle;

    public ItemViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

  }

}

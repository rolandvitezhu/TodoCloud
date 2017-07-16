package com.example.todocloud.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.todocloud.R;
import com.example.todocloud.app.AppController;
import com.example.todocloud.data.Todo;
import com.example.todocloud.datastorage.DbLoader;
import com.example.todocloud.service.AlarmService;

import java.util.ArrayList;
import java.util.List;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.MyViewHolder> {

  private List<Todo> todos;
  private SparseBooleanArray selectedTodos;
  private DbLoader dbLoader;

  public TodoAdapter(DbLoader dbLoader) {
    todos = new ArrayList<>();
    selectedTodos = new SparseBooleanArray();
    this.dbLoader = dbLoader;
  }

  public void updateDataSet(ArrayList<Todo> todosList) {
    this.todos.clear();
    this.todos.addAll(todosList);
  }

  @Override
  public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View itemView = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.todo_item, parent, false);

    return new MyViewHolder(itemView);
  }

  @Override
  public void onBindViewHolder(final MyViewHolder holder, int position) {
    final Todo todo = todos.get(position);

    holder.completed.setChecked(todo.getCompleted());
    holder.title.setText(todo.getTitle());
    holder.dueDate.setText(todo.getDueDate());
    holder.priority.setVisibility(todo.isPriority() ? View.VISIBLE : View.INVISIBLE);

    holder.itemView.setActivated(selectedTodos.get(position));
    holder.completed.setOnTouchListener(new View.OnTouchListener() {

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
    todo.setCompleted(!todo.getCompleted());
  }

  private void updateTodo(Todo todo) {
    todo.setDirty(true);
    dbLoader.updateTodo(todo);
  }

  public void removeTodoFromAdapter(MyViewHolder holder) {
    int todoAdapterPosition = holder.getAdapterPosition();
    todos.remove(todoAdapterPosition);
    notifyItemRemoved(todoAdapterPosition);
  }

  private void handleReminderService(Todo todo) {
    if (isCompleted(todo)) {
      cancelReminderService(todo);
    } else if (isSetReminder(todo)) {
      createReminderService(todo);
    }
  }

  private boolean shouldHandleCheckBoxTouchEvent(MotionEvent event, MyViewHolder holder) {
    // To reproduce "holder.getAdapterPosition() == -1", do the following: select 1 todo and
    // touch it's CheckBox.
    return !AppController.isActionMode()
        && event.getAction() == MotionEvent.ACTION_UP
        && holder.getAdapterPosition() != -1;
  }

  private void cancelReminderService(Todo todo) {
    Context applicationContext = AppController.getAppContext();
    Intent reminderService = new Intent(applicationContext, AlarmService.class);
    reminderService.putExtra("todo", todo);
    reminderService.setAction(AlarmService.CANCEL);
    applicationContext.startService(reminderService);
  }

  private boolean isSetReminder(Todo todo) {
    return !todo.getReminderDateTime().equals("-1");
  }

  private void createReminderService(Todo todo) {
    Context applicationContext = AppController.getAppContext();
    Intent reminderService = new Intent(applicationContext, AlarmService.class);
    reminderService.putExtra("todo", todo);
    reminderService.setAction(AlarmService.CREATE);
    applicationContext.startService(reminderService);
  }

  private Boolean isCompleted(Todo todo) {
    return todo.getCompleted();
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
      selectedTodos.put(position, true);
    } else {
      selectedTodos.delete(position);
    }
    notifyItemChanged(position);
  }

  public void clearSelection() {
    selectedTodos.clear();
    notifyDataSetChanged();
  }

  public int getSelectedItemCount() {
    return selectedTodos.size();
  }

  public ArrayList<Todo> getSelectedTodos() {
    Todo selectedTodo;
    ArrayList<Todo> selectedTodos = new ArrayList<>(this.selectedTodos.size());
    for (int i = 0; i < this.selectedTodos.size(); i++) {
      int selectedTodoPosition = this.selectedTodos.keyAt(i);
      selectedTodo = getTodo(selectedTodoPosition);
      selectedTodos.add(selectedTodo);
    }
    return selectedTodos;
  }

  private boolean isNotSelected(int position) {
    return !selectedTodos.get(position);
  }

  public class MyViewHolder extends RecyclerView.ViewHolder {

    public AppCompatCheckBox completed;
    public TextView title;
    public TextView dueDate;
    public ImageView priority;

    public MyViewHolder(View itemView) {
      super(itemView);
      completed = (AppCompatCheckBox) itemView.findViewById(R.id.completed);
      title = (TextView) itemView.findViewById(R.id.title);
      dueDate = (TextView) itemView.findViewById(R.id.dueDate);
      priority = (ImageView) itemView.findViewById(R.id.priority);
    }

  }

}

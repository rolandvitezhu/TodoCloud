package com.example.todocloud.adapter;

import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.todocloud.R;
import com.example.todocloud.data.Todo;

import java.util.ArrayList;
import java.util.List;

public class TodoAdapterTest extends RecyclerView.Adapter<TodoAdapterTest.MyViewHolder> {

  private List<Todo> todos;
  private SparseBooleanArray selectedTodos;

  public TodoAdapterTest() {
    todos = new ArrayList<>();
    selectedTodos = new SparseBooleanArray();
  }

  public void updateDataSet(ArrayList<Todo> todosList) {
    this.todos.clear();
    this.todos.addAll(todosList);
  }

  @Override
  public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View itemView = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.todo_item_test, parent, false);

    return new MyViewHolder(itemView);
  }

  @Override
  public void onBindViewHolder(MyViewHolder holder, int position) {
    Todo todo = todos.get(position);

    holder.completed.setChecked(todo.getCompleted());
    holder.title.setText(todo.getTitle());
    holder.dueDate.setText(todo.getDueDate());
    holder.priority.setVisibility(todo.isPriority() ? View.VISIBLE : View.INVISIBLE);

    holder.itemView.setActivated(selectedTodos.get(position));
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

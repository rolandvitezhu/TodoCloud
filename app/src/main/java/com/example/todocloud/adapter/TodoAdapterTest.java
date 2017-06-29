package com.example.todocloud.adapter;

import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.RecyclerView;
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

  private List<Todo> todosList;

  public TodoAdapterTest() {
    todosList = new ArrayList<>();
  }

  public void updateDataSet(ArrayList<Todo> todosList) {
    this.todosList.clear();
    this.todosList.addAll(todosList);
  }

  @Override
  public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View itemView = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.todo_item_test, parent, false);

    return new MyViewHolder(itemView);
  }

  @Override
  public void onBindViewHolder(MyViewHolder holder, int position) {
    Todo todo = todosList.get(position);
    holder.completed.setChecked(todo.getCompleted());
    holder.title.setText(todo.getTitle());
    holder.dueDate.setText(todo.getDueDate());
    holder.priority.setVisibility(todo.isPriority() ? View.VISIBLE : View.INVISIBLE);
  }

  @Override
  public int getItemCount() {
    return todosList.size();
  }

  public Todo getItem(int position) {
    return todosList.get(position);
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

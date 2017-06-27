package com.example.todocloud.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.todocloud.R;
import com.example.todocloud.data.Todo;

import java.util.List;

public class TodoAdapterTest extends RecyclerView.Adapter<TodoAdapterTest.MyViewHolder> {

  private List<Todo> todosList;

  public TodoAdapterTest(List<Todo> todosList) {
    this.todosList = todosList;
  }

  @Override
  public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View itemView = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.todo_item, parent, false);

    return new MyViewHolder(itemView);
  }

  @Override
  public void onBindViewHolder(MyViewHolder holder, int position) {
    Todo todo = todosList.get(position);
    holder.title.setText(todo.getTitle());
  }

  @Override
  public int getItemCount() {
    return todosList.size();
  }

  public class MyViewHolder extends RecyclerView.ViewHolder {

    public TextView title;

    public MyViewHolder(View itemView) {
      super(itemView);
      title = (TextView) itemView.findViewById(R.id.tvTitle);
    }

  }

}

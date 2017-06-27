package com.example.todocloud.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.todocloud.R;
import com.example.todocloud.adapter.TodoAdapterTest;
import com.example.todocloud.data.Todo;

import java.util.ArrayList;
import java.util.List;

public class TodoListFragmentTest extends Fragment {

  private List<Todo> todosList = new ArrayList<>();
  private TodoAdapterTest todoAdapterTest;
  private RecyclerView recyclerView;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    todoAdapterTest = new TodoAdapterTest(todosList);
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
    return view;
  }

  private void updateTodoAdapterTest() {
    Todo todo = new Todo(1, "test", "test", "test", "Test1", false,
        "test", "test", "test", false, 1, false, false);
    todosList.add(todo);

    todo = new Todo(1, "test", "test", "test", "Test2", false,
        "test", "test", "test", false, 1, false, false);
    todosList.add(todo);

    todo = new Todo(1, "test", "test", "test", "Test3", false,
        "test", "test", "test", false, 1, false, false);
    todosList.add(todo);

    todoAdapterTest.notifyDataSetChanged();
  }

}

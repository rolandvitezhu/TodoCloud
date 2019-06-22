package com.rolandvitezhu.todocloud.ui.activity.main.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import com.rolandvitezhu.todocloud.data.Todo;

import java.util.ArrayList;

public class TodosViewModel extends ViewModel {

  private MutableLiveData<ArrayList<Todo>> todos;
  private Todo todo;
  private boolean isPredefinedList;

  public LiveData<ArrayList<Todo>> getTodos() {
    if (todos == null)
      todos = new MutableLiveData<>();

    return todos;
  }

  public void setTodos(ArrayList<Todo> todos) {
    if (this.todos == null)
      this.todos = new MutableLiveData<>();

    this.todos.setValue(todos);
  }

  public void clearTodos() {
    if (this.todos != null)
      todos.setValue(new ArrayList<>());
  }

  public void setTodo(Todo todo) {
    this.todo = todo;
  }

  public Todo getTodo() {
    return todo;
  }

  public boolean isPredefinedList() {
    return isPredefinedList;
  }

  public void setIsPredefinedList(boolean isPredefinedList) {
    this.isPredefinedList = isPredefinedList;
  }
}

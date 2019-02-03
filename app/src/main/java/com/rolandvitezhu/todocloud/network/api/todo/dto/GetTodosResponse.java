package com.rolandvitezhu.todocloud.network.api.todo.dto;

import com.google.gson.annotations.SerializedName;
import com.rolandvitezhu.todocloud.data.Todo;
import com.rolandvitezhu.todocloud.network.api.dto.BaseResponse;

import java.util.ArrayList;

public class GetTodosResponse extends BaseResponse {

  @SerializedName("todos")
  public ArrayList<Todo> todos;

  public ArrayList<Todo> getTodos() {
    return todos;
  }

  public void setTodos(ArrayList<Todo> todos) {
    this.todos = todos;
  }
}

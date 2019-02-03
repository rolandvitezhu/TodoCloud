package com.rolandvitezhu.todocloud.network.api.todo.service;

import com.rolandvitezhu.todocloud.network.api.todo.dto.InsertTodoRequest;
import com.rolandvitezhu.todocloud.network.api.todo.dto.InsertTodoResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface InsertTodoService {

  @POST("todo_cloud/v1/todo/insert")
  Call<InsertTodoResponse> insertTodo(
      @Header("authorization") String apiKey, @Body InsertTodoRequest request
  );
}

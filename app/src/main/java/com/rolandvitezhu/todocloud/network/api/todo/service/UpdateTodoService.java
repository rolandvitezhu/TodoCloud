package com.rolandvitezhu.todocloud.network.api.todo.service;

import com.rolandvitezhu.todocloud.network.api.todo.dto.UpdateTodoRequest;
import com.rolandvitezhu.todocloud.network.api.todo.dto.UpdateTodoResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface UpdateTodoService {

  // 000webhost.com doesn't allow PUT and DELETE requests for free accounts
  @POST("todo_cloud/v1/todo/update")
  Call<UpdateTodoResponse> updateTodo(
      @Header("authorization") String apiKey, @Body UpdateTodoRequest request
  );
}

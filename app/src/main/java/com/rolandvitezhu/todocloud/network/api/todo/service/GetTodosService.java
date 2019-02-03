package com.rolandvitezhu.todocloud.network.api.todo.service;

import com.rolandvitezhu.todocloud.network.api.todo.dto.GetTodosResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface GetTodosService {

  @GET("todo_cloud/v1/todo/{row_version}")
  Call<GetTodosResponse> getTodos(
      @Path("row_version") int rowVersion, @Header("authorization") String apiKey
  );
}

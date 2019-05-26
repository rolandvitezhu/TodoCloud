package com.rolandvitezhu.todocloud.network;

import com.rolandvitezhu.todocloud.network.api.category.dto.GetCategoriesResponse;
import com.rolandvitezhu.todocloud.network.api.category.dto.InsertCategoryRequest;
import com.rolandvitezhu.todocloud.network.api.category.dto.InsertCategoryResponse;
import com.rolandvitezhu.todocloud.network.api.category.dto.UpdateCategoryRequest;
import com.rolandvitezhu.todocloud.network.api.category.dto.UpdateCategoryResponse;
import com.rolandvitezhu.todocloud.network.api.list.dto.GetListsResponse;
import com.rolandvitezhu.todocloud.network.api.list.dto.InsertListRequest;
import com.rolandvitezhu.todocloud.network.api.list.dto.InsertListResponse;
import com.rolandvitezhu.todocloud.network.api.list.dto.UpdateListRequest;
import com.rolandvitezhu.todocloud.network.api.list.dto.UpdateListResponse;
import com.rolandvitezhu.todocloud.network.api.todo.dto.GetTodosResponse;
import com.rolandvitezhu.todocloud.network.api.todo.dto.InsertTodoRequest;
import com.rolandvitezhu.todocloud.network.api.todo.dto.InsertTodoResponse;
import com.rolandvitezhu.todocloud.network.api.todo.dto.UpdateTodoRequest;
import com.rolandvitezhu.todocloud.network.api.todo.dto.UpdateTodoResponse;
import com.rolandvitezhu.todocloud.network.api.user.dto.LoginUserRequest;
import com.rolandvitezhu.todocloud.network.api.user.dto.LoginUserResponse;
import com.rolandvitezhu.todocloud.network.api.user.dto.ModifyPasswordRequest;
import com.rolandvitezhu.todocloud.network.api.user.dto.ModifyPasswordResponse;
import com.rolandvitezhu.todocloud.network.api.user.dto.RegisterUserRequest;
import com.rolandvitezhu.todocloud.network.api.user.dto.RegisterUserResponse;
import com.rolandvitezhu.todocloud.network.api.user.dto.ResetPasswordRequest;
import com.rolandvitezhu.todocloud.network.api.user.dto.ResetPasswordResponse;

import io.reactivex.Single;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

  @POST("todo_cloud/v1/user/register")
  Single<RegisterUserResponse> registerUser(@Body RegisterUserRequest request);

  @POST("todo_cloud/v1/user/login")
  Single<LoginUserResponse> loginUser(@Body LoginUserRequest request);

  @POST("todo_cloud/v1/user/reset_password")
  Single<ResetPasswordResponse> resetPassword(@Body ResetPasswordRequest request);

  @POST("todo_cloud/v1/user/modify_password")
  Single<ModifyPasswordResponse> modifyPassword(@Body ModifyPasswordRequest request);

  @GET("todo_cloud/v1/todo/{row_version}")
  Single<GetTodosResponse> getTodos(@Path("row_version") int rowVersion);

  @POST("todo_cloud/v1/todo/insert")
  Single<InsertTodoResponse> insertTodo(@Body InsertTodoRequest request);

  // 000webhost.com doesn't allow PUT and DELETE requests for free accounts
  @POST("todo_cloud/v1/todo/update")
  Single<UpdateTodoResponse> updateTodo(@Body UpdateTodoRequest request);

  @GET("todo_cloud/v1/list/{row_version}")
  Single<GetListsResponse> getLists(@Path("row_version") int rowVersion);

  @POST("todo_cloud/v1/list/insert")
  Single<InsertListResponse> insertList(@Body InsertListRequest request);

  // 000webhost.com doesn't allow PUT and DELETE requests for free accounts
  @POST("todo_cloud/v1/list/update")
  Single<UpdateListResponse> updateList(@Body UpdateListRequest request);

  @GET("todo_cloud/v1/category/{row_version}")
  Single<GetCategoriesResponse> getCategories(@Path("row_version") int rowVersion);

  @POST("todo_cloud/v1/category/insert")
  Single<InsertCategoryResponse> insertCategory(@Body InsertCategoryRequest request);

  // 000webhost.com doesn't allow PUT and DELETE requests for free accounts
  @POST("todo_cloud/v1/category/update")
  Single<UpdateCategoryResponse> updateCategory(@Body UpdateCategoryRequest request);
}

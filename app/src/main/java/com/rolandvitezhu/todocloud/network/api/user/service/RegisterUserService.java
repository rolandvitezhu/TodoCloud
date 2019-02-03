package com.rolandvitezhu.todocloud.network.api.user.service;

import com.rolandvitezhu.todocloud.network.api.user.dto.RegisterUserRequest;
import com.rolandvitezhu.todocloud.network.api.user.dto.RegisterUserResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RegisterUserService {

  @POST("todo_cloud/v1/user/register")
  Call<RegisterUserResponse> registerUser(@Body RegisterUserRequest request);
}

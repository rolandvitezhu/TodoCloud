package com.rolandvitezhu.todocloud.network.api.user.service;

import com.rolandvitezhu.todocloud.network.api.user.dto.LoginUserRequest;
import com.rolandvitezhu.todocloud.network.api.user.dto.LoginUserResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface LoginUserService {

  @POST("todo_cloud/v1/user/login")
  Call<LoginUserResponse> loginUser(@Body LoginUserRequest request);
}

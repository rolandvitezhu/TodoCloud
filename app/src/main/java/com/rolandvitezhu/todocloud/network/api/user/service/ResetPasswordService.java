package com.rolandvitezhu.todocloud.network.api.user.service;

import com.rolandvitezhu.todocloud.network.api.user.dto.ResetPasswordRequest;
import com.rolandvitezhu.todocloud.network.api.user.dto.ResetPasswordResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ResetPasswordService {

  @POST("todo_cloud/v1/user/reset_password")
  Call<ResetPasswordResponse> resetPassword(@Body ResetPasswordRequest request);
}

package com.rolandvitezhu.todocloud.network.api.user.service;

import com.rolandvitezhu.todocloud.network.api.user.dto.ModifyPasswordRequest;
import com.rolandvitezhu.todocloud.network.api.user.dto.ModifyPasswordResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ModifyPasswordService {

  @POST("todo_cloud/v1/user/modify_password")
  Call<ModifyPasswordResponse> modifyPassword(
      @Header("authorization") String apiKey, @Body ModifyPasswordRequest request
  );
}

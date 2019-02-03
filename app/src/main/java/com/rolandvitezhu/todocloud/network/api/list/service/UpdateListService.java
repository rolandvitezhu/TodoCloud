package com.rolandvitezhu.todocloud.network.api.list.service;

import com.rolandvitezhu.todocloud.network.api.list.dto.UpdateListRequest;
import com.rolandvitezhu.todocloud.network.api.list.dto.UpdateListResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface UpdateListService {

  // 000webhost.com doesn't allow PUT and DELETE requests for free accounts
  @POST("todo_cloud/v1/list/update")
  Call<UpdateListResponse> updateList(
      @Header("authorization") String apiKey, @Body UpdateListRequest request
  );
}

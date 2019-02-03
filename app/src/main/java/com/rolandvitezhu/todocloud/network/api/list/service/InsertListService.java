package com.rolandvitezhu.todocloud.network.api.list.service;

import com.rolandvitezhu.todocloud.network.api.list.dto.InsertListRequest;
import com.rolandvitezhu.todocloud.network.api.list.dto.InsertListResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface InsertListService {

  @POST("todo_cloud/v1/list/insert")
  Call<InsertListResponse> insertList(
      @Header("authorization") String apiKey, @Body InsertListRequest request
  );
}

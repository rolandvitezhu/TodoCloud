package com.rolandvitezhu.todocloud.network.api.rowversion.service;

import com.rolandvitezhu.todocloud.network.api.rowversion.dto.GetNextRowVersionResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface GetNextRowVersionService {

  @GET("todo_cloud/v1/get_next_row_version/{table}")
  Call<GetNextRowVersionResponse> getNextRowVersion(
      @Path("table") String table, @Header("authorization") String apiKey
  );
}

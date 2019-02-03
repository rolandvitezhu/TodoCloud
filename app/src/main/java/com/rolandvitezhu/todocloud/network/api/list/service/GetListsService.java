package com.rolandvitezhu.todocloud.network.api.list.service;

import com.rolandvitezhu.todocloud.network.api.list.dto.GetListsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface GetListsService {

  @GET("todo_cloud/v1/list/{row_version}")
  Call<GetListsResponse> getLists(
      @Path("row_version") int rowVersion, @Header("authorization") String apiKey
  );
}

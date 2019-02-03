package com.rolandvitezhu.todocloud.network.api.category.service;

import com.rolandvitezhu.todocloud.network.api.category.dto.GetCategoriesResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface GetCategoriesService {

  @GET("todo_cloud/v1/category/{row_version}")
  Call<GetCategoriesResponse> getCategories(
      @Path("row_version") int rowVersion, @Header("authorization") String apiKey
  );
}

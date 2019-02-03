package com.rolandvitezhu.todocloud.network.api.category.service;

import com.rolandvitezhu.todocloud.network.api.category.dto.InsertCategoryRequest;
import com.rolandvitezhu.todocloud.network.api.category.dto.InsertCategoryResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface InsertCategoryService {

  @POST("todo_cloud/v1/category/insert")
  Call<InsertCategoryResponse> insertCategory(
      @Header("authorization") String apiKey, @Body InsertCategoryRequest request
  );
}

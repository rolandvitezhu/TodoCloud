package com.rolandvitezhu.todocloud.network.api.category.service;

import com.rolandvitezhu.todocloud.network.api.category.dto.UpdateCategoryRequest;
import com.rolandvitezhu.todocloud.network.api.category.dto.UpdateCategoryResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface UpdateCategoryService {

  // 000webhost.com doesn't allow PUT and DELETE requests for free accounts
  @POST("todo_cloud/v1/category/update")
  Call<UpdateCategoryResponse> updateCategory(
      @Header("authorization") String apiKey, @Body UpdateCategoryRequest request
  );
}

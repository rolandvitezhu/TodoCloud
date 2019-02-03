package com.rolandvitezhu.todocloud.network.api.category.dto;

import com.google.gson.annotations.SerializedName;
import com.rolandvitezhu.todocloud.data.Category;
import com.rolandvitezhu.todocloud.network.api.dto.BaseResponse;

import java.util.ArrayList;

public class GetCategoriesResponse extends BaseResponse {

  @SerializedName("categories")
  public ArrayList<Category> categories;

  public ArrayList<Category> getCategories() {
    return categories;
  }

  public void setCategories(ArrayList<Category> categories) {
    this.categories = categories;
  }
}

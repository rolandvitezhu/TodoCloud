package com.rolandvitezhu.todocloud.network.api.user.dto;

import com.google.gson.annotations.SerializedName;
import com.rolandvitezhu.todocloud.network.api.dto.BaseResponse;

public class LoginUserResponse extends BaseResponse {

  @SerializedName("user_online_id")
  public String userOnlineId;
  @SerializedName("name")
  public String name;
  @SerializedName("email")
  public String email;
  @SerializedName("api_key")
  public String apiKey;

  public String getUserOnlineId() {
    return userOnlineId;
  }

  public void setUserOnlineId(String userOnlineId) {
    this.userOnlineId = userOnlineId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }
}

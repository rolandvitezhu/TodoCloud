package com.rolandvitezhu.todocloud.network.api.user.dto;

import com.google.gson.annotations.SerializedName;

public class RegisterUserRequest {

  @SerializedName("user_online_id")
  public String userOnlineId;
  @SerializedName("name")
  public String name;
  @SerializedName("email")
  public String email;
  @SerializedName("password")
  public String password;

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

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}

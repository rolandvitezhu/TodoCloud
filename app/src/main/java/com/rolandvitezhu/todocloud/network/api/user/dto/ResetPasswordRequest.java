package com.rolandvitezhu.todocloud.network.api.user.dto;

import com.google.gson.annotations.SerializedName;

public class ResetPasswordRequest {

  @SerializedName("email")
  public String email;

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }
}

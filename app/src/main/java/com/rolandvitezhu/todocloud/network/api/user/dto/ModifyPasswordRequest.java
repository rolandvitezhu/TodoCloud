package com.rolandvitezhu.todocloud.network.api.user.dto;

import com.google.gson.annotations.SerializedName;

public class ModifyPasswordRequest {

  @SerializedName("current_password")
  public String currentPassword;
  @SerializedName("new_password")
  public String newPassword;

  public String getCurrentPassword() {
    return currentPassword;
  }

  public void setCurrentPassword(String currentPassword) {
    this.currentPassword = currentPassword;
  }

  public String getNewPassword() {
    return newPassword;
  }

  public void setNewPassword(String newPassword) {
    this.newPassword = newPassword;
  }
}

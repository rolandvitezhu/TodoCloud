package com.rolandvitezhu.todocloud.network.api.dto;

import com.google.gson.annotations.SerializedName;

public class BaseResponse {

  @SerializedName("error")
  public String error;
  @SerializedName("message")
  public String message;

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}

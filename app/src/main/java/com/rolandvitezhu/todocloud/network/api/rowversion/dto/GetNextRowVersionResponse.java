package com.rolandvitezhu.todocloud.network.api.rowversion.dto;

import com.google.gson.annotations.SerializedName;
import com.rolandvitezhu.todocloud.network.api.dto.BaseResponse;

public class GetNextRowVersionResponse extends BaseResponse {

  @SerializedName("next_row_version")
  public int nextRowVersion;

  public int getNextRowVersion() {
    return nextRowVersion;
  }

  public void setNextRowVersion(int nextRowVersion) {
    this.nextRowVersion = nextRowVersion;
  }
}

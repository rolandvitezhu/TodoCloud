package com.rolandvitezhu.todocloud.network.api.list.dto;

import com.google.gson.annotations.SerializedName;

public class InsertListRequest {

  @SerializedName("list_online_id")
  public String listOnlineId;
  @SerializedName("category_online_id")
  public String categoryOnlineId;
  @SerializedName("title")
  public String title;
  @SerializedName("row_version")
  public int rowVersion;
  @SerializedName("deleted")
  public Boolean deleted;
  @SerializedName("position")
  public int position;

  public String getListOnlineId() {
    return listOnlineId;
  }

  public void setListOnlineId(String listOnlineId) {
    this.listOnlineId = listOnlineId;
  }

  public String getCategoryOnlineId() {
    return categoryOnlineId;
  }

  public void setCategoryOnlineId(String categoryOnlineId) {
    this.categoryOnlineId = categoryOnlineId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public int getRowVersion() {
    return rowVersion;
  }

  public void setRowVersion(int rowVersion) {
    this.rowVersion = rowVersion;
  }

  public Boolean getDeleted() {
    return deleted;
  }

  public void setDeleted(Boolean deleted) {
    this.deleted = deleted;
  }

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }
}

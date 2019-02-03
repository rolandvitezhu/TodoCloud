package com.rolandvitezhu.todocloud.network.api.list.dto;

import com.google.gson.annotations.SerializedName;
import com.rolandvitezhu.todocloud.data.List;
import com.rolandvitezhu.todocloud.network.api.dto.BaseResponse;

import java.util.ArrayList;

public class GetListsResponse extends BaseResponse {

  @SerializedName("lists")
  public ArrayList<List> lists;

  public ArrayList<List> getLists() {
    return lists;
  }

  public void setLists(ArrayList<List> lists) {
    this.lists = lists;
  }
}

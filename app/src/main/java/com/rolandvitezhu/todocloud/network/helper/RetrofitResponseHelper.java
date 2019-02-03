package com.rolandvitezhu.todocloud.network.helper;

import com.google.gson.GsonBuilder;
import com.rolandvitezhu.todocloud.network.api.dto.BaseResponse;

import retrofit2.Response;

public class RetrofitResponseHelper {

  public static String ResponseToJson(Response response) {
    try {
      return new GsonBuilder().disableHtmlEscaping().serializeNulls().create().toJson(response.body());
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  public static boolean IsNoError(Response response) {
    if (response.body() != null) {
      BaseResponse baseResponse = (BaseResponse) response.body();

      return baseResponse.getError().equals("false");
    } else {
      return false;
    }
  }
}

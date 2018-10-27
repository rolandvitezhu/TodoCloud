package com.rolandvitezhu.todocloud.datasynchronizer;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.rolandvitezhu.todocloud.app.AppConfig;
import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.datastorage.DbLoader;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

class BaseDataSynchronizer {

  int nextRowVersion;
  DbLoader dbLoader;

  public BaseDataSynchronizer(DbLoader dbLoader) {
    this.dbLoader = dbLoader;
  }

  void getNextRowVersion(
      String TABLE,
      Response.Listener<JSONObject> responseListener,
      Response.ErrorListener responseErrorListener
  ) {
    String tag_json_object_request = "request_get_next_row_version";
    nextRowVersion = 0;
    JsonObjectRequest getNextRowVersionRequest = prepareGetNextRowVersionRequest(
        TABLE,
        responseListener,
        responseErrorListener
    );
    AppController.getInstance().addToRequestQueue(getNextRowVersionRequest, tag_json_object_request);
  }

  private JsonObjectRequest prepareGetNextRowVersionRequest(
      String table,
      Response.Listener<JSONObject> responseListener,
      Response.ErrorListener responseErrorListener
  ) {
    String url = prepareGetNextRowVersionUrl(table);
    JsonObjectRequest getNextRowVersionRequest = new JsonObjectRequest(
        Request.Method.GET,
        url,
        null,
        responseListener,
        responseErrorListener
    ) {
      @Override
      public Map<String, String> getHeaders() throws AuthFailureError {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("authorization", dbLoader.getApiKey());
        return headers;
      }
    };

    return getNextRowVersionRequest;
  }

  private String prepareGetNextRowVersionUrl(String table) {
    int end = AppConfig.URL_GET_NEXT_ROW_VERSION.lastIndexOf(":");
    return AppConfig.URL_GET_NEXT_ROW_VERSION.substring(0, end)
        + table;
  }
}

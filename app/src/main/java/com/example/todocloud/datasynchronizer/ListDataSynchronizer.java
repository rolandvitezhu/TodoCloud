package com.example.todocloud.datasynchronizer;

import android.support.annotation.NonNull;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.example.todocloud.app.AppConfig;
import com.example.todocloud.app.AppController;
import com.example.todocloud.data.List;
import com.example.todocloud.datastorage.DbLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ListDataSynchronizer extends DataSynchronizer {

  private static final String TAG = ListDataSynchronizer.class.getSimpleName();

  private OnSyncListDataListener onSyncListDataListener;
  private DbLoader dbLoader;

  public ListDataSynchronizer(DbLoader dbLoader) {
    this.dbLoader = dbLoader;
  }

  public void setOnSyncListDataListener(OnSyncListDataListener onSyncListDataListener) {
    this.onSyncListDataListener = onSyncListDataListener;
  }

  public void getLists() {
    String tag_string_request = "request_get_lists";

    String url = prepareGetListsUrl();
    StringRequest getListsRequest = new StringRequest(
        Request.Method.GET,
        url,
        new Response.Listener<String>() {

          @Override
          public void onResponse(String response) {
            Log.d(TAG, "Get Lists Response: " + response);
            try {
              JSONObject jsonResponse = new JSONObject(response);
              boolean error = jsonResponse.getBoolean("error");

              if (!error) {
                ArrayList<com.example.todocloud.data.List> lists = getLists(jsonResponse);
                if (!lists.isEmpty()) {
                  updateListsInLocalDatabase(lists);
                }
                onSyncListDataListener.onGetLists();
              } else {
                String message = jsonResponse.getString("message");
                Log.d(TAG, "Error Message: " + message);
              }
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }

          @NonNull
          private ArrayList<List> getLists(JSONObject jsonResponse) throws JSONException {
            JSONArray jsonLists = jsonResponse.getJSONArray("lists");
            ArrayList<com.example.todocloud.data.List> lists = new ArrayList<>();

            for (int i = 0; i < jsonLists.length(); i++) {
              JSONObject jsonList = jsonLists.getJSONObject(i);
              com.example.todocloud.data.List list =
                  new com.example.todocloud.data.List(jsonList);
              lists.add(list);
            }
            return lists;
          }

          private void updateListsInLocalDatabase(ArrayList<List> lists) {
            for (List list : lists) {
              boolean exists = dbLoader.isListExists(list.getListOnlineId());
              if (!exists) {
                dbLoader.createList(list);
              } else {
                dbLoader.updateList(list);
              }
            }
          }

        },
        new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError error) {
            String errorMessage = error.getMessage();
            Log.e(TAG, "Get Lists Error: " + errorMessage);
            if (errorMessage != null) {
              onSyncListDataListener.onSyncError(errorMessage);
            }
          }

        }
    ) {

      @Override
      public Map<String, String> getHeaders() throws AuthFailureError {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("authorization", dbLoader.getApiKey());
        return headers;
      }

    };

    AppController.getInstance().addToRequestQueue(getListsRequest, tag_string_request);
  }

  @NonNull
  private String prepareGetListsUrl() {
    int end = AppConfig.URL_GET_LISTS.lastIndexOf(":");
    return AppConfig.URL_GET_LISTS.substring(0, end)
        + dbLoader.getListRowVersion();
  }

  public interface OnSyncListDataListener {
    void onGetLists();
    void onSyncError(String errorMessage);
  }

}

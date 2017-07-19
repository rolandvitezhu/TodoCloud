package com.example.todocloud.datasynchronizer;

import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.example.todocloud.R;
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
                ArrayList<List> lists = getLists(jsonResponse);
                if (!lists.isEmpty()) {
                  updateListsInLocalDatabase(lists);
                }
                onSyncListDataListener.onFinishGetLists();
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
            ArrayList<List> lists = new ArrayList<>();

            for (int i = 0; i < jsonLists.length(); i++) {
              JSONObject jsonList = jsonLists.getJSONObject(i);
              List list = new List(jsonList);
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

  public void updateLists() {
    ArrayList<List> listsToUpdate = dbLoader.getListsToUpdate();

    if (!listsToUpdate.isEmpty()) {
      String tag_json_object_request = "request_update_list";
      for (final List listToUpdate : listsToUpdate) {
        JSONObject jsonRequest = new JSONObject();
        try {
          putListData(listToUpdate, jsonRequest);
        } catch (JSONException e) {
          e.printStackTrace();
        }

        JsonObjectRequest updateListsRequest = new JsonObjectRequest(
            JsonObjectRequest.Method.PUT,
            AppConfig.URL_UPDATE_LIST,
            jsonRequest,
            new Response.Listener<JSONObject>() {

              @Override
              public void onResponse(JSONObject response) {
                Log.d(TAG, "Update List Response: " + response);
                try {
                  boolean error = response.getBoolean("error");

                  if (!error) {
                    makeListUpToDate(response);
                  } else {
                    String message = response.getString("message");
                    Log.d(TAG, "Error Message: " + message);
                  }

                } catch (JSONException e) {
                  e.printStackTrace();
                }
              }

              private void makeListUpToDate(JSONObject response) throws JSONException {
                listToUpdate.setRowVersion(response.getInt("row_version"));
                listToUpdate.setDirty(false);
                dbLoader.updateList(listToUpdate);
              }

            },
            new Response.ErrorListener() {

              @Override
              public void onErrorResponse(VolleyError error) {
                String errorMessage = error.getMessage();
                Log.e(TAG, "Update List Error: " + errorMessage);
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

        AppController.getInstance().addToRequestQueue(updateListsRequest, tag_json_object_request);
      }
    }
    onSyncListDataListener.onFinishUpdateLists();
  }

  @NonNull
  private String prepareGetListsUrl() {
    int end = AppConfig.URL_GET_LISTS.lastIndexOf(":");
    return AppConfig.URL_GET_LISTS.substring(0, end)
        + dbLoader.getListRowVersion();
  }

  private void putListData(List listData, JSONObject jsonRequest) throws JSONException {
    jsonRequest.put("list_online_id", listData.getListOnlineId().trim());
    if (listData.getCategoryOnlineId() != null) {
      jsonRequest.put("category_online_id", listData.getCategoryOnlineId().trim());
    } else {
      jsonRequest.put("category_online_id", "");
    }
    jsonRequest.put("title", listData.getTitle().trim());
    jsonRequest.put("deleted", listData.getDeleted() ? 1 : 0);
  }

  public interface OnSyncListDataListener {
    void onFinishGetLists();
    void onFinishUpdateLists();
    void onSyncError(String errorMessage);
  }

}

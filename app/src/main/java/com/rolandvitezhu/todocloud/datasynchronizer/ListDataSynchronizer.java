package com.rolandvitezhu.todocloud.datasynchronizer;

import android.support.annotation.NonNull;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.rolandvitezhu.todocloud.app.AppConfig;
import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.data.List;
import com.rolandvitezhu.todocloud.datastorage.DbConstants;
import com.rolandvitezhu.todocloud.datastorage.DbLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class ListDataSynchronizer extends BaseDataSynchronizer {

  private static final String TAG = ListDataSynchronizer.class.getSimpleName();
  private static final String TABLE = DbConstants.List.DATABASE_TABLE;

  private OnSyncListDataListener onSyncListDataListener;
  private boolean isUpdateListRequestsFinished;
  private int updateListRequestCount;
  private int currentUpdateListRequest;
  private boolean isInsertListRequestsFinished;
  private int insertListRequestCount;
  private int currentInsertListRequest;

  private ArrayList<List> listsToUpdate;
  private ArrayList<List> listsToInsert;

  private Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
    @Override
    public void onResponse(JSONObject response) {
      try {
        boolean error = response.getBoolean("error");

        if (!error) {
          nextRowVersion = response.getInt("next_row_version");
          setRowVersionsForLists(listsToUpdate);
          setRowVersionsForLists(listsToInsert);
          updateLists();
        } else {
          String message = response.getString("message");
          Log.d(TAG, "Error Message: " + message);
        }
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
  };

  private Response.ErrorListener responseErrorListener = new Response.ErrorListener() {
    @Override
    public void onErrorResponse(VolleyError error) {
      Log.e(TAG, "Get Next Row Version Error: " + error);
    }
  };

  ListDataSynchronizer(DbLoader dbLoader) {
    super(dbLoader);
  }

  void setOnSyncListDataListener(OnSyncListDataListener onSyncListDataListener) {
    this.onSyncListDataListener = onSyncListDataListener;
  }

  void syncListData() {
    initListRequestsStates();
    getLists();
  }

  private void initListRequestsStates() {
    isUpdateListRequestsFinished = false;
    isInsertListRequestsFinished = false;
    listsToUpdate = dbLoader.getListsToUpdate();
    listsToInsert = dbLoader.getListsToInsert();
    nextRowVersion = 0;
  }

  private void setRowVersionsForLists(ArrayList<List> lists) {
    for (List list : lists) {
      list.setRowVersion(nextRowVersion++);
    }
  }

  private void getLists() {
    String tag_string_request = "request_get_lists";
    StringRequest getListsRequest = prepareGetListsRequest();
    AppController.getInstance().addToRequestQueue(getListsRequest, tag_string_request);
  }

  private void updateLists() {
    if (!listsToUpdate.isEmpty()) {
      AppController appController = AppController.getInstance();
      String tag_json_object_request = "request_update_list";
      updateListRequestCount = listsToUpdate.size();
      currentUpdateListRequest = 1;
      for (final List listToUpdate : listsToUpdate) {
        JsonObjectRequest updateListRequest = prepareUpdateListRequest(listToUpdate);
        appController.addToRequestQueue(updateListRequest, tag_json_object_request);
      }
    } else {
      isUpdateListRequestsFinished = true;
      if (isAllListRequestsFinished()) {
        onSyncListDataListener.onFinishSyncListData();
      }
    }
    insertLists();
  }

  private void insertLists() {
    if (!listsToInsert.isEmpty()) {
      AppController appController = AppController.getInstance();
      String tag_json_object_request = "request_insert_list";
      insertListRequestCount = listsToInsert.size();
      currentInsertListRequest = 1;
      for (final List listToInsert : listsToInsert) {
        JsonObjectRequest insertListRequest = prepareInsertListRequest(listToInsert);
        appController.addToRequestQueue(insertListRequest, tag_json_object_request);
      }
    } else {
      isInsertListRequestsFinished = true;
      if (isAllListRequestsFinished()) {
        onSyncListDataListener.onFinishSyncListData();
      }
    }
  }

  private StringRequest prepareGetListsRequest() {
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

                boolean shouldUpdateOrInsertLists = !listsToUpdate.isEmpty() || !listsToInsert.isEmpty();
                if (shouldUpdateOrInsertLists)
                  getNextRowVersion(TABLE, responseListener, responseErrorListener);
                else onSyncListDataListener.onFinishSyncListData();
              } else {
                String message = jsonResponse.getString("message");
                Log.d(TAG, "Error Message: " + message);
                if (message == null) message = "Unknown error";
                onSyncListDataListener.onSyncError(message);
              }
            } catch (JSONException e) {
              e.printStackTrace();
              String errorMessage = "Unknown error";
              onSyncListDataListener.onSyncError(errorMessage);
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
            if (errorMessage == null) errorMessage = "Unknown error";
            onSyncListDataListener.onSyncError(errorMessage);
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
    return getListsRequest;
  }

  // 000webhost.com don't allow PUT and DELETE requests for free accounts
  private JsonObjectRequest prepareUpdateListRequest(final List listToUpdate) {
    JSONObject updateListJsonRequest = prepareUpdateListJsonRequest(listToUpdate);
    JsonObjectRequest updateListRequest = new JsonObjectRequest(
        JsonObjectRequest.Method.POST,
        AppConfig.URL_UPDATE_LIST,
        updateListJsonRequest,
        new Response.Listener<JSONObject>() {

          @Override
          public void onResponse(JSONObject response) {
            Log.d(TAG, "Update List Response: " + response);
            try {
              boolean error = response.getBoolean("error");

              if (!error) {
                makeListUpToDate();
                if (isLastUpdateListRequest()) {
                  isUpdateListRequestsFinished = true;
                  if (isAllListRequestsFinished()) {
                    onSyncListDataListener.onFinishSyncListData();
                  }
                }
              } else {
                String message = response.getString("message");
                Log.d(TAG, "Error Message: " + message);
                if (message == null) message = "Unknown error";
                onSyncListDataListener.onSyncError(message);
                if (isLastUpdateListRequest()) {
                  isUpdateListRequestsFinished = true;
                  if (isAllListRequestsFinished()) {
                    onSyncListDataListener.onFinishSyncListData();
                  }
                }
              }

            } catch (JSONException e) {
              e.printStackTrace();
              String errorMessage = "Unknown error";
              onSyncListDataListener.onSyncError(errorMessage);
              if (isLastUpdateListRequest()) {
                isUpdateListRequestsFinished = true;
                if (isAllListRequestsFinished()) {
                  onSyncListDataListener.onFinishSyncListData();
                }
              }
            }
          }

          private void makeListUpToDate() {
            listToUpdate.setDirty(false);
            dbLoader.updateList(listToUpdate);
          }

        },
        new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError error) {
            String errorMessage = error.getMessage();
            Log.e(TAG, "Update List Error: " + errorMessage);
            if (errorMessage == null) errorMessage = "Unknown error";
            onSyncListDataListener.onSyncError(errorMessage);
            if (isLastUpdateListRequest()) {
              isUpdateListRequestsFinished = true;
              if (isAllListRequestsFinished()) {
                onSyncListDataListener.onFinishSyncListData();
              }
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
    return updateListRequest;
  }

  private JSONObject prepareUpdateListJsonRequest(final List listToUpdate) {
    JSONObject updateListJsonRequest = new JSONObject();
    try {
      putListData(listToUpdate, updateListJsonRequest);
    } catch (JSONException e) {
      e.printStackTrace();
      String errorMessage = "Unknown error";
      onSyncListDataListener.onSyncError(errorMessage);
      if (isLastUpdateListRequest()) {
        isUpdateListRequestsFinished = true;
        if (isAllListRequestsFinished()) {
          onSyncListDataListener.onFinishSyncListData();
        }
      }
    }
    return updateListJsonRequest;
  }

  private JsonObjectRequest prepareInsertListRequest(final List listToInsert) {
    JSONObject insertListJsonRequest = prepareInsertListJsonRequest(listToInsert);
    JsonObjectRequest insertListRequest = new JsonObjectRequest(
        JsonObjectRequest.Method.POST,
        AppConfig.URL_INSERT_LIST,
        insertListJsonRequest,
        new Response.Listener<JSONObject>() {

          @Override
          public void onResponse(JSONObject response) {
            Log.d(TAG, "Insert List Response: " + response);
            try {
              boolean error = response.getBoolean("error");

              if (!error) {
                makeListUpToDate();
                if (isLastInsertListRequest()) {
                  isInsertListRequestsFinished = true;
                  if (isAllListRequestsFinished()) {
                    onSyncListDataListener.onFinishSyncListData();
                  }
                }
              } else {
                String message = response.getString("message");
                Log.d(TAG, "Error Message: " + message);
                if (message == null) message = "Unknown error";
                onSyncListDataListener.onSyncError(message);
                if (isLastInsertListRequest()) {
                  isInsertListRequestsFinished = true;
                  if (isAllListRequestsFinished()) {
                    onSyncListDataListener.onFinishSyncListData();
                  }
                }
              }
            } catch (JSONException e) {
              e.printStackTrace();
              String errorMessage = "Unknown error";
              onSyncListDataListener.onSyncError(errorMessage);
              if (isLastInsertListRequest()) {
                isInsertListRequestsFinished = true;
                if (isAllListRequestsFinished()) {
                  onSyncListDataListener.onFinishSyncListData();
                }
              }
            }
          }

          private void makeListUpToDate() {
            listToInsert.setDirty(false);
            dbLoader.updateList(listToInsert);
          }

        },
        new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError error) {
            String errorMessage = error.getMessage();
            Log.e(TAG, "Insert List Error: " + errorMessage);
            if (errorMessage == null) errorMessage = "Unknown error";
            onSyncListDataListener.onSyncError(errorMessage);
            if (isLastInsertListRequest()) {
              isInsertListRequestsFinished = true;
              if (isAllListRequestsFinished()) {
                onSyncListDataListener.onFinishSyncListData();
              }
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
    return insertListRequest;
  }

  private boolean isLastInsertListRequest() {
    if (currentInsertListRequest++ == insertListRequestCount) {
      return true;
    } else {
      return false;
    }
  }

  private boolean isLastUpdateListRequest() {
    if (currentUpdateListRequest++ == updateListRequestCount) {
      return true;
    } else {
      return false;
    }
  }

  @NonNull
  private String prepareGetListsUrl() {
    int end = AppConfig.URL_GET_LISTS.lastIndexOf(":");
    return AppConfig.URL_GET_LISTS.substring(0, end)
        + dbLoader.getLastListRowVersion();
  }

  private JSONObject prepareInsertListJsonRequest(final List listToInsert) {
    JSONObject insertListJsonRequest = new JSONObject();
    try {
      putListData(listToInsert, insertListJsonRequest);
    } catch (JSONException e) {
      e.printStackTrace();
      String errorMessage = "Unknown error";
      onSyncListDataListener.onSyncError(errorMessage);
      if (isLastInsertListRequest()) {
        isInsertListRequestsFinished = true;
        if (isAllListRequestsFinished()) {
          onSyncListDataListener.onFinishSyncListData();
        }
      }
    }
    return insertListJsonRequest;
  }

  private void putListData(List listData, JSONObject jsonRequest) throws JSONException {
    jsonRequest.put("list_online_id", listData.getListOnlineId().trim());
    if (listData.getCategoryOnlineId() != null) {
      jsonRequest.put("category_online_id", listData.getCategoryOnlineId().trim());
    } else {
      jsonRequest.put("category_online_id", "");
    }
    jsonRequest.put("title", listData.getTitle().trim());
    jsonRequest.put(DbConstants.List.KEY_ROW_VERSION, listData.getRowVersion());
    jsonRequest.put("deleted", listData.getDeleted() ? 1 : 0);
    // TODO: Swap dummy value with real data
    jsonRequest.put(DbConstants.List.KEY_POSITION, 0);
  }

  private boolean isAllListRequestsFinished() {
    return isUpdateListRequestsFinished && isInsertListRequestsFinished;
  }

  interface OnSyncListDataListener {
    void onFinishSyncListData();
    void onSyncError(String errorMessage);
  }

}

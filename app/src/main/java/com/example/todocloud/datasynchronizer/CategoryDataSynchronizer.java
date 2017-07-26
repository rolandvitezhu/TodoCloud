package com.example.todocloud.datasynchronizer;

import android.support.annotation.NonNull;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.example.todocloud.app.AppConfig;
import com.example.todocloud.app.AppController;
import com.example.todocloud.data.Category;
import com.example.todocloud.datastorage.DbLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class CategoryDataSynchronizer {

  private static final String TAG = CategoryDataSynchronizer.class.getSimpleName();

  private OnSyncCategoryDataListener onSyncCategoryDataListener;
  private DbLoader dbLoader;
  private boolean isUpdateCategoryRequestsFinished;
  private int updateCategoryRequestCount;
  private int currentUpdateCategoryRequest;
  private boolean isInsertCategoryRequestsFinished;
  private int insertCategoryRequestCount;
  private int currentInsertCategoryRequest;

  CategoryDataSynchronizer(DbLoader dbLoader) {
    this.dbLoader = dbLoader;
  }

  void setOnSyncCategoryDataListener(
      OnSyncCategoryDataListener onSyncCategoryDataListener
  ) {
    this.onSyncCategoryDataListener = onSyncCategoryDataListener;
  }

  void syncCategoryData() {
    initCategoryRequestsStates();
    getCategories();
  }

  private void initCategoryRequestsStates() {
    isUpdateCategoryRequestsFinished = false;
    isInsertCategoryRequestsFinished = false;
  }

  private void getCategories() {
    String tag_string_request = "request_get_categories";
    StringRequest getCategoriesRequest = prepareGetCategoriesRequest();
    AppController.getInstance().addToRequestQueue(getCategoriesRequest, tag_string_request);
  }

  private void updateCategories() {
    ArrayList<Category> categoriesToUpdate = dbLoader.getCategoriesToUpdate();

    if (!categoriesToUpdate.isEmpty()) {
      AppController appController = AppController.getInstance();
      String tag_json_object_request = "request_update_category";
      updateCategoryRequestCount = categoriesToUpdate.size();
      currentUpdateCategoryRequest = 1;
      for (final Category categoryToUpdate : categoriesToUpdate) {
        JsonObjectRequest updateCategoryRequest = prepareUpdateCategoryRequest(categoryToUpdate);
        appController.addToRequestQueue(updateCategoryRequest, tag_json_object_request);
      }
    } else {
      isUpdateCategoryRequestsFinished = true;
      if (isAllCategoryRequestsFinished()) {
        onSyncCategoryDataListener.onFinishSyncCategoryData();
      }
    }
    insertCategories();
  }

  private void insertCategories() {
    ArrayList<Category> categoriesToInsert = dbLoader.getCategoriesToInsert();

    if (!categoriesToInsert.isEmpty()) {
      AppController appController = AppController.getInstance();
      String tag_json_object_request = "request_insert_category";
      insertCategoryRequestCount = categoriesToInsert.size();
      currentInsertCategoryRequest = 1;
      for (final Category categoryToInsert : categoriesToInsert) {
        JsonObjectRequest insertCategoryRequest = prepareInsertCategoryRequest(categoryToInsert);
        appController.addToRequestQueue(insertCategoryRequest, tag_json_object_request);
      }
    } else {
      isInsertCategoryRequestsFinished = true;
      if (isAllCategoryRequestsFinished()) {
        onSyncCategoryDataListener.onFinishSyncCategoryData();
      }
    }
  }

  private StringRequest prepareGetCategoriesRequest() {
    String url = prepareGetCategoriesUrl();
    StringRequest getCategoriesRequest = new StringRequest(
        Request.Method.GET,
        url,
        new Response.Listener<String>() {

          @Override
          public void onResponse(String response) {
            Log.d(TAG, "Get Categories Response: " + response);
            try {
              JSONObject jsonResponse = new JSONObject(response);
              boolean error = jsonResponse.getBoolean("error");

              if (!error) {
                ArrayList<Category> categories = getCategories(jsonResponse);
                if (!categories.isEmpty()) {
                  updateCategoriesInLocalDatabase(categories);
                }
                updateCategories();
              } else {
                String message = jsonResponse.getString("message");
                Log.d(TAG, "Error Message: " + message);
                if (message == null) message = "Unknown error";
                onSyncCategoryDataListener.onSyncError(message);
              }
            } catch (JSONException e) {
              e.printStackTrace();
              String errorMessage = "Unknown error";
              onSyncCategoryDataListener.onSyncError(errorMessage);
            }
          }

          @NonNull
          private ArrayList<Category> getCategories(JSONObject jsonResponse) throws JSONException {
            JSONArray jsonCategories = jsonResponse.getJSONArray("categories");
            ArrayList<Category> categories = new ArrayList<>();

            for (int i = 0; i < jsonCategories.length(); i++) {
              JSONObject jsonCategory = jsonCategories.getJSONObject(i);
              Category category = new Category(jsonCategory);
              categories.add(category);
            }
            return categories;
          }

          private void updateCategoriesInLocalDatabase(ArrayList<Category> categories) {
            for (Category category : categories) {
              boolean exists = dbLoader.isCategoryExists(category.getCategoryOnlineId());
              if (!exists) {
                dbLoader.createCategory(category);
              } else {
                dbLoader.updateCategory(category);
              }
            }
          }

        },
        new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError error) {
            String errorMessage = error.getMessage();
            Log.e(TAG, "Get Categories Error: " + errorMessage);
            if (errorMessage == null) errorMessage = "Unknown error";
            onSyncCategoryDataListener.onSyncError(errorMessage);
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
    return getCategoriesRequest;
  }

  private JsonObjectRequest prepareUpdateCategoryRequest(final Category categoryToUpdate) {
    JSONObject updateCategoryJsonRequest = prepareUpdateCategoryJsonRequest(categoryToUpdate);
    JsonObjectRequest updateCategoryRequest = new JsonObjectRequest(
        JsonObjectRequest.Method.PUT,
        AppConfig.URL_UPDATE_CATEGORY,
        updateCategoryJsonRequest,
        new Response.Listener<JSONObject>() {

          @Override
          public void onResponse(JSONObject response) {
            Log.d(TAG, "Update Category Response: " + response);
            try {
              boolean error = response.getBoolean("error");

              if (!error) {
                makeCategoryUpToDate(response);
                if (isLastUpdateCategoryRequest()) {
                  isUpdateCategoryRequestsFinished = true;
                  if (isAllCategoryRequestsFinished()) {
                    onSyncCategoryDataListener.onFinishSyncCategoryData();
                  }
                }
              } else {
                String message = response.getString("message");
                Log.d(TAG, "Error Message: " + message);
                if (message == null) message = "Unknown error";
                onSyncCategoryDataListener.onSyncError(message);
                if (isLastUpdateCategoryRequest()) {
                  isUpdateCategoryRequestsFinished = true;
                  if (isAllCategoryRequestsFinished()) {
                    onSyncCategoryDataListener.onFinishSyncCategoryData();
                  }
                }
              }

            } catch (JSONException e) {
              e.printStackTrace();
              String errorMessage = "Unknown error";
              onSyncCategoryDataListener.onSyncError(errorMessage);
              if (isLastUpdateCategoryRequest()) {
                isUpdateCategoryRequestsFinished = true;
                if (isAllCategoryRequestsFinished()) {
                  onSyncCategoryDataListener.onFinishSyncCategoryData();
                }
              }
            }
          }

          private void makeCategoryUpToDate(JSONObject response) throws JSONException {
            categoryToUpdate.setRowVersion(response.getInt("row_version"));
            categoryToUpdate.setDirty(false);
            dbLoader.updateCategory(categoryToUpdate);
          }

        },
        new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError error) {
            String errorMessage = error.getMessage();
            Log.e(TAG, "Update Category Error: " + errorMessage);
            if (errorMessage == null) errorMessage = "Unknown error";
            onSyncCategoryDataListener.onSyncError(errorMessage);
            if (isLastUpdateCategoryRequest()) {
              isUpdateCategoryRequestsFinished = true;
              if (isAllCategoryRequestsFinished()) {
                onSyncCategoryDataListener.onFinishSyncCategoryData();
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
    return updateCategoryRequest;
  }

  private JSONObject prepareUpdateCategoryJsonRequest(final Category categoryToUpdate) {
    JSONObject updateCategoryJsonRequest = new JSONObject();
    try {
      putCategoryData(categoryToUpdate, updateCategoryJsonRequest);
    } catch (JSONException e) {
      e.printStackTrace();
      String errorMessage = "Unknown error";
      onSyncCategoryDataListener.onSyncError(errorMessage);
      if (isLastUpdateCategoryRequest()) {
        isUpdateCategoryRequestsFinished = true;
        if (isAllCategoryRequestsFinished()) {
          onSyncCategoryDataListener.onFinishSyncCategoryData();
        }
      }
    }
    return updateCategoryJsonRequest;
  }

  private JsonObjectRequest prepareInsertCategoryRequest(final Category categoryToInsert) {
    JSONObject insertCategoryJsonRequest = prepareInsertCategoryJsonRequest(categoryToInsert);
    JsonObjectRequest insertCategoryRequest = new JsonObjectRequest(
        JsonObjectRequest.Method.POST,
        AppConfig.URL_INSERT_CATEGORY,
        insertCategoryJsonRequest,
        new Response.Listener<JSONObject>() {

          @Override
          public void onResponse(JSONObject response) {
            Log.d(TAG, "Insert Category Response: " + response);
            try {
              boolean error = response.getBoolean("error");

              if (!error) {
                makeCategoryUpToDate(response);
                if (isLastInsertCategoryRequest()) {
                  isInsertCategoryRequestsFinished = true;
                  if (isAllCategoryRequestsFinished()) {
                    onSyncCategoryDataListener.onFinishSyncCategoryData();
                  }
                }
              } else {
                String message = response.getString("message");
                Log.d(TAG, "Error Message: " + message);
                if (message == null) message = "Unknown error";
                onSyncCategoryDataListener.onSyncError(message);
                if (isLastInsertCategoryRequest()) {
                  isInsertCategoryRequestsFinished = true;
                  if (isAllCategoryRequestsFinished()) {
                    onSyncCategoryDataListener.onFinishSyncCategoryData();
                  }
                }
              }
            } catch (JSONException e) {
              e.printStackTrace();
              String errorMessage = "Unknown error";
              onSyncCategoryDataListener.onSyncError(errorMessage);
              if (isLastInsertCategoryRequest()) {
                isInsertCategoryRequestsFinished = true;
                if (isAllCategoryRequestsFinished()) {
                  onSyncCategoryDataListener.onFinishSyncCategoryData();
                }
              }
            }
          }

          private void makeCategoryUpToDate(JSONObject response) throws JSONException {
            categoryToInsert.setRowVersion(response.getInt("row_version"));
            categoryToInsert.setDirty(false);
            dbLoader.updateCategory(categoryToInsert);
          }

        },
        new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError error) {
            String errorMessage = error.getMessage();
            Log.e(TAG, "Insert Category Error: " + errorMessage);
            if (errorMessage == null) errorMessage = "Unknown error";
            onSyncCategoryDataListener.onSyncError(errorMessage);
            if (isLastInsertCategoryRequest()) {
              isInsertCategoryRequestsFinished = true;
              if (isAllCategoryRequestsFinished()) {
                onSyncCategoryDataListener.onFinishSyncCategoryData();
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
    return insertCategoryRequest;
  }

  private boolean isLastInsertCategoryRequest() {
    if (currentInsertCategoryRequest++ == insertCategoryRequestCount) {
      return true;
    } else {
      return false;
    }
  }

  private boolean isLastUpdateCategoryRequest() {
    if (currentUpdateCategoryRequest++ == updateCategoryRequestCount) {
      return true;
    } else {
      return false;
    }
  }

  @NonNull
  private String prepareGetCategoriesUrl() {
    int end = AppConfig.URL_GET_CATEGORIES.lastIndexOf(":");
    return AppConfig.URL_GET_CATEGORIES.substring(0, end) +
        dbLoader.getLastCategoryRowVersion();
  }

  private JSONObject prepareInsertCategoryJsonRequest(final Category categoryToInsert) {
    JSONObject insertCategoryJsonRequest = new JSONObject();
    try {
      putCategoryData(categoryToInsert, insertCategoryJsonRequest);
    } catch (JSONException e) {
      e.printStackTrace();
      String errorMessage = "Unknown error";
      onSyncCategoryDataListener.onSyncError(errorMessage);
      if (isLastInsertCategoryRequest()) {
        isInsertCategoryRequestsFinished = true;
        if (isAllCategoryRequestsFinished()) {
          onSyncCategoryDataListener.onFinishSyncCategoryData();
        }
      }
    }
    return insertCategoryJsonRequest;
  }

  private void putCategoryData(
      Category categoryData,
      JSONObject jsonRequest
  ) throws JSONException {
    jsonRequest.put("category_online_id", categoryData.getCategoryOnlineId().trim());
    jsonRequest.put("title", categoryData.getTitle().trim());
    jsonRequest.put("deleted", categoryData.getDeleted() ? 1 : 0);
  }

  private boolean isAllCategoryRequestsFinished() {
    return isUpdateCategoryRequestsFinished && isInsertCategoryRequestsFinished;
  }

  interface OnSyncCategoryDataListener {
    void onFinishSyncCategoryData();
    void onSyncError(String errorMessage);
  }

}

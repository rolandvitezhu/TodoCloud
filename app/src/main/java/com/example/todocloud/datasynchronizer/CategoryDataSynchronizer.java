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
import com.example.todocloud.data.Category;
import com.example.todocloud.datastorage.DbLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CategoryDataSynchronizer extends DataSynchronizer {

  private static final String TAG = CategoryDataSynchronizer.class.getSimpleName();

  private OnSyncCategoryDataListener onSyncCategoryDataListener;
  private DbLoader dbLoader;

  public CategoryDataSynchronizer(DbLoader dbLoader) {
    this.dbLoader = dbLoader;
  }

  public void setOnSyncCategoryDataListener(
      OnSyncCategoryDataListener onSyncCategoryDataListener
  ) {
    this.onSyncCategoryDataListener = onSyncCategoryDataListener;
  }

  public void getCategories() {
    String tag_string_request = "request_get_categories";

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
                onSyncCategoryDataListener.onGetCategories();
              } else {
                String message = jsonResponse.getString("message");
                Log.d(TAG, "Error Message: " + message);
              }
            } catch (JSONException e) {
              e.printStackTrace();
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
            if (errorMessage != null) {
              onSyncCategoryDataListener.onSyncError(errorMessage);
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

    AppController.getInstance().addToRequestQueue(getCategoriesRequest, tag_string_request);
  }

  @NonNull
  private String prepareGetCategoriesUrl() {
    int end = AppConfig.URL_GET_CATEGORIES.lastIndexOf(":");
    return AppConfig.URL_GET_CATEGORIES.substring(0, end) +
        dbLoader.getCategoryRowVersion();
  }

  public interface OnSyncCategoryDataListener {
    void onGetCategories();
    void onSyncError(String errorMessage);
  }

}

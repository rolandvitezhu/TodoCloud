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
import com.example.todocloud.data.Todo;
import com.example.todocloud.datastorage.DbLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TodoDataSynchronizer extends DataSynchronizer {

  private static final String TAG = TodoDataSynchronizer.class.getSimpleName();

  private OnSyncTodoDataListener onSyncTodoDataListener;
  private DbLoader dbLoader;

  public TodoDataSynchronizer(DbLoader dbLoader) {
    this.dbLoader = dbLoader;
  }

  public void setOnSyncTodoDataListener(OnSyncTodoDataListener onSyncTodoDataListener) {
    this.onSyncTodoDataListener = onSyncTodoDataListener;
  }

  public void getTodos() {
    String tag_string_request = "request_get_todos";

    String url = prepareGetTodosUrl();
    StringRequest getTodosRequest = new StringRequest(
        Request.Method.GET,
        url,
        new Response.Listener<String>() {

          @Override
          public void onResponse(String response) {
            Log.d(TAG, "Get Todos Response: " + response);
            try {
              JSONObject jsonResponse = new JSONObject(response);
              boolean error = jsonResponse.getBoolean("error");

              if (!error) {
                ArrayList<Todo> todos = getTodos(jsonResponse);
                onSyncTodoDataListener.onGetTodos(todos);
              } else {
                String message = jsonResponse.getString("message");
                Log.d(TAG, "Error Message: " + message);
              }
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }

          @NonNull
          private ArrayList<Todo> getTodos(JSONObject jsonResponse) throws JSONException {
            JSONArray jsonTodos = jsonResponse.getJSONArray("todos");
            ArrayList<Todo> todos = new ArrayList<>();

            for (int i = 0; i < jsonTodos.length(); i++) {
              JSONObject jsonTodo = jsonTodos.getJSONObject(i);
              Todo todo = new Todo(jsonTodo);
              todos.add(todo);
            }
            return todos;
          }

        },
        new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError error) {
            String errorMessage = error.getMessage();
            Log.e(TAG, "Get Todos Error: " + errorMessage);
            if (errorMessage != null) {
              onSyncTodoDataListener.onSyncError(errorMessage);
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

    AppController.getInstance().addToRequestQueue(getTodosRequest, tag_string_request);
  }

  @NonNull
  private String prepareGetTodosUrl() {
    int end = AppConfig.URL_GET_TODOS.lastIndexOf(":");
    return AppConfig.URL_GET_TODOS.substring(0, end)
        + dbLoader.getTodoRowVersion();
  }

  public interface OnSyncTodoDataListener {
    void onGetTodos(ArrayList<Todo> todos);
    void onSyncError(String errorMessage);
  }

}

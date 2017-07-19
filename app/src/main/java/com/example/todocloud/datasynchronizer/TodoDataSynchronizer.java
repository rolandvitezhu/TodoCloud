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
                if (!todos.isEmpty()) {
                  updateTodosInLocalDatabase(todos);
                }
                onSyncTodoDataListener.onGetTodos();
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

          private void updateTodosInLocalDatabase(ArrayList<Todo> todos) {
            for (Todo todo : todos) {
              boolean exists = dbLoader.isTodoExists(todo.getTodoOnlineId());
              if (!exists) {
                dbLoader.createTodo(todo);
              } else {
                dbLoader.updateTodo(todo);
              }
            }
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

  public void updateTodos() {
    ArrayList<Todo> todosToUpdate = dbLoader.getTodosToUpdate();

    if (!todosToUpdate.isEmpty()) {
      String tag_json_object_request = "request_update_todo";
      for (final Todo todoToUpdate : todosToUpdate) {
        JSONObject jsonRequest = new JSONObject();
        try {
          putTodoData(todoToUpdate, jsonRequest);
        } catch (JSONException e) {
          e.printStackTrace();
        }

        JsonObjectRequest updateTodosRequest = new JsonObjectRequest(
            JsonObjectRequest.Method.PUT,
            AppConfig.URL_UPDATE_TODO,
            jsonRequest,
            new Response.Listener<JSONObject>() {

              @Override
              public void onResponse(JSONObject response) {
                Log.d(TAG, "Update Todo Response: " + response);
                try {
                  boolean error = response.getBoolean("error");

                  if (!error) {
                    makeTodoUpToDate(response);
                  } else {
                    String message = response.getString("message");
                    Log.d(TAG, "Error Message: " + message);
                  }
                } catch (JSONException e) {
                  e.printStackTrace();
                }
              }

              private void makeTodoUpToDate(JSONObject response) throws JSONException {
                todoToUpdate.setRowVersion(response.getInt("row_version"));
                todoToUpdate.setDirty(false);
                dbLoader.updateTodo(todoToUpdate);
              }

            },
            new Response.ErrorListener() {

              @Override
              public void onErrorResponse(VolleyError error) {
                String errorMessage = error.getMessage();
                Log.e(TAG, "Update Todo Error: " + errorMessage);
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

        AppController.getInstance().addToRequestQueue(updateTodosRequest, tag_json_object_request);
      }
    }
    onSyncTodoDataListener.onUpdateTodos();
  }

  private void putTodoData(Todo todoData, JSONObject jsonRequest) throws JSONException {
    jsonRequest.put("todo_online_id", todoData.getTodoOnlineId().trim());
    if (todoData.getListOnlineId() != null) {
      jsonRequest.put("list_online_id", todoData.getListOnlineId().trim());
    } else {
      jsonRequest.put("list_online_id", "");
    }
    jsonRequest.put("title", todoData.getTitle().trim());
    jsonRequest.put("priority", todoData.isPriority() ? 1 : 0);
    jsonRequest.put("due_date", todoData.getDueDate().trim());
    if (todoData.getReminderDateTime() != null) {
      jsonRequest.put("reminder_datetime", todoData.getReminderDateTime().trim());
    } else {
      jsonRequest.put("reminder_datetime", "");
    }
    if (todoData.getDescription() != null) {
      jsonRequest.put("description", todoData.getDescription().trim());
    } else {
      jsonRequest.put("description", "");
    }
    jsonRequest.put("completed", todoData.isCompleted() ? 1 : 0);
    jsonRequest.put("deleted", todoData.getDeleted() ? 1 : 0);
  }

  @NonNull
  private String prepareGetTodosUrl() {
    int end = AppConfig.URL_GET_TODOS.lastIndexOf(":");
    return AppConfig.URL_GET_TODOS.substring(0, end)
        + dbLoader.getTodoRowVersion();
  }

  public interface OnSyncTodoDataListener {
    void onGetTodos();
    void onUpdateTodos();
    void onSyncError(String errorMessage);
  }

}

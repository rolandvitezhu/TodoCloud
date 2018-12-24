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
import com.rolandvitezhu.todocloud.data.Todo;
import com.rolandvitezhu.todocloud.datastorage.DbConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TodoDataSynchronizer extends BaseDataSynchronizer {

  private static final String TAG = TodoDataSynchronizer.class.getSimpleName();
  private static final String TABLE = DbConstants.Todo.DATABASE_TABLE;

  private OnSyncTodoDataListener onSyncTodoDataListener;
  private boolean isUpdateTodoRequestsFinished;
  private int updateTodoRequestCount;
  private int currentUpdateTodoRequest;
  private boolean isInsertTodoRequestsFinished;
  private int insertTodoRequestCount;
  private int currentInsertTodoRequest;

  private ArrayList<Todo> todosToUpdate;
  private ArrayList<Todo> todosToInsert;

  private Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
    @Override
    public void onResponse(JSONObject response) {
      try {
        boolean error = response.getBoolean("error");

        if (!error) {
          nextRowVersion = response.getInt("next_row_version");
          setRowVersionsForTodos(todosToUpdate);
          setRowVersionsForTodos(todosToInsert);
          updateTodos();
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

  void setOnSyncTodoDataListener(OnSyncTodoDataListener onSyncTodoDataListener) {
    this.onSyncTodoDataListener = onSyncTodoDataListener;
  }

  void syncTodoData() {
    initTodoRequestsStates();
    getTodos();
  }

  private void initTodoRequestsStates() {
    isUpdateTodoRequestsFinished = false;
    isInsertTodoRequestsFinished = false;
    todosToUpdate = dbLoader.getTodosToUpdate();
    todosToInsert = dbLoader.getTodosToInsert();
    nextRowVersion = 0;
  }

  private void setRowVersionsForTodos(ArrayList<Todo> todos) {
    for (Todo todo : todos) {
      todo.setRowVersion(nextRowVersion++);
    }
  }

  private void getTodos() {
    String tag_string_request = "request_get_todos";
    StringRequest getTodosRequest = prepareGetTodosRequest();
    AppController.getInstance().addToRequestQueue(getTodosRequest, tag_string_request);
  }

  private void updateTodos() {
    if (!todosToUpdate.isEmpty()) {
      AppController appController = AppController.getInstance();
      String tag_json_object_request = "request_update_todo";
      updateTodoRequestCount = todosToUpdate.size();
      currentUpdateTodoRequest = 1;
      for (final Todo todoToUpdate : todosToUpdate) {
        JsonObjectRequest updateTodoRequest = prepareUpdateTodoRequest(todoToUpdate);
        appController.addToRequestQueue(updateTodoRequest, tag_json_object_request);
      }
    } else {
      isUpdateTodoRequestsFinished = true;
      if (isAllTodoRequestsFinished()) {
        onSyncTodoDataListener.onFinishSyncTodoData();
      }
    }
    insertTodos();
  }

  private void insertTodos() {
    if (!todosToInsert.isEmpty()) {
      AppController appController = AppController.getInstance();
      String tag_json_object_request = "request_insert_todo";
      insertTodoRequestCount = todosToInsert.size();
      currentInsertTodoRequest = 1;
      for (final Todo todoToInsert : todosToInsert) {
        JsonObjectRequest insertTodoRequest = prepareInsertTodoRequest(todoToInsert);
        appController.addToRequestQueue(insertTodoRequest, tag_json_object_request);
      }
    } else {
      isInsertTodoRequestsFinished = true;
      if (isAllTodoRequestsFinished()) {
        onSyncTodoDataListener.onFinishSyncTodoData();
      }
    }
  }

  private StringRequest prepareGetTodosRequest() {
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

                boolean shouldUpdateOrInsertTodos = !todosToUpdate.isEmpty() || !todosToInsert.isEmpty();
                if (shouldUpdateOrInsertTodos)
                  getNextRowVersion(TABLE, responseListener, responseErrorListener);
                else onSyncTodoDataListener.onFinishSyncTodoData();
              } else {
                String message = jsonResponse.getString("message");
                Log.d(TAG, "Error Message: " + message);
                if (message == null) message = "Unknown error";
                onSyncTodoDataListener.onSyncError(message);
              }
            } catch (JSONException e) {
              e.printStackTrace();
              String errorMessage = "Unknown error";
              onSyncTodoDataListener.onSyncError(errorMessage);
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
                dbLoader.fixTodoPositions();
              }
            }
          }

        },
        new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError error) {
            String errorMessage = error.getMessage();
            Log.e(TAG, "Get Todos Error: " + errorMessage);
            if (errorMessage == null) errorMessage = "Unknown error";
            onSyncTodoDataListener.onSyncError(errorMessage);
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
    return getTodosRequest;
  }

  // 000webhost.com don't allow PUT and DELETE requests for free accounts
  private JsonObjectRequest prepareUpdateTodoRequest(final Todo todoToUpdate) {
    JSONObject updateTodoJsonRequest = prepareUpdateTodoJsonRequest(todoToUpdate);
    JsonObjectRequest updateTodoRequest = new JsonObjectRequest(
        JsonObjectRequest.Method.POST,
        AppConfig.URL_UPDATE_TODO,
        updateTodoJsonRequest,
        new Response.Listener<JSONObject>() {

          @Override
          public void onResponse(JSONObject response) {
            Log.d(TAG, "Update Todo Response: " + response);
            try {
              boolean error = response.getBoolean("error");

              if (!error) {
                makeTodoUpToDate();
                if (isLastUpdateTodoRequest()) {
                  isUpdateTodoRequestsFinished = true;
                  if (isAllTodoRequestsFinished()) {
                    onSyncTodoDataListener.onFinishSyncTodoData();
                  }
                }
              } else {
                String message = response.getString("message");
                Log.d(TAG, "Error Message: " + message);
                if (message == null) message = "Unknown error";
                onSyncTodoDataListener.onSyncError(message);
                if (isLastUpdateTodoRequest()) {
                  isUpdateTodoRequestsFinished = true;
                  if (isAllTodoRequestsFinished()) {
                    onSyncTodoDataListener.onFinishSyncTodoData();
                  }
                }
              }
            } catch (JSONException e) {
              e.printStackTrace();
              String errorMessage = "Unknown error";
              onSyncTodoDataListener.onSyncError(errorMessage);
              if (isLastUpdateTodoRequest()) {
                isUpdateTodoRequestsFinished = true;
                if (isAllTodoRequestsFinished()) {
                  onSyncTodoDataListener.onFinishSyncTodoData();
                }
              }
            }
          }

          private void makeTodoUpToDate() {
            todoToUpdate.setDirty(false);
            dbLoader.updateTodo(todoToUpdate);
            dbLoader.fixTodoPositions();
          }

        },
        new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError error) {
            String errorMessage = error.getMessage();
            Log.e(TAG, "Update Todo Error: " + errorMessage);
            if (errorMessage == null) errorMessage = "Unknown error";
            onSyncTodoDataListener.onSyncError(errorMessage);
            if (isLastUpdateTodoRequest()) {
              isUpdateTodoRequestsFinished = true;
              if (isAllTodoRequestsFinished()) {
                onSyncTodoDataListener.onFinishSyncTodoData();
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
    return updateTodoRequest;
  }

  private JSONObject prepareUpdateTodoJsonRequest(final Todo todoToUpdate) {
    JSONObject updateTodoJsonRequest = new JSONObject();
    try {
      putTodoData(todoToUpdate, updateTodoJsonRequest);
    } catch (JSONException e) {
      e.printStackTrace();
      String errorMessage = "Unknown error";
      onSyncTodoDataListener.onSyncError(errorMessage);
      if (isLastUpdateTodoRequest()) {
        isUpdateTodoRequestsFinished = true;
        if (isAllTodoRequestsFinished()) {
          onSyncTodoDataListener.onFinishSyncTodoData();
        }
      }
    }
    return updateTodoJsonRequest;
  }

  private JsonObjectRequest prepareInsertTodoRequest(final Todo todoToInsert) {
    JSONObject insertTodoJsonRequest = prepareInsertTodoJsonRequest(todoToInsert);
    JsonObjectRequest insertTodoRequest = new JsonObjectRequest(
        JsonObjectRequest.Method.POST,
        AppConfig.URL_INSERT_TODO,
        insertTodoJsonRequest,
        new Response.Listener<JSONObject>() {

          @Override
          public void onResponse(JSONObject response) {
            Log.d(TAG, "Insert Todo Response: " + response);
            try {
              boolean error = response.getBoolean("error");

              if (!error) {
                makeTodoUpToDate();
                if (isLastInsertTodoRequest()) {
                  isInsertTodoRequestsFinished = true;
                  if (isAllTodoRequestsFinished()) {
                    onSyncTodoDataListener.onFinishSyncTodoData();
                  }
                }
              } else {
                String message = response.getString("message");
                Log.d(TAG, "Error Message: " + message);
                if (message == null) message = "Unknown error";
                onSyncTodoDataListener.onSyncError(message);
                if (isLastInsertTodoRequest()) {
                  isInsertTodoRequestsFinished = true;
                  if (isAllTodoRequestsFinished()) {
                    onSyncTodoDataListener.onFinishSyncTodoData();
                  }
                }
              }

            } catch (JSONException e) {
              e.printStackTrace();
              String errorMessage = "Unknown error";
              onSyncTodoDataListener.onSyncError(errorMessage);
              if (isLastInsertTodoRequest()) {
                isInsertTodoRequestsFinished = true;
                if (isAllTodoRequestsFinished()) {
                  onSyncTodoDataListener.onFinishSyncTodoData();
                }
              }
            }
          }

          private void makeTodoUpToDate() {
            todoToInsert.setDirty(false);
            dbLoader.updateTodo(todoToInsert);
            dbLoader.fixTodoPositions();
          }

        },
        new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError error) {
            String errorMessage = error.getMessage();
            Log.e(TAG, "Insert Todo Error: " + errorMessage);
            if (errorMessage == null) errorMessage = "Unknown error";
            onSyncTodoDataListener.onSyncError(errorMessage);
            if (isLastInsertTodoRequest()) {
              isInsertTodoRequestsFinished = true;
              if (isAllTodoRequestsFinished()) {
                onSyncTodoDataListener.onFinishSyncTodoData();
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
    return insertTodoRequest;
  }

  private boolean isLastInsertTodoRequest() {
    if (currentInsertTodoRequest++ == insertTodoRequestCount) {
      return true;
    } else {
      return false;
    }
  }

  private boolean isLastUpdateTodoRequest() {
    if (currentUpdateTodoRequest++ == updateTodoRequestCount) {
      return true;
    } else {
      return false;
    }
  }

  @NonNull
  private String prepareGetTodosUrl() {
    int end = AppConfig.URL_GET_TODOS.lastIndexOf(":");
    return AppConfig.URL_GET_TODOS.substring(0, end)
        + dbLoader.getLastTodoRowVersion();
  }

  private JSONObject prepareInsertTodoJsonRequest(final Todo todoToInsert) {
    JSONObject insertTodoJsonRequest = new JSONObject();
    try {
      putTodoData(todoToInsert, insertTodoJsonRequest);
    } catch (JSONException e) {
      e.printStackTrace();
      String errorMessage = "Unknown error";
      onSyncTodoDataListener.onSyncError(errorMessage);
      if (isLastInsertTodoRequest()) {
        isInsertTodoRequestsFinished = true;
        if (isAllTodoRequestsFinished()) {
          onSyncTodoDataListener.onFinishSyncTodoData();
        }
      }
    }
    return insertTodoJsonRequest;
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
    if (todoData.getDueDate() != null) {
      jsonRequest.put("due_date", todoData.getDueDate());
    } else {
      jsonRequest.put("due_date", 0);
    }
    if (todoData.getReminderDateTime() != null) {
      jsonRequest.put("reminder_date_time", todoData.getReminderDateTime());
    } else {
      jsonRequest.put("reminder_date_time", 0);
    }
    if (todoData.getDescription() != null) {
      jsonRequest.put("description", todoData.getDescription().trim());
    } else {
      jsonRequest.put("description", "");
    }
    jsonRequest.put("completed", todoData.isCompleted() ? 1 : 0);
    jsonRequest.put(DbConstants.Todo.KEY_ROW_VERSION, todoData.getRowVersion());
    jsonRequest.put("deleted", todoData.getDeleted() ? 1 : 0);
    jsonRequest.put(DbConstants.Todo.KEY_POSITION, todoData.getPosition());
  }

  private boolean isAllTodoRequestsFinished() {
    return isUpdateTodoRequestsFinished && isInsertTodoRequestsFinished;
  }

  interface OnSyncTodoDataListener {
    void onFinishSyncTodoData();
    void onSyncError(String errorMessage);
  }

}
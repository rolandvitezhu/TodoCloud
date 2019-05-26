package com.rolandvitezhu.todocloud.datasynchronizer;

import android.util.Log;

import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.data.Todo;
import com.rolandvitezhu.todocloud.datastorage.DbConstants;
import com.rolandvitezhu.todocloud.network.ApiService;
import com.rolandvitezhu.todocloud.network.api.rowversion.dto.GetNextRowVersionResponse;
import com.rolandvitezhu.todocloud.network.api.rowversion.service.GetNextRowVersionService;
import com.rolandvitezhu.todocloud.network.api.todo.dto.GetTodosResponse;
import com.rolandvitezhu.todocloud.network.api.todo.dto.InsertTodoRequest;
import com.rolandvitezhu.todocloud.network.api.todo.dto.InsertTodoResponse;
import com.rolandvitezhu.todocloud.network.api.todo.dto.UpdateTodoRequest;
import com.rolandvitezhu.todocloud.network.api.todo.dto.UpdateTodoResponse;
import com.rolandvitezhu.todocloud.network.helper.RetrofitResponseHelper;

import java.util.ArrayList;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;

public class TodoDataSynchronizer extends BaseDataSynchronizer {

  private static final String TAG = TodoDataSynchronizer.class.getSimpleName();

  private ApiService apiService;

  private CompositeDisposable disposable;

  private OnSyncTodoDataListener onSyncTodoDataListener;
  private boolean isUpdateTodoRequestsFinished;
  private int updateTodoRequestCount;
  private int currentUpdateTodoRequest;
  private boolean isInsertTodoRequestsFinished;
  private int insertTodoRequestCount;
  private int currentInsertTodoRequest;

  private ArrayList<Todo> todosToUpdate;
  private ArrayList<Todo> todosToInsert;

  public TodoDataSynchronizer() {
    AppController.getInstance().getAppComponent().inject(this);

    apiService = retrofit.create(ApiService.class);
  }

  void setOnSyncTodoDataListener(OnSyncTodoDataListener onSyncTodoDataListener) {
    this.onSyncTodoDataListener = onSyncTodoDataListener;
  }

  void syncTodoData(CompositeDisposable disposable) {
    this.disposable = disposable;
    initTodoRequestsStates();

    this.disposable.add(
        apiService
        .getTodos(dbLoader.getLastTodoRowVersion())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeWith(createGetTodosDisposableSingleObserver())
    );
  }

  private DisposableSingleObserver<GetTodosResponse>
  createGetTodosDisposableSingleObserver() {
    return new DisposableSingleObserver<GetTodosResponse>() {

      @Override
      public void onSuccess(GetTodosResponse getTodosResponse) {
        Log.d(TAG, "Get Todos Response: " + getTodosResponse);

        if (getTodosResponse != null && getTodosResponse.error.equals("false")) {
          ArrayList<Todo> todos = getTodosResponse.getTodos();

          if (!todos.isEmpty())
            updateTodosInLocalDatabase(todos);

          // Insert nor update todos if any
          boolean shouldUpdateOrInsertTodos = !todosToUpdate.isEmpty() || !todosToInsert.isEmpty();

          if (shouldUpdateOrInsertTodos)
            updateOrInsertTodos();
          else onSyncTodoDataListener.onFinishSyncTodoData();
        } else if (getTodosResponse != null) {
          // Handle error, if any
          String message = getTodosResponse.getMessage();

          if (message == null) message = "Unknown error";
          onSyncTodoDataListener.onSyncError(message);
        }
      }

      @Override
      public void onError(Throwable throwable) {
        Log.d(TAG, "Get Todos Response - onFailure: " + throwable.toString());

        onSyncTodoDataListener.onSyncError(throwable.toString());
      }
    };
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

  private void updateTodos() {
    // Process list
    if (!todosToUpdate.isEmpty()) {
      updateTodoRequestCount = todosToUpdate.size();
      currentUpdateTodoRequest = 1;

      // Process list item
      for (Todo todoToUpdate : todosToUpdate) {
        UpdateTodoRequest updateTodoRequest = new UpdateTodoRequest();

        updateTodoRequest.setTodoOnlineId(todoToUpdate.getTodoOnlineId());
        updateTodoRequest.setListOnlineId(todoToUpdate.getListOnlineId());
        updateTodoRequest.setTitle(todoToUpdate.getTitle());
        updateTodoRequest.setPriority(todoToUpdate.isPriority());
        updateTodoRequest.setDueDate(todoToUpdate.getDueDate());
        updateTodoRequest.setReminderDateTime(todoToUpdate.getReminderDateTime());
        updateTodoRequest.setDescription(todoToUpdate.getDescription());
        updateTodoRequest.setCompleted(todoToUpdate.isCompleted());
        updateTodoRequest.setRowVersion(todoToUpdate.getRowVersion());
        updateTodoRequest.setDeleted(todoToUpdate.getDeleted());
        updateTodoRequest.setPosition(todoToUpdate.getPosition());

        disposable.add(
            apiService
                .updateTodo(updateTodoRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(createUpdateTodoDisposableSingleObserver(todoToUpdate))
        );
      }
      // Sync finished - there are no list items
    } else {
      isUpdateTodoRequestsFinished = true;
      if (isAllTodoRequestsFinished()) {
        onSyncTodoDataListener.onFinishSyncTodoData();
      }
    }
  }

  private DisposableSingleObserver<UpdateTodoResponse>
  createUpdateTodoDisposableSingleObserver(Todo todoToUpdate) {
    return new DisposableSingleObserver<UpdateTodoResponse>() {

      @Override
      public void onSuccess(UpdateTodoResponse updateTodoResponse) {
        Log.d(TAG, "Update Todo Response: " + updateTodoResponse);

        if (updateTodoResponse != null && updateTodoResponse.error.equals("false")) {
          makeTodoUpToDate(todoToUpdate);
          if (isLastUpdateTodoRequest()) {
            isUpdateTodoRequestsFinished = true;
            if (isAllTodoRequestsFinished()) {
              onSyncTodoDataListener.onFinishSyncTodoData();
            }
          }
        } else if (updateTodoResponse != null) {
          String message = updateTodoResponse.getMessage();

          if (message == null) message = "Unknown error";
          onSyncTodoDataListener.onSyncError(message);

          if (isLastUpdateTodoRequest()) {
            isUpdateTodoRequestsFinished = true;
            if (isAllTodoRequestsFinished()) {
              onSyncTodoDataListener.onFinishSyncTodoData();
            }
          }
        }
      }

      @Override
      public void onError(Throwable throwable) {
        Log.d(TAG, "Update Todo Response - onFailure: " + throwable.toString());

        onSyncTodoDataListener.onSyncError(throwable.toString());

        if (isLastUpdateTodoRequest()) {
          isUpdateTodoRequestsFinished = true;
          if (isAllTodoRequestsFinished()) {
            onSyncTodoDataListener.onFinishSyncTodoData();
          }
        }
      }
    };
  }

  private void makeTodoUpToDate(Todo todoToUpdate) {
    todoToUpdate.setDirty(false);
    dbLoader.updateTodo(todoToUpdate);
    dbLoader.fixTodoPositions();
  }

  private void insertTodos() {
    // Process list
    if (!todosToInsert.isEmpty()) {
      insertTodoRequestCount = todosToInsert.size();
      currentInsertTodoRequest = 1;

      // Process list item
      for (Todo todoToInsert : todosToInsert) {
        InsertTodoRequest insertTodoRequest = new InsertTodoRequest();

        insertTodoRequest.setTodoOnlineId(todoToInsert.getTodoOnlineId());
        insertTodoRequest.setListOnlineId(todoToInsert.getListOnlineId());
        insertTodoRequest.setTitle(todoToInsert.getTitle());
        insertTodoRequest.setPriority(todoToInsert.isPriority());
        insertTodoRequest.setDueDate(todoToInsert.getDueDate());
        insertTodoRequest.setReminderDateTime(todoToInsert.getReminderDateTime());
        insertTodoRequest.setDescription(todoToInsert.getDescription());
        insertTodoRequest.setCompleted(todoToInsert.isCompleted());
        insertTodoRequest.setRowVersion(todoToInsert.getRowVersion());
        insertTodoRequest.setDeleted(todoToInsert.getDeleted());
        insertTodoRequest.setPosition(todoToInsert.getPosition());

        disposable.add(
            apiService
            .insertTodo(insertTodoRequest)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(createInsertTodoDisposableSingleObserver(todoToInsert))
        );
      }
      // Sync finished - there are no list items
    } else {
      isInsertTodoRequestsFinished = true;
      if (isAllTodoRequestsFinished()) {
        onSyncTodoDataListener.onFinishSyncTodoData();
      }
    }
  }

  private DisposableSingleObserver<InsertTodoResponse>
  createInsertTodoDisposableSingleObserver(Todo todoToInsert) {
    return new DisposableSingleObserver<InsertTodoResponse>() {

      @Override
      public void onSuccess(InsertTodoResponse insertTodoResponse) {
        Log.d(TAG, "Insert Todo Response: " + insertTodoResponse);

        if (insertTodoResponse != null && insertTodoResponse.error.equals("false")) {
          makeTodoUpToDate(todoToInsert);
          if (isLastInsertTodoRequest()) {
            isInsertTodoRequestsFinished = true;
            if (isAllTodoRequestsFinished()) {
              onSyncTodoDataListener.onFinishSyncTodoData();
            }
          }
        } else if (insertTodoResponse != null) {
          String message = insertTodoResponse.getMessage();

          if (message == null) message = "Unknown error";
          onSyncTodoDataListener.onSyncError(message);

          if (isLastInsertTodoRequest()) {
            isInsertTodoRequestsFinished = true;
            if (isAllTodoRequestsFinished()) {
              onSyncTodoDataListener.onFinishSyncTodoData();
            }
          }
        }
      }

      @Override
      public void onError (Throwable throwable){
        Log.d(TAG, "Insert Todo Response - onFailure: " + throwable.toString());

        onSyncTodoDataListener.onSyncError(throwable.toString());

        if (isLastInsertTodoRequest()) {
          isInsertTodoRequestsFinished = true;
          if (isAllTodoRequestsFinished()) {
            onSyncTodoDataListener.onFinishSyncTodoData();
          }
        }
      }
    };
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

  private void updateOrInsertTodos() {
    GetNextRowVersionService getNextRowVersionService = retrofit.create(GetNextRowVersionService.class);

    Call<GetNextRowVersionResponse> call = getNextRowVersionService.getNextRowVersion(
        DbConstants.Todo.DATABASE_TABLE, dbLoader.getApiKey()
    );

    call.enqueue(new Callback<GetNextRowVersionResponse>() {
      @Override
      public void onResponse(
          Call<GetNextRowVersionResponse> call,
          retrofit2.Response<GetNextRowVersionResponse> response
      ) {
        Log.d(TAG, "Get Next Row Version Response: " + RetrofitResponseHelper.ResponseToJson(response));

        if (RetrofitResponseHelper.IsNoError(response)) {
          nextRowVersion = response.body() != null ? response.body().getNextRowVersion() : 0;

          setRowVersionsForTodos(todosToUpdate);
          setRowVersionsForTodos(todosToInsert);

          updateTodos();
          insertTodos();
        } else if (response.body() != null) {
          String message = response.body().getMessage();

          if (message == null) message = "Unknown error";
          onSyncTodoDataListener.onSyncError(message);
        }
      }

      @Override
      public void onFailure(Call<GetNextRowVersionResponse> call, Throwable t) {
        Log.d(TAG, "Get Next Row Version Response - onFailure: " + t.toString());
      }
    });
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

  private boolean isAllTodoRequestsFinished() {
    return isUpdateTodoRequestsFinished && isInsertTodoRequestsFinished;
  }

  interface OnSyncTodoDataListener {
    void onFinishSyncTodoData();
    void onSyncError(String errorMessage);
  }

}

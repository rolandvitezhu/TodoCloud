package com.rolandvitezhu.todocloud.datasynchronizer;

import android.util.Log;

import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.data.List;
import com.rolandvitezhu.todocloud.datastorage.DbConstants;
import com.rolandvitezhu.todocloud.network.ApiService;
import com.rolandvitezhu.todocloud.network.api.list.dto.GetListsResponse;
import com.rolandvitezhu.todocloud.network.api.list.dto.InsertListRequest;
import com.rolandvitezhu.todocloud.network.api.list.dto.InsertListResponse;
import com.rolandvitezhu.todocloud.network.api.list.dto.UpdateListRequest;
import com.rolandvitezhu.todocloud.network.api.list.dto.UpdateListResponse;
import com.rolandvitezhu.todocloud.network.api.rowversion.dto.GetNextRowVersionResponse;
import com.rolandvitezhu.todocloud.network.api.rowversion.service.GetNextRowVersionService;
import com.rolandvitezhu.todocloud.network.helper.RetrofitResponseHelper;

import java.util.ArrayList;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;

@Singleton
public class ListDataSynchronizer extends BaseDataSynchronizer {

  private static final String TAG = ListDataSynchronizer.class.getSimpleName();

  private ApiService apiService;

  private CompositeDisposable disposable;

  private OnSyncListDataListener onSyncListDataListener;
  private boolean isUpdateListRequestsFinished;
  private int updateListRequestCount;
  private int currentUpdateListRequest;
  private boolean isInsertListRequestsFinished;
  private int insertListRequestCount;
  private int currentInsertListRequest;

  private ArrayList<List> listsToUpdate;
  private ArrayList<List> listsToInsert;

  @Inject
  public ListDataSynchronizer() {
    Objects.requireNonNull(AppController.Companion.getInstance()).getAppComponent().inject(this);

    apiService = retrofit.create(ApiService.class);
  }

  void setOnSyncListDataListener(OnSyncListDataListener onSyncListDataListener) {
    this.onSyncListDataListener = onSyncListDataListener;
  }

  void syncListData(CompositeDisposable disposable) {
    this.disposable = disposable;
    initListRequestsStates();

    this.disposable.add(
        apiService
            .getLists(dbLoader.getLastListRowVersion())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(createGetListsDisposableSingleObserver())
    );
  }

  private DisposableSingleObserver<GetListsResponse>
  createGetListsDisposableSingleObserver() {
    return new DisposableSingleObserver<GetListsResponse>() {

      @Override
      public void onSuccess(GetListsResponse getListsResponse) {
        Log.d(TAG, "Get Lists Response: " + getListsResponse);

        if (getListsResponse != null && getListsResponse.error.equals("false")) {
          ArrayList<List> lists = null;

          lists = getListsResponse.getLists();

          if (!lists.isEmpty())
            updateListsInLocalDatabase(lists);

          boolean shouldUpdateOrInsertLists = !listsToUpdate.isEmpty() || !listsToInsert.isEmpty();

          if (shouldUpdateOrInsertLists)
            updateOrInsertLists();
          else onSyncListDataListener.onFinishSyncListData();
        } else if (getListsResponse != null) {
          // Handle error, if any
          String message = getListsResponse.getMessage();

          if (message == null) message = "Unknown error";
          onSyncListDataListener.onSyncError(message);
        }
      }

      @Override
      public void onError(Throwable throwable) {
        Log.d(TAG, "Get Next Row Version Response - onFailure: " + throwable.toString());
      }
    };
  }

  private void updateOrInsertLists() {
    GetNextRowVersionService getNextRowVersionService = retrofit.create(GetNextRowVersionService.class);

    Call<GetNextRowVersionResponse> call = getNextRowVersionService.getNextRowVersion(
        DbConstants.List.DATABASE_TABLE, dbLoader.getApiKey()
    );

    call.enqueue(new Callback<GetNextRowVersionResponse>() {
      @Override
      public void onResponse(
          Call<GetNextRowVersionResponse> call,
          retrofit2.Response<GetNextRowVersionResponse> response
      ) {
        Log.d(
            TAG,
            "Get Next Row Version Response: "
                + RetrofitResponseHelper.ResponseToJson(response)
        );

        if (RetrofitResponseHelper.IsNoError(response)) {
          nextRowVersion = response.body() != null ? response.body().getNextRowVersion() : 0;

          setRowVersionsForLists(listsToUpdate);
          setRowVersionsForLists(listsToInsert);

          updateLists();
          insertLists();
        } else if (response.body() != null) {
          String message = response.body().getMessage();

          if (message == null) message = "Unknown error";
          onSyncListDataListener.onSyncError(message);
        }
      }

      @Override
      public void onFailure(Call<GetNextRowVersionResponse> call, Throwable t) {
        Log.d(TAG, "Get Next Row Version Response - onFailure: " + t.toString());
      }
    });
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

  private void updateLists() {
    // Process list
    if (!listsToUpdate.isEmpty()) {
      updateListRequestCount = listsToUpdate.size();
      currentUpdateListRequest = 1;

      // Process list item
      for (List listToUpdate : listsToUpdate) {
        UpdateListRequest updateListRequest = new UpdateListRequest();

        updateListRequest.setListOnlineId(listToUpdate.getListOnlineId());
        updateListRequest.setCategoryOnlineId(listToUpdate.getCategoryOnlineId());
        updateListRequest.setTitle(listToUpdate.getTitle());
        updateListRequest.setRowVersion(listToUpdate.getRowVersion());
        updateListRequest.setDeleted(listToUpdate.getDeleted());
        updateListRequest.setPosition(listToUpdate.getPosition());

        disposable.add(
            apiService
                .updateList(updateListRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(createUpdateListDisposableSingleObserver(listToUpdate))
        );
      }
      // Sync finished - there are no list items
    } else {
      isUpdateListRequestsFinished = true;
      if (isAllListRequestsFinished()) {
        onSyncListDataListener.onFinishSyncListData();
      }
    }
  }

  private DisposableSingleObserver<UpdateListResponse>
  createUpdateListDisposableSingleObserver(List listToUpdate) {
    return new DisposableSingleObserver<UpdateListResponse>() {

      @Override
      public void onSuccess(UpdateListResponse updateListResponse) {
        Log.d(TAG, "Update List Response: " + updateListResponse);

        if (updateListResponse != null && updateListResponse.error.equals("false")) {
          makeListUpToDate(listToUpdate);
          if (isLastUpdateListRequest()) {
            isUpdateListRequestsFinished = true;
            if (isAllListRequestsFinished()) {
              onSyncListDataListener.onFinishSyncListData();
            }
          }
        } else if (updateListResponse != null) {
          String message = updateListResponse.getMessage();

          if (message == null) message = "Unknown error";
          onSyncListDataListener.onSyncError(message);

          if (isLastUpdateListRequest()) {
            isUpdateListRequestsFinished = true;
            if (isAllListRequestsFinished()) {
              onSyncListDataListener.onFinishSyncListData();
            }
          }
        }
      }

      @Override
      public void onError(Throwable throwable) {
        Log.d(TAG, "Update List Response - onFailure: " + throwable.toString());

        onSyncListDataListener.onSyncError(throwable.toString());

        if (isLastUpdateListRequest()) {
          isUpdateListRequestsFinished = true;
          if (isAllListRequestsFinished()) {
            onSyncListDataListener.onFinishSyncListData();
          }
        }
      }
    };
  }

  private void makeListUpToDate(List listToUpdate) {
    listToUpdate.setDirty(false);
    dbLoader.updateList(listToUpdate);
  }

  private void insertLists() {
    // Process list
    if (!listsToInsert.isEmpty()) {
      insertListRequestCount = listsToInsert.size();
      currentInsertListRequest = 1;

      // Process list item
      for (List listToInsert : listsToInsert) {
        InsertListRequest insertListRequest = new InsertListRequest();

        insertListRequest.setListOnlineId(listToInsert.getListOnlineId());
        insertListRequest.setCategoryOnlineId(listToInsert.getCategoryOnlineId());
        insertListRequest.setTitle(listToInsert.getTitle());
        insertListRequest.setRowVersion(listToInsert.getRowVersion());
        insertListRequest.setDeleted(listToInsert.getDeleted());
        insertListRequest.setPosition(listToInsert.getPosition());

        disposable.add(
            apiService
                .insertList(insertListRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(createInsertListDisposableSingleObserver(listToInsert))
        );
      }
      // Sync finished - there are no list items
    } else {
      isInsertListRequestsFinished = true;
      if (isAllListRequestsFinished()) {
        onSyncListDataListener.onFinishSyncListData();
      }
    }
  }

  private DisposableSingleObserver<InsertListResponse>
  createInsertListDisposableSingleObserver(List listToInsert) {
    return new DisposableSingleObserver<InsertListResponse>() {
      @Override
      public void onSuccess(InsertListResponse insertListResponse) {
        Log.d(TAG, "Insert List Response: " + insertListResponse);

        if (insertListResponse != null && insertListResponse.error.equals("false")) {
          makeListUpToDate(listToInsert);
          if (isLastInsertListRequest()) {
            isInsertListRequestsFinished = true;
            if (isAllListRequestsFinished()) {
              onSyncListDataListener.onFinishSyncListData();
            }
          }
        } else if (insertListResponse != null) {
          String message = insertListResponse.getMessage();

          if (message == null) message = "Unknown error";
          onSyncListDataListener.onSyncError(message);

          if (isLastInsertListRequest()) {
            isInsertListRequestsFinished = true;
            if (isAllListRequestsFinished()) {
              onSyncListDataListener.onFinishSyncListData();
            }
          }
        }
      }

      @Override
      public void onError(Throwable throwable) {
        Log.d(TAG, "Insert List Response - onFailure: " + throwable.toString());

        onSyncListDataListener.onSyncError(throwable.toString());

        if (isLastInsertListRequest()) {
          isInsertListRequestsFinished = true;
          if (isAllListRequestsFinished()) {
            onSyncListDataListener.onFinishSyncListData();
          }
        }
      }
    };
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

  private boolean isAllListRequestsFinished() {
    return isUpdateListRequestsFinished && isInsertListRequestsFinished;
  }

  interface OnSyncListDataListener {
    void onFinishSyncListData();
    void onSyncError(String errorMessage);
  }

}

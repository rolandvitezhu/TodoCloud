package com.rolandvitezhu.todocloud.datasynchronizer;

import android.util.Log;

import com.rolandvitezhu.todocloud.data.List;
import com.rolandvitezhu.todocloud.datastorage.DbConstants;
import com.rolandvitezhu.todocloud.network.api.list.dto.GetListsResponse;
import com.rolandvitezhu.todocloud.network.api.list.dto.InsertListRequest;
import com.rolandvitezhu.todocloud.network.api.list.dto.InsertListResponse;
import com.rolandvitezhu.todocloud.network.api.list.dto.UpdateListRequest;
import com.rolandvitezhu.todocloud.network.api.list.dto.UpdateListResponse;
import com.rolandvitezhu.todocloud.network.api.list.service.GetListsService;
import com.rolandvitezhu.todocloud.network.api.list.service.InsertListService;
import com.rolandvitezhu.todocloud.network.api.list.service.UpdateListService;
import com.rolandvitezhu.todocloud.network.api.rowversion.dto.GetNextRowVersionResponse;
import com.rolandvitezhu.todocloud.network.api.rowversion.service.GetNextRowVersionService;
import com.rolandvitezhu.todocloud.network.helper.RetrofitResponseHelper;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;

public class ListDataSynchronizer extends BaseDataSynchronizer {

  private static final String TAG = ListDataSynchronizer.class.getSimpleName();

  private OnSyncListDataListener onSyncListDataListener;
  private boolean isUpdateListRequestsFinished;
  private int updateListRequestCount;
  private int currentUpdateListRequest;
  private boolean isInsertListRequestsFinished;
  private int insertListRequestCount;
  private int currentInsertListRequest;

  private ArrayList<List> listsToUpdate;
  private ArrayList<List> listsToInsert;

  void setOnSyncListDataListener(OnSyncListDataListener onSyncListDataListener) {
    this.onSyncListDataListener = onSyncListDataListener;
  }

  void syncListData() {
    initListRequestsStates();

    GetListsService getListsService = retrofit.create(GetListsService.class);

    Call<GetListsResponse> call = getListsService.
        getLists(dbLoader.getLastTodoRowVersion(), dbLoader.getApiKey());

    call.enqueue(new Callback<GetListsResponse>() {
      @Override
      public void onResponse(Call<GetListsResponse> call, retrofit2.Response<GetListsResponse> response) {
        Log.d(TAG, "Get Lists Response: " + RetrofitResponseHelper.ResponseToJson(response));

        if (RetrofitResponseHelper.IsNoError(response)) {
          ArrayList<List> lists = null;

          if (response.body() != null) {
            lists = response.body().getLists();
          }
          if (lists != null && !lists.isEmpty()) {
            updateListsInLocalDatabase(lists);
          }

          boolean shouldUpdateOrInsertLists = !listsToUpdate.isEmpty() || !listsToInsert.isEmpty();

          if (shouldUpdateOrInsertLists)
            updateOrInsertLists();
          else onSyncListDataListener.onFinishSyncListData();
        } else if (response.body() != null) {
          // Handle error, if any
          String message = response.body().getMessage();

          if (message == null) message = "Unknown error";
          onSyncListDataListener.onSyncError(message);
        }
      }

      @Override
      public void onFailure(Call<GetListsResponse> call, Throwable t) {
        Log.d(TAG, "Get Next Row Version Response - onFailure: " + t.toString());
      }
    });
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
        UpdateListService updateListService = retrofit.create(UpdateListService.class);

        UpdateListRequest updateListRequest = new UpdateListRequest();

        updateListRequest.setListOnlineId(listToUpdate.getListOnlineId());
        updateListRequest.setCategoryOnlineId(listToUpdate.getCategoryOnlineId());
        updateListRequest.setTitle(listToUpdate.getTitle());
        updateListRequest.setRowVersion(listToUpdate.getRowVersion());
        updateListRequest.setDeleted(listToUpdate.getDeleted());
        updateListRequest.setPosition(listToUpdate.getPosition());

        Call<UpdateListResponse> call = updateListService.updateList(
            dbLoader.getApiKey(),
            updateListRequest
        );

        call.enqueue(new Callback<UpdateListResponse>() {
          @Override
          public void onResponse(Call<UpdateListResponse> call, retrofit2.Response<UpdateListResponse> response) {
            Log.d(TAG, "Update List Response: " + RetrofitResponseHelper.ResponseToJson(response));

            if (RetrofitResponseHelper.IsNoError(response)) {
              makeListUpToDate(listToUpdate);
              if (isLastUpdateListRequest()) {
                isUpdateListRequestsFinished = true;
                if (isAllListRequestsFinished()) {
                  onSyncListDataListener.onFinishSyncListData();
                }
              }
            } else if (response.body() != null) {
              String message = response.body().getMessage();

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
          public void onFailure(Call<UpdateListResponse> call, Throwable t) {
            Log.d(TAG, "Update List Response - onFailure: " + t.toString());

            onSyncListDataListener.onSyncError(t.toString());

            if (isLastUpdateListRequest()) {
              isUpdateListRequestsFinished = true;
              if (isAllListRequestsFinished()) {
                onSyncListDataListener.onFinishSyncListData();
              }
            }
          }
        });
      }
      // Sync finished - there are no list items
    } else {
      isUpdateListRequestsFinished = true;
      if (isAllListRequestsFinished()) {
        onSyncListDataListener.onFinishSyncListData();
      }
    }
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
        InsertListService insertListService = retrofit.create(InsertListService.class);

        InsertListRequest insertListRequest = new InsertListRequest();

        insertListRequest.setListOnlineId(listToInsert.getListOnlineId());
        insertListRequest.setCategoryOnlineId(listToInsert.getCategoryOnlineId());
        insertListRequest.setTitle(listToInsert.getTitle());
        insertListRequest.setRowVersion(listToInsert.getRowVersion());
        insertListRequest.setDeleted(listToInsert.getDeleted());
        insertListRequest.setPosition(listToInsert.getPosition());

        Call<InsertListResponse> call = insertListService.insertList(
            dbLoader.getApiKey(), insertListRequest
        );

        call.enqueue(new Callback<InsertListResponse>() {
          @Override
          public void onResponse(Call<InsertListResponse> call, retrofit2.Response<InsertListResponse> response) {
            Log.d(TAG, "Insert List Response: " + RetrofitResponseHelper.ResponseToJson(response));

            if (RetrofitResponseHelper.IsNoError(response)) {
              makeListUpToDate(listToInsert);
              if (isLastInsertListRequest()) {
                isInsertListRequestsFinished = true;
                if (isAllListRequestsFinished()) {
                  onSyncListDataListener.onFinishSyncListData();
                }
              }
            } else if (response.body() != null) {
              String message = response.body().getMessage();

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
          public void onFailure(Call<InsertListResponse> call, Throwable t) {
            Log.d(TAG, "Insert List Response - onFailure: " + t.toString());

            onSyncListDataListener.onSyncError(t.toString());

            if (isLastInsertListRequest()) {
              isInsertListRequestsFinished = true;
              if (isAllListRequestsFinished()) {
                onSyncListDataListener.onFinishSyncListData();
              }
            }
          }
        });
      }
      // Sync finished - there are no list items
    } else {
      isInsertListRequestsFinished = true;
      if (isAllListRequestsFinished()) {
        onSyncListDataListener.onFinishSyncListData();
      }
    }
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

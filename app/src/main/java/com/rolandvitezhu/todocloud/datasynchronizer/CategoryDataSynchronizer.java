package com.rolandvitezhu.todocloud.datasynchronizer;

import android.util.Log;

import com.rolandvitezhu.todocloud.data.Category;
import com.rolandvitezhu.todocloud.datastorage.DbConstants;
import com.rolandvitezhu.todocloud.network.api.category.dto.GetCategoriesResponse;
import com.rolandvitezhu.todocloud.network.api.category.dto.InsertCategoryRequest;
import com.rolandvitezhu.todocloud.network.api.category.dto.InsertCategoryResponse;
import com.rolandvitezhu.todocloud.network.api.category.dto.UpdateCategoryRequest;
import com.rolandvitezhu.todocloud.network.api.category.dto.UpdateCategoryResponse;
import com.rolandvitezhu.todocloud.network.api.category.service.GetCategoriesService;
import com.rolandvitezhu.todocloud.network.api.category.service.InsertCategoryService;
import com.rolandvitezhu.todocloud.network.api.category.service.UpdateCategoryService;
import com.rolandvitezhu.todocloud.network.api.rowversion.dto.GetNextRowVersionResponse;
import com.rolandvitezhu.todocloud.network.api.rowversion.service.GetNextRowVersionService;
import com.rolandvitezhu.todocloud.network.helper.RetrofitResponseHelper;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;

public class CategoryDataSynchronizer extends BaseDataSynchronizer {

  private static final String TAG = CategoryDataSynchronizer.class.getSimpleName();

  private OnSyncCategoryDataListener onSyncCategoryDataListener;
  private boolean isUpdateCategoryRequestsFinished;
  private int updateCategoryRequestCount;
  private int currentUpdateCategoryRequest;
  private boolean isInsertCategoryRequestsFinished;
  private int insertCategoryRequestCount;
  private int currentInsertCategoryRequest;

  private ArrayList<Category> categoriesToUpdate;
  private ArrayList<Category> categoriesToInsert;

  void setOnSyncCategoryDataListener(
      OnSyncCategoryDataListener onSyncCategoryDataListener
  ) {
    this.onSyncCategoryDataListener = onSyncCategoryDataListener;
  }

  void syncCategoryData() {
    initCategoryRequestsStates();

    GetCategoriesService getCategoriesService = retrofit.create(GetCategoriesService.class);

    Call<GetCategoriesResponse> call = getCategoriesService.
        getCategories(dbLoader.getLastTodoRowVersion(), dbLoader.getApiKey());

    call.enqueue(new Callback<GetCategoriesResponse>() {
      @Override
      public void onResponse(Call<GetCategoriesResponse> call, retrofit2.Response<GetCategoriesResponse> response) {
        Log.d(TAG, "Get Categories Response: " + RetrofitResponseHelper.ResponseToJson(response));

        if (RetrofitResponseHelper.IsNoError(response)) {
          ArrayList<Category> categories = null;

          if (response.body() != null) {
            categories = response.body().getCategories();
          }
          if (categories != null && !categories.isEmpty()) {
            updateCategoriesInLocalDatabase(categories);
          }

          boolean shouldUpdateOrInsertCategories = !categoriesToUpdate.isEmpty() || !categoriesToInsert.isEmpty();

          if (shouldUpdateOrInsertCategories)
            updateOrInsertCategories();
          else onSyncCategoryDataListener.onFinishSyncCategoryData();
        } else if (response.body() != null) {
          // Handle error, if any
          String message = response.body().getMessage();

          if (message == null) message = "Unknown error";
          onSyncCategoryDataListener.onSyncError(message);
        }
      }

      @Override
      public void onFailure(Call<GetCategoriesResponse> call, Throwable t) {
        Log.d(TAG, "Get Next Row Version Response - onFailure: " + t.toString());
      }
    });
  }

  private void updateOrInsertCategories() {
    GetNextRowVersionService getNextRowVersionService = retrofit.create(GetNextRowVersionService.class);

    Call<GetNextRowVersionResponse> call = getNextRowVersionService.getNextRowVersion(
        DbConstants.Category.DATABASE_TABLE, dbLoader.getApiKey()
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

          setRowVersionsForCategories(categoriesToUpdate);
          setRowVersionsForCategories(categoriesToInsert);

          updateCategories();
          insertCategories();
        } else if (response.body() != null) {
          String message = response.body().getMessage();

          if (message == null) message = "Unknown error";
          onSyncCategoryDataListener.onSyncError(message);
        }
      }

      @Override
      public void onFailure(Call<GetNextRowVersionResponse> call, Throwable t) {
        Log.d(TAG, "Get Next Row Version Response - onFailure: " + t.toString());
      }
    });
  }

  private void initCategoryRequestsStates() {
    isUpdateCategoryRequestsFinished = false;
    isInsertCategoryRequestsFinished = false;
    categoriesToUpdate = dbLoader.getCategoriesToUpdate();
    categoriesToInsert = dbLoader.getCategoriesToInsert();
    nextRowVersion = 0;
  }

  private void setRowVersionsForCategories(ArrayList<Category> categories) {
    for (Category category : categories) {
      category.setRowVersion(nextRowVersion++);
    }
  }

  private void updateCategories() {
    // Process list
    if (!categoriesToUpdate.isEmpty()) {
      updateCategoryRequestCount = categoriesToUpdate.size();
      currentUpdateCategoryRequest = 1;

      // Process list item
      for (Category categoryToUpdate : categoriesToUpdate) {
        UpdateCategoryService updateCategoryService = retrofit.create(UpdateCategoryService.class);

        UpdateCategoryRequest updateCategoryRequest = new UpdateCategoryRequest();

        updateCategoryRequest.setCategoryOnlineId(categoryToUpdate.getCategoryOnlineId());
        updateCategoryRequest.setTitle(categoryToUpdate.getTitle());
        updateCategoryRequest.setRowVersion(categoryToUpdate.getRowVersion());
        updateCategoryRequest.setDeleted(categoryToUpdate.getDeleted());
        updateCategoryRequest.setPosition(categoryToUpdate.getPosition());

        Call<UpdateCategoryResponse> call = updateCategoryService.updateCategory(
            dbLoader.getApiKey(),
            updateCategoryRequest
        );

        call.enqueue(new Callback<UpdateCategoryResponse>() {
          @Override
          public void onResponse(Call<UpdateCategoryResponse> call, retrofit2.Response<UpdateCategoryResponse> response) {
            Log.d(TAG, "Update Category Response: " + RetrofitResponseHelper.ResponseToJson(response));

            if (RetrofitResponseHelper.IsNoError(response)) {
              makeCategoryUpToDate(categoryToUpdate);
              if (isLastUpdateCategoryRequest()) {
                isUpdateCategoryRequestsFinished = true;
                if (isAllCategoryRequestsFinished()) {
                  onSyncCategoryDataListener.onFinishSyncCategoryData();
                }
              }
            } else if (response.body() != null) {
              String message = response.body().getMessage();

              if (message == null) message = "Unknown error";
              onSyncCategoryDataListener.onSyncError(message);

              if (isLastUpdateCategoryRequest()) {
                isUpdateCategoryRequestsFinished = true;
                if (isAllCategoryRequestsFinished()) {
                  onSyncCategoryDataListener.onFinishSyncCategoryData();
                }
              }
            }
          }

          @Override
          public void onFailure(Call<UpdateCategoryResponse> call, Throwable t) {
            Log.d(TAG, "Update Category Response - onFailure: " + t.toString());

            onSyncCategoryDataListener.onSyncError(t.toString());

            if (isLastUpdateCategoryRequest()) {
              isUpdateCategoryRequestsFinished = true;
              if (isAllCategoryRequestsFinished()) {
                onSyncCategoryDataListener.onFinishSyncCategoryData();
              }
            }
          }
        });
      }
      // Sync finished - there are no list items
    } else {
      isUpdateCategoryRequestsFinished = true;
      if (isAllCategoryRequestsFinished()) {
        onSyncCategoryDataListener.onFinishSyncCategoryData();
      }
    }
  }

  private void insertCategories() {
    // Process list
    if (!categoriesToInsert.isEmpty()) {
      insertCategoryRequestCount = categoriesToInsert.size();
      currentInsertCategoryRequest = 1;

      // Process list item
      for (Category categoryToInsert : categoriesToInsert) {
        InsertCategoryService insertCategoryService = retrofit.create(InsertCategoryService.class);

        InsertCategoryRequest insertCategoryRequest = new InsertCategoryRequest();

        insertCategoryRequest.setCategoryOnlineId(categoryToInsert.getCategoryOnlineId());
        insertCategoryRequest.setTitle(categoryToInsert.getTitle());
        insertCategoryRequest.setRowVersion(categoryToInsert.getRowVersion());
        insertCategoryRequest.setDeleted(categoryToInsert.getDeleted());
        insertCategoryRequest.setPosition(categoryToInsert.getPosition());

        Call<InsertCategoryResponse> call = insertCategoryService.insertCategory(
            dbLoader.getApiKey(), insertCategoryRequest
        );

        call.enqueue(new Callback<InsertCategoryResponse>() {
          @Override
          public void onResponse(Call<InsertCategoryResponse> call, retrofit2.Response<InsertCategoryResponse> response) {
            Log.d(TAG, "Insert Category Response: " + RetrofitResponseHelper.ResponseToJson(response));

            if (RetrofitResponseHelper.IsNoError(response)) {
              makeCategoryUpToDate(categoryToInsert);
              if (isLastInsertCategoryRequest()) {
                isInsertCategoryRequestsFinished = true;
                if (isAllCategoryRequestsFinished()) {
                  onSyncCategoryDataListener.onFinishSyncCategoryData();
                }
              }
            } else if (response.body() != null) {
              String message = response.body().getMessage();

              if (message == null) message = "Unknown error";
              onSyncCategoryDataListener.onSyncError(message);

              if (isLastInsertCategoryRequest()) {
                isInsertCategoryRequestsFinished = true;
                if (isAllCategoryRequestsFinished()) {
                  onSyncCategoryDataListener.onFinishSyncCategoryData();
                }
              }
            }
          }

          @Override
          public void onFailure(Call<InsertCategoryResponse> call, Throwable t) {
            Log.d(TAG, "Insert Category Response - onFailure: " + t.toString());

            onSyncCategoryDataListener.onSyncError(t.toString());

            if (isLastInsertCategoryRequest()) {
              isInsertCategoryRequestsFinished = true;
              if (isAllCategoryRequestsFinished()) {
                onSyncCategoryDataListener.onFinishSyncCategoryData();
              }
            }
          }
        });
      }
      // Sync finished - there are no list items
    } else {
      isInsertCategoryRequestsFinished = true;
      if (isAllCategoryRequestsFinished()) {
        onSyncCategoryDataListener.onFinishSyncCategoryData();
      }
    }
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

  private void makeCategoryUpToDate(Category categoryToUpdate) {
    categoryToUpdate.setDirty(false);
    dbLoader.updateCategory(categoryToUpdate);
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

  private boolean isAllCategoryRequestsFinished() {
    return isUpdateCategoryRequestsFinished && isInsertCategoryRequestsFinished;
  }

  interface OnSyncCategoryDataListener {
    void onFinishSyncCategoryData();
    void onSyncError(String errorMessage);
  }

}

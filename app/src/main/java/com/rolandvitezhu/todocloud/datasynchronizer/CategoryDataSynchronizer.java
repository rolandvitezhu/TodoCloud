package com.rolandvitezhu.todocloud.datasynchronizer;

import android.util.Log;

import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.data.Category;
import com.rolandvitezhu.todocloud.datastorage.DbConstants;
import com.rolandvitezhu.todocloud.network.ApiService;
import com.rolandvitezhu.todocloud.network.api.category.dto.GetCategoriesResponse;
import com.rolandvitezhu.todocloud.network.api.category.dto.InsertCategoryRequest;
import com.rolandvitezhu.todocloud.network.api.category.dto.InsertCategoryResponse;
import com.rolandvitezhu.todocloud.network.api.category.dto.UpdateCategoryRequest;
import com.rolandvitezhu.todocloud.network.api.category.dto.UpdateCategoryResponse;
import com.rolandvitezhu.todocloud.network.api.rowversion.dto.GetNextRowVersionResponse;
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
public class CategoryDataSynchronizer extends BaseDataSynchronizer {

  private static final String TAG = CategoryDataSynchronizer.class.getSimpleName();

  private ApiService apiService;

  private CompositeDisposable disposable;

  private OnSyncCategoryDataListener onSyncCategoryDataListener;
  private boolean isUpdateCategoryRequestsFinished;
  private int updateCategoryRequestCount;
  private int currentUpdateCategoryRequest;
  private boolean isInsertCategoryRequestsFinished;
  private int insertCategoryRequestCount;
  private int currentInsertCategoryRequest;

  private ArrayList<Category> categoriesToUpdate;
  private ArrayList<Category> categoriesToInsert;

  @Inject
  public CategoryDataSynchronizer() {
    Objects.requireNonNull(AppController.Companion.getInstance()).getAppComponent().inject(this);

    apiService = retrofit.create(ApiService.class);
  }

  void setOnSyncCategoryDataListener(
      OnSyncCategoryDataListener onSyncCategoryDataListener
  ) {
    this.onSyncCategoryDataListener = onSyncCategoryDataListener;
  }

  void syncCategoryData(CompositeDisposable disposable) {
    this.disposable = disposable;;
    initCategoryRequestsStates();

    this.disposable.add(
        apiService
            .getCategories(dbLoader.getLastCategoryRowVersion())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(createGetCategoriesDisposableSingleObserver())
    );
  }

  private DisposableSingleObserver<GetCategoriesResponse>
  createGetCategoriesDisposableSingleObserver() {
    return new DisposableSingleObserver<GetCategoriesResponse>() {
      @Override
      public void onSuccess(GetCategoriesResponse getCategoriesResponse) {
        Log.d(TAG, "Get Categories Response: " + getCategoriesResponse);

        if (getCategoriesResponse != null && getCategoriesResponse.error.equals("false")) {
          ArrayList<Category> categories = null;

          categories = getCategoriesResponse.getCategories();

          if (!categories.isEmpty())
            updateCategoriesInLocalDatabase(categories);

          boolean shouldUpdateOrInsertCategories = !categoriesToUpdate.isEmpty() || !categoriesToInsert.isEmpty();

          if (shouldUpdateOrInsertCategories)
            updateOrInsertCategories();
          else onSyncCategoryDataListener.onFinishSyncCategoryData();
        } else if (getCategoriesResponse != null) {
          // Handle error, if any
          String message = getCategoriesResponse.getMessage();

          if (message == null) message = "Unknown error";
          onSyncCategoryDataListener.onSyncError(message);
        }
      }

      @Override
      public void onError(Throwable throwable) {
        Log.d(TAG, "Get Next Row Version Response - onFailure: " + throwable.toString());
      }
    };
  }

  private void updateOrInsertCategories() {
    Call<GetNextRowVersionResponse> call = apiService.getNextRowVersion(
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
                + RetrofitResponseHelper.Companion.ResponseToJson(response)
        );

        if (RetrofitResponseHelper.Companion.IsNoError(response)) {
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
        UpdateCategoryRequest updateCategoryRequest = new UpdateCategoryRequest();

        updateCategoryRequest.setCategoryOnlineId(categoryToUpdate.getCategoryOnlineId());
        updateCategoryRequest.setTitle(categoryToUpdate.getTitle());
        updateCategoryRequest.setRowVersion(categoryToUpdate.getRowVersion());
        updateCategoryRequest.setDeleted(categoryToUpdate.getDeleted());
        updateCategoryRequest.setPosition(categoryToUpdate.getPosition());

        disposable.add(
            apiService
                .updateCategory(updateCategoryRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(createUpdateCategoryDisposableSingleObserver(categoryToUpdate))
        );
      }
      // Sync finished - there are no list items
    } else {
      isUpdateCategoryRequestsFinished = true;
      if (isAllCategoryRequestsFinished()) {
        onSyncCategoryDataListener.onFinishSyncCategoryData();
      }
    }
  }

  private DisposableSingleObserver<UpdateCategoryResponse>
  createUpdateCategoryDisposableSingleObserver(Category categoryToUpdate) {
    return new DisposableSingleObserver<UpdateCategoryResponse>() {
      @Override
      public void onSuccess(UpdateCategoryResponse updateCategoryResponse) {
        Log.d(TAG, "Update Category Response: " + updateCategoryResponse);

        if (updateCategoryResponse != null && updateCategoryResponse.error.equals("false")) {
          makeCategoryUpToDate(categoryToUpdate);
          if (isLastUpdateCategoryRequest()) {
            isUpdateCategoryRequestsFinished = true;
            if (isAllCategoryRequestsFinished()) {
              onSyncCategoryDataListener.onFinishSyncCategoryData();
            }
          }
        } else if (updateCategoryResponse != null) {
          String message = updateCategoryResponse.getMessage();

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
      public void onError(Throwable throwable) {
        Log.d(TAG, "Update Category Response - onFailure: " + throwable.toString());

        onSyncCategoryDataListener.onSyncError(throwable.toString());

        if (isLastUpdateCategoryRequest()) {
          isUpdateCategoryRequestsFinished = true;
          if (isAllCategoryRequestsFinished()) {
            onSyncCategoryDataListener.onFinishSyncCategoryData();
          }
        }
      }
    };
  }

  private void insertCategories() {
    // Process list
    if (!categoriesToInsert.isEmpty()) {
      insertCategoryRequestCount = categoriesToInsert.size();
      currentInsertCategoryRequest = 1;

      // Process list item
      for (Category categoryToInsert : categoriesToInsert) {
        InsertCategoryRequest insertCategoryRequest = new InsertCategoryRequest();

        insertCategoryRequest.setCategoryOnlineId(categoryToInsert.getCategoryOnlineId());
        insertCategoryRequest.setTitle(categoryToInsert.getTitle());
        insertCategoryRequest.setRowVersion(categoryToInsert.getRowVersion());
        insertCategoryRequest.setDeleted(categoryToInsert.getDeleted());
        insertCategoryRequest.setPosition(categoryToInsert.getPosition());

        disposable.add(
            apiService
                .insertCategory(insertCategoryRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(createInsertCategoryDisposableSingleObserver(categoryToInsert))
        );
      }
      // Sync finished - there are no list items
    } else {
      isInsertCategoryRequestsFinished = true;
      if (isAllCategoryRequestsFinished()) {
        onSyncCategoryDataListener.onFinishSyncCategoryData();
      }
    }
  }

  private DisposableSingleObserver<InsertCategoryResponse>
  createInsertCategoryDisposableSingleObserver(Category categoryToInsert) {
    return new DisposableSingleObserver<InsertCategoryResponse>() {
      @Override
      public void onSuccess(InsertCategoryResponse insertCategoryResponse) {
        Log.d(TAG, "Insert Category Response: " + insertCategoryResponse);

        if (insertCategoryResponse != null && insertCategoryResponse.error.equals("false")) {
          makeCategoryUpToDate(categoryToInsert);
          if (isLastInsertCategoryRequest()) {
            isInsertCategoryRequestsFinished = true;
            if (isAllCategoryRequestsFinished()) {
              onSyncCategoryDataListener.onFinishSyncCategoryData();
            }
          }
        } else if (insertCategoryResponse != null) {
          String message = insertCategoryResponse.getMessage();

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
      public void onError(Throwable throwable) {
        Log.d(TAG, "Insert Category Response - onFailure: " + throwable.toString());

        onSyncCategoryDataListener.onSyncError(throwable.toString());

        if (isLastInsertCategoryRequest()) {
          isInsertCategoryRequestsFinished = true;
          if (isAllCategoryRequestsFinished()) {
            onSyncCategoryDataListener.onFinishSyncCategoryData();
          }
        }
      }
    };
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

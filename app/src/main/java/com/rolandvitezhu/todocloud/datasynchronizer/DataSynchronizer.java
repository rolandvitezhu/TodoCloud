package com.rolandvitezhu.todocloud.datasynchronizer;

import com.rolandvitezhu.todocloud.app.AppController;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;

public class DataSynchronizer implements
    TodoDataSynchronizer.OnSyncTodoDataListener,
    ListDataSynchronizer.OnSyncListDataListener,
    CategoryDataSynchronizer.OnSyncCategoryDataListener {

  @Inject
  TodoDataSynchronizer todoDataSynchronizer;
  @Inject
  ListDataSynchronizer listDataSynchronizer;
  @Inject
  CategoryDataSynchronizer categoryDataSynchronizer;

  private OnSyncDataListener onSyncDataListener;

  private boolean isLastTodoRequestProcessed;
  private boolean isLastListRequestProcessed;
  private boolean isLastCategoryRequestProcessed;

  public DataSynchronizer() {
    AppController.getInstance().getAppComponent().inject(this);
    todoDataSynchronizer.setOnSyncTodoDataListener(this);
    listDataSynchronizer.setOnSyncListDataListener(this);
    categoryDataSynchronizer.setOnSyncCategoryDataListener(this);
  }

  public void setOnSyncDataListener(OnSyncDataListener onSyncDataListener) {
    this.onSyncDataListener = onSyncDataListener;
  }

  /**
   * Call update and insert methods only, if get requests processed successfully. Otherwise the
   * client will have data in the local database with the biggest current row_version before it
   * get all of the data from the remote database, which is missing in the local database. Hence
   * it don't will get that data.
   * If an error occurs in the processing of the requests, they should be aborted and start the
   * whole processing from the beginning, with the call of get methods.
   */
  public void syncData(CompositeDisposable disposable) {
    initializeSyncStates();
    todoDataSynchronizer.syncTodoData(disposable);
    listDataSynchronizer.syncListData(disposable);
    categoryDataSynchronizer.syncCategoryData(disposable);
  }

  private void initializeSyncStates() {
    isLastTodoRequestProcessed = false;
    isLastListRequestProcessed = false;
    isLastCategoryRequestProcessed = false;
  }

  private boolean isSynchronizationCompleted() {
    return isLastTodoRequestProcessed
        && isLastListRequestProcessed
        && isLastCategoryRequestProcessed;
  }

  @Override
  public void onFinishSyncTodoData() {
    isLastTodoRequestProcessed = true;
    if (isSynchronizationCompleted()) onSyncDataListener.onFinishSyncData();
  }

  @Override
  public void onFinishSyncListData() {
    onSyncDataListener.onFinishSyncListData();
    isLastListRequestProcessed = true;
    if (isSynchronizationCompleted()) onSyncDataListener.onFinishSyncData();
  }

  @Override
  public void onFinishSyncCategoryData() {
    onSyncDataListener.onFinishSyncCategoryData();
    isLastCategoryRequestProcessed = true;
    if (isSynchronizationCompleted()) onSyncDataListener.onFinishSyncData();
  }

  @Override
  public void onSyncError(String errorMessage) {
    onSyncDataListener.onSyncError(errorMessage);
    onSyncDataListener.onFinishSyncData();
  }

  public interface OnSyncDataListener {
    void onFinishSyncListData();
    void onFinishSyncCategoryData();
    void onFinishSyncData();
    void onSyncError(String errorMessage);
  }

}

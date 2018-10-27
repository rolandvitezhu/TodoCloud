package com.rolandvitezhu.todocloud.datasynchronizer;

import com.rolandvitezhu.todocloud.datastorage.DbLoader;

public class DataSynchronizer implements
    TodoDataSynchronizer.OnSyncTodoDataListener,
    ListDataSynchronizer.OnSyncListDataListener,
    CategoryDataSynchronizer.OnSyncCategoryDataListener {

  private TodoDataSynchronizer todoDataSynchronizer;
  private ListDataSynchronizer listDataSynchronizer;
  private CategoryDataSynchronizer categoryDataSynchronizer;

  private OnSyncDataListener onSyncDataListener;

  private boolean isLastTodoRequestProcessed;
  private boolean isLastListRequestProcessed;
  private boolean isLastCategoryRequestProcessed;

  public DataSynchronizer(DbLoader dbLoader) {
    todoDataSynchronizer = new TodoDataSynchronizer(dbLoader);
    listDataSynchronizer = new ListDataSynchronizer(dbLoader);
    categoryDataSynchronizer = new CategoryDataSynchronizer(dbLoader);
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
  public void syncData() {
    initializeSyncStates();
    todoDataSynchronizer.syncTodoData();
    listDataSynchronizer.syncListData();
    categoryDataSynchronizer.syncCategoryData();
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

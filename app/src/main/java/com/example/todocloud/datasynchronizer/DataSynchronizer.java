package com.example.todocloud.datasynchronizer;

import com.example.todocloud.datastorage.DbLoader;

public class DataSynchronizer implements
    TodoDataSynchronizer.OnSyncTodoDataListener,
    ListDataSynchronizer.OnSyncListDataListener,
    CategoryDataSynchronizer.OnSyncCategoryDataListener {

  private TodoDataSynchronizer todoDataSynchronizer;
  private ListDataSynchronizer listDataSynchronizer;
  private CategoryDataSynchronizer categoryDataSynchronizer;

  private OnSyncDataListener onSyncDataListener;

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

  public void syncData() {
    todoDataSynchronizer.getTodos();
  }

  @Override
  public void onFinishGetTodos() {
    listDataSynchronizer.getLists();
  }

  @Override
  public void onFinishGetLists() {
    categoryDataSynchronizer.getCategories();
  }

  @Override
  public void onFinishGetCategories() {
    todoDataSynchronizer.updateTodos();
  }

  @Override
  public void onFinishUpdateTodos() {
    listDataSynchronizer.updateLists();
  }

  @Override
  public void onFinishUpdateLists() {
    categoryDataSynchronizer.updateCategories();
  }

  @Override
  public void onFinishUpdateCategories() {
    todoDataSynchronizer.insertTodos();
  }

  @Override
  public void onFinishInsertTodos() {
    listDataSynchronizer.insertLists();
  }

  @Override
  public void onFinishInsertLists() {
    categoryDataSynchronizer.insertCategories();
  }

  @Override
  public void onProcessLastListRequest() {
    onSyncDataListener.onProcessLastListRequest();
  }

  @Override
  public void onProcessLastCategoryRequest() {
    onSyncDataListener.onProcessLastCategoryRequest();
  }

  @Override
  public void onSyncError(String errorMessage) {
    onSyncDataListener.onSyncError(errorMessage);
  }

  public interface OnSyncDataListener {
    void onProcessLastListRequest();
    void onProcessLastCategoryRequest();
    void onFinishSyncData();
    void onSyncError(String errorMessage);
  }

}

package com.rolandvitezhu.todocloud.ui.activity.main.viewmodel;

import android.arch.lifecycle.ViewModel;

public class SearchListsViewModel extends ViewModel {

//  private MutableLiveData<List<PredefinedList>> predefinedLists;
//  private PredefinedList predefinedList;
  private String queryText;

//  public LiveData<List<PredefinedList>> getPredefinedLists() {
//    if (predefinedLists == null)
//      predefinedLists = new MutableLiveData<>();
//
//    return predefinedLists;
//  }
//
//  public void setPredefinedLists(List<PredefinedList> predefinedLists) {
//    if (this.predefinedLists == null)
//      this.predefinedLists = new MutableLiveData<>();
//
//    this.predefinedLists.setValue(predefinedLists);
//  }
//
//  public PredefinedList getPredefinedList() {
//    return predefinedList;
//  }
//
//  public void setPredefinedList(PredefinedList predefinedList) {
//    this.predefinedList = predefinedList;
//  }

  public String getQueryText() {
    return queryText;
  }

  public void setQueryText(String queryText) {
    this.queryText = queryText;
  }
}

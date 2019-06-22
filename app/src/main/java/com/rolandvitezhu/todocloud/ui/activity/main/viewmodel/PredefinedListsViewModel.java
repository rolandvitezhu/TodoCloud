package com.rolandvitezhu.todocloud.ui.activity.main.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import com.rolandvitezhu.todocloud.data.PredefinedList;

import java.util.List;

public class PredefinedListsViewModel extends ViewModel {

  private MutableLiveData<List<PredefinedList>> predefinedLists;
  private PredefinedList predefinedList;

  public LiveData<List<PredefinedList>> getPredefinedLists() {
    if (predefinedLists == null)
      predefinedLists = new MutableLiveData<>();

    return predefinedLists;
  }

  public void setPredefinedLists(List<PredefinedList> predefinedLists) {
    if (this.predefinedLists == null)
      this.predefinedLists = new MutableLiveData<>();

    this.predefinedLists.setValue(predefinedLists);
  }

  public PredefinedList getPredefinedList() {
    return predefinedList;
  }

  public void setPredefinedList(PredefinedList predefinedList) {
    this.predefinedList = predefinedList;
  }
}

package com.rolandvitezhu.todocloud.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import java.util.List;

public class ListsViewModel extends ViewModel {

  private MutableLiveData<List<com.rolandvitezhu.todocloud.data.List>> lists;
  private com.rolandvitezhu.todocloud.data.List list;

  public LiveData<List<com.rolandvitezhu.todocloud.data.List>> getLists() {
    if (lists == null)
      lists = new MutableLiveData<>();

    return lists;
  }

  public void setLists(List<com.rolandvitezhu.todocloud.data.List> lists) {
    if (this.lists == null)
      this.lists = new MutableLiveData<>();

    this.lists.setValue(lists);
  }

  public com.rolandvitezhu.todocloud.data.List getList() {
    if (list == null)
      list = new com.rolandvitezhu.todocloud.data.List();

    return list;
  }

  public void setList(com.rolandvitezhu.todocloud.data.List list) {
    this.list = list;
  }
}

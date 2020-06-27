package com.rolandvitezhu.todocloud.ui.activity.main.viewmodel;

import androidx.lifecycle.ViewModel;

public class SearchListsViewModel extends ViewModel {

  private String queryText;

  public String getQueryText() {
    return queryText;
  }

  public void setQueryText(String queryText) {
    this.queryText = queryText;
  }
}

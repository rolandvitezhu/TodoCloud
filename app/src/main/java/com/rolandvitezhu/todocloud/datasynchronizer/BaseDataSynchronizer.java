package com.rolandvitezhu.todocloud.datasynchronizer;

import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.datastorage.DbLoader;

import javax.inject.Inject;

import retrofit2.Retrofit;

public class BaseDataSynchronizer {

  @Inject
  DbLoader dbLoader;
  @Inject
  Retrofit retrofit;

  int nextRowVersion;

  public BaseDataSynchronizer() {
    AppController.getInstance().getAppComponent().inject(this);
  }
}

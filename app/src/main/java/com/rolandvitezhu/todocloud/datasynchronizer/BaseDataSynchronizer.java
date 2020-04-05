package com.rolandvitezhu.todocloud.datasynchronizer;

import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.datastorage.DbLoader;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Retrofit;

@Singleton
public class BaseDataSynchronizer {

  @Inject
  DbLoader dbLoader;
  @Inject
  Retrofit retrofit;

  int nextRowVersion;

  @Inject
  public BaseDataSynchronizer() {
    Objects.requireNonNull(AppController.Companion.getInstance()).getAppComponent().inject(this);
  }
}

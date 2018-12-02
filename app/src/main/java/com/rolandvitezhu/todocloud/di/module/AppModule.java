package com.rolandvitezhu.todocloud.di.module;

import android.app.Application;

import com.rolandvitezhu.todocloud.adapter.CategoryAdapter;
import com.rolandvitezhu.todocloud.adapter.ListAdapter;
import com.rolandvitezhu.todocloud.adapter.PredefinedListAdapter;
import com.rolandvitezhu.todocloud.adapter.TodoAdapter;
import com.rolandvitezhu.todocloud.datastorage.DbLoader;
import com.rolandvitezhu.todocloud.helper.SessionManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {

  private Application application;

  public AppModule(Application application) {
    this.application = application;
  }

  @Provides
  @Singleton
  Application provideApplication() {
    return application;
  }

  @Provides
  @Singleton
  DbLoader provideDbLoader() {
    return new DbLoader();
  }

  @Provides
  @Singleton
  SessionManager provideSessionManager() {
    return new SessionManager();
  }

  @Provides
  @Singleton
  TodoAdapter provideTodoAdapter() {
    return new TodoAdapter();
  }

  @Provides
  @Singleton
  PredefinedListAdapter providePredefinedListAdapter() {
    return new PredefinedListAdapter();
  }

  @Provides
  @Singleton
  ListAdapter provideListAdapter() {
    return new ListAdapter();
  }

  @Provides
  @Singleton
  CategoryAdapter provideCategoryAdapter() {
    return new CategoryAdapter();
  }
}

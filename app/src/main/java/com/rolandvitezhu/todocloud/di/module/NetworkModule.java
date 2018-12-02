package com.rolandvitezhu.todocloud.di.module;

import com.rolandvitezhu.todocloud.datasynchronizer.CategoryDataSynchronizer;
import com.rolandvitezhu.todocloud.datasynchronizer.DataSynchronizer;
import com.rolandvitezhu.todocloud.datasynchronizer.ListDataSynchronizer;
import com.rolandvitezhu.todocloud.datasynchronizer.TodoDataSynchronizer;
import com.rolandvitezhu.todocloud.datasynchronizer.UserDataSynchronizer;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class NetworkModule {

  @Provides
  @Singleton
  UserDataSynchronizer provideUserDataSynchronizer() {
    return new UserDataSynchronizer();
  }

  @Provides
  @Singleton
  TodoDataSynchronizer provideTodoDataSynchronizer() {
    return new TodoDataSynchronizer();
  }

  @Provides
  @Singleton
  ListDataSynchronizer provideListDataSynchronizer() {
    return new ListDataSynchronizer();
  }

  @Provides
  @Singleton
  CategoryDataSynchronizer provideCategoryDataSynchronizer() {
    return new CategoryDataSynchronizer();
  }

  @Provides
  @Singleton
  DataSynchronizer provideDataSynchronizer() {
    return new DataSynchronizer();
  }
}

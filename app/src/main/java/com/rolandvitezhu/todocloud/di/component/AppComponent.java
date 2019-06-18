package com.rolandvitezhu.todocloud.di.component;

import com.rolandvitezhu.todocloud.adapter.TodoAdapter;
import com.rolandvitezhu.todocloud.datastorage.asynctask.UpdateViewModelTask;
import com.rolandvitezhu.todocloud.datasynchronizer.BaseDataSynchronizer;
import com.rolandvitezhu.todocloud.datasynchronizer.DataSynchronizer;
import com.rolandvitezhu.todocloud.di.module.AppModule;
import com.rolandvitezhu.todocloud.di.module.NetworkModule;
import com.rolandvitezhu.todocloud.fragment.LoginUserFragment;
import com.rolandvitezhu.todocloud.fragment.ModifyPasswordFragment;
import com.rolandvitezhu.todocloud.fragment.MoveListDialogFragment;
import com.rolandvitezhu.todocloud.fragment.RegisterUserFragment;
import com.rolandvitezhu.todocloud.fragment.ResetPasswordFragment;
import com.rolandvitezhu.todocloud.fragment.SearchFragment;
import com.rolandvitezhu.todocloud.fragment.TodoListFragment;
import com.rolandvitezhu.todocloud.service.ReminderService;
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity;
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.MainListFragment;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {AppModule.class, NetworkModule.class})
public interface AppComponent {
  void inject(MainActivity mainActivity);
  void inject(MainListFragment mainListFragment);
  void inject(LoginUserFragment loginUserFragment);
  void inject(RegisterUserFragment registerUserFragment);
  void inject(ModifyPasswordFragment modifyPasswordFragment);
  void inject(ResetPasswordFragment resetPasswordFragment);
  void inject(TodoListFragment todoListFragment);
  void inject(SearchFragment searchFragment);
  void inject(MoveListDialogFragment moveListDialogFragment);
  void inject(TodoAdapter todoAdapter);
  void inject(UpdateViewModelTask updateViewModelTask);
  void inject(BaseDataSynchronizer baseDataSynchronizer);
  void inject(ReminderService reminderService);
  void inject(DataSynchronizer dataSynchronizer);
}

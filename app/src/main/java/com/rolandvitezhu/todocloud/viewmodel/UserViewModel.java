package com.rolandvitezhu.todocloud.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import com.rolandvitezhu.todocloud.data.User;

public class UserViewModel extends ViewModel {

  private MutableLiveData<User> user;

  public LiveData<User> getUser() {
    if (user == null)
      user = new MutableLiveData<>();

    return user;
  }

  public void setUser(User user) {
    if (this.user == null)
      this.user = new MutableLiveData<>();

    this.user.setValue(user);
  }
}

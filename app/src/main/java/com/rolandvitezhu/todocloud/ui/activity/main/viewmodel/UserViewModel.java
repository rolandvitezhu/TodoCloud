package com.rolandvitezhu.todocloud.ui.activity.main.viewmodel;

import com.rolandvitezhu.todocloud.data.User;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

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

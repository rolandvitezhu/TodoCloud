package com.rolandvitezhu.todocloud.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import com.rolandvitezhu.todocloud.data.Category;

import java.util.HashMap;
import java.util.List;

public class CategoriesViewModel extends ViewModel {

  private MutableLiveData<HashMap<Category, List<com.rolandvitezhu.todocloud.data.List>>> hmCategories;
  private Category category;

  public LiveData<HashMap<Category, List<com.rolandvitezhu.todocloud.data.List>>> getCategories() {
    if (hmCategories == null)
      hmCategories = new MutableLiveData<>();

    return hmCategories;
  }

  public void setCategories(HashMap<Category, List<com.rolandvitezhu.todocloud.data.List>> hmCategories) {
    if (this.hmCategories == null)
      this.hmCategories = new MutableLiveData<>();

    this.hmCategories.setValue(hmCategories);
  }

  public Category getCategory() {
    return category;
  }

  public void setCategory(Category category) {
    this.category = category;
  }
}

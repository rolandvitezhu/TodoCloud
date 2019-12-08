package com.rolandvitezhu.todocloud.ui.activity.main.viewmodel;

import com.rolandvitezhu.todocloud.data.Category;

import java.util.LinkedHashMap;
import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CategoriesViewModel extends ViewModel {

  private MutableLiveData<LinkedHashMap<Category, List<com.rolandvitezhu.todocloud.data.List>>> lhmCategories;
  private Category category;

  public LiveData<LinkedHashMap<Category, List<com.rolandvitezhu.todocloud.data.List>>> getCategories() {
    if (lhmCategories == null)
      lhmCategories = new MutableLiveData<>();

    return lhmCategories;
  }

  public void setCategories(LinkedHashMap<Category, List<com.rolandvitezhu.todocloud.data.List>> hmCategories) {
    if (this.lhmCategories == null)
      this.lhmCategories = new MutableLiveData<>();

    this.lhmCategories.setValue(hmCategories);
  }

  public Category getCategory() {
    return category;
  }

  public void setCategory(Category category) {
    this.category = category;
  }
}

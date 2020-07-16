package com.rolandvitezhu.todocloud.ui.activity.main.viewmodel;

import com.rolandvitezhu.todocloud.data.Category;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

  /**
   * Toggles the "isSelected" property for the category passed as a parameter in the view model.
   * @param category
   */
  public void toggleCategorySelected(@NotNull Category category) {
    for (Map.Entry<Category, List<com.rolandvitezhu.todocloud.data.List>> entry :
        Objects.requireNonNull(lhmCategories.getValue()).entrySet()) {
      if (entry.getKey().get_id() == category.get_id()) {
        entry.getKey().setSelected(!category.isSelected());
        setCategories(lhmCategories.getValue());
        break;
      }
    }
  }

  /**
   * Toggles the "isSelected" property for the list passed as a parameter in the view model.
   * @param list
   */
  public void toggleListSelected(@NotNull com.rolandvitezhu.todocloud.data.List list) {
    for (Map.Entry<Category, List<com.rolandvitezhu.todocloud.data.List>> entry :
        Objects.requireNonNull(lhmCategories.getValue()).entrySet()) {
      for (com.rolandvitezhu.todocloud.data.List listItem : entry.getValue()) {
        if (listItem.get_id() == list.get_id())
        {
          listItem.setSelected(!list.isSelected());
          setCategories(lhmCategories.getValue());
          return;
        }
      }
    }
  }

  /**
   * Set the "isSelected" property of all the Category and List objects in the dictionary data
   * structure.
   */
  public void deselectItems() {
    for (Map.Entry<Category, List<com.rolandvitezhu.todocloud.data.List>> entry :
        Objects.requireNonNull(lhmCategories.getValue()).entrySet()) {
      entry.getKey().setSelected(false);
      for (com.rolandvitezhu.todocloud.data.List listItem : entry.getValue()) {
        listItem.setSelected(false);
      }
    }
    setCategories(lhmCategories.getValue());
  }
}

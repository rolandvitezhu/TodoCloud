package com.rolandvitezhu.todocloud.datastorage.asynctask;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.data.Category;
import com.rolandvitezhu.todocloud.data.PredefinedList;
import com.rolandvitezhu.todocloud.data.Todo;
import com.rolandvitezhu.todocloud.datastorage.DbLoader;
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.CategoriesViewModel;
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.ListsViewModel;
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.PredefinedListsViewModel;
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.TodosViewModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.inject.Inject;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

public class UpdateViewModelTask extends AsyncTask<Bundle, Void, Void> {

  @Inject
  DbLoader dbLoader;

  private Object viewModel;
  private FragmentActivity activity;
  private List<com.rolandvitezhu.todocloud.data.List> lists;
  private List<PredefinedList> predefinedLists;
  private ArrayList<Todo> todos;
  private LinkedHashMap<Category, List<com.rolandvitezhu.todocloud.data.List>> lhmCategories;

  public UpdateViewModelTask(Object viewModel, FragmentActivity activity) {
    this.viewModel = viewModel;
    this.activity = activity;
    AppController.Companion.getInstance().getAppComponent().inject(this);
  }

  @Override
  protected Void doInBackground(Bundle... params) {
    if (viewModel instanceof TodosViewModel) {
      updateTodosViewModel();
    } else if (viewModel instanceof PredefinedListsViewModel) {
      updatePredefinedListsViewModel();
    } else if (viewModel instanceof CategoriesViewModel) {
      updateCategoriesViewModel();
    } else if (viewModel instanceof ListsViewModel) {
      updateListsViewModel();
    }

    return null;
  }

  @Override
  protected void onPostExecute(Void aVoid) {
    super.onPostExecute(aVoid);

    if (viewModel instanceof TodosViewModel) {
      ((TodosViewModel) viewModel).setTodos(todos);
    } else if (viewModel instanceof PredefinedListsViewModel) {
      ((PredefinedListsViewModel) viewModel).setPredefinedLists(predefinedLists);
    } else if (viewModel instanceof CategoriesViewModel) {
      ((CategoriesViewModel) viewModel).setCategories(lhmCategories);
    } else if (viewModel instanceof ListsViewModel) {
      ((ListsViewModel) viewModel).setLists(lists);
    }
  }

  private void updateTodosViewModel() {
    if (activity != null) {
      if (((TodosViewModel) viewModel).isPredefinedList()) {
        // Get todos for predefined lists and search lists
        PredefinedListsViewModel predefinedListsViewModel =
            ViewModelProviders.of(activity).get(PredefinedListsViewModel.class);

        String selectFromDB = predefinedListsViewModel.getPredefinedList().getSelectFromDB();
        todos = dbLoader.getPredefinedListTodos(selectFromDB);
      } else {
        // Get todos for custom lists
        ListsViewModel listsViewModel =
            ViewModelProviders.of(activity).get(ListsViewModel.class);

        String listOnlineId = listsViewModel.getList().getListOnlineId();
        todos = dbLoader.getTodosByListOnlineId(listOnlineId);
      }
    }
  }

  private void updatePredefinedListsViewModel() {
    String todayPredefinedListWhere = dbLoader.prepareTodayPredefinedListWhere();
    String next7DaysPredefinedListWhere = dbLoader.prepareNext7DaysPredefinedListWhere();
    String allPredefinedListWhere = dbLoader.prepareAllPredefinedListWhere();
    String completedPredefinedListWhere = dbLoader.prepareCompletedPredefinedListWhere();

    Context context = AppController.Companion.getAppContext();

    predefinedLists = new ArrayList<>();

    predefinedLists.add(
        new PredefinedList(context.getString(R.string.all_today), todayPredefinedListWhere));
    predefinedLists.add(
        new PredefinedList(context.getString(R.string.all_next7days), next7DaysPredefinedListWhere));
    predefinedLists.add(
        new PredefinedList(context.getString(R.string.all_all), allPredefinedListWhere));
    predefinedLists.add(
        new PredefinedList(context.getString(R.string.all_completed), completedPredefinedListWhere));
  }

  private void updateListsViewModel() {
    lists = dbLoader.getListsNotInCategory();
  }

  private void updateCategoriesViewModel() {
    List<Category> categories = dbLoader.getCategories();
    lhmCategories = new LinkedHashMap<>();

    for (Category category : categories) {
      List<com.rolandvitezhu.todocloud.data.List> listData =
          dbLoader.getListsByCategoryOnlineId(category.getCategoryOnlineId());
      lhmCategories.put(category, listData);
    }
  }

}

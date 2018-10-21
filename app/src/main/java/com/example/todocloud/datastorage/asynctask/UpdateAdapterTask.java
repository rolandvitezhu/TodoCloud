package com.example.todocloud.datastorage.asynctask;

import android.os.AsyncTask;
import android.os.Bundle;

import com.example.todocloud.adapter.CategoryAdapter;
import com.example.todocloud.adapter.ListAdapter;
import com.example.todocloud.adapter.PredefinedListAdapter;
import com.example.todocloud.adapter.TodoAdapter;
import com.example.todocloud.data.Category;
import com.example.todocloud.data.PredefinedList;
import com.example.todocloud.data.Todo;
import com.example.todocloud.datastorage.DbLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UpdateAdapterTask extends AsyncTask<Bundle, Void, Void> {

  private DbLoader dbLoader;
  private Object adapter;
  private List<com.example.todocloud.data.List> lists;
  private ArrayList<Todo> todos;
  private List<Category> categories;
  private HashMap<Category, List<com.example.todocloud.data.List>> hmCategories;

  public UpdateAdapterTask(DbLoader dbLoader, Object adapter) {
    this.dbLoader = dbLoader;
    this.adapter = adapter;
  }

  @Override
  protected Void doInBackground(Bundle... params) {
    if (adapter instanceof TodoAdapter) {
      updateTodoAdapter(params);
    } else if (adapter instanceof PredefinedListAdapter) {
      updatePredefinedListAdapter();
    } else if (adapter instanceof CategoryAdapter) {
      updateCategoryAdapter();
    } else if (adapter instanceof ListAdapter) {
      updateListAdapter();
    }
    return null;
  }

  @Override
  protected void onPostExecute(Void aVoid) {
    super.onPostExecute(aVoid);
    if (adapter instanceof TodoAdapter) {
      ((TodoAdapter) adapter).updateDataSet(todos);
      ((TodoAdapter) adapter).notifyDataSetChanged();
    } else if (adapter instanceof PredefinedListAdapter) {
      ((PredefinedListAdapter) adapter).notifyDataSetChanged();
    } else if (adapter instanceof CategoryAdapter) {
      ((CategoryAdapter) adapter).update(categories, hmCategories);
      ((CategoryAdapter) adapter).notifyDataSetChanged();
    } else if (adapter instanceof ListAdapter) {
      ((ListAdapter) adapter).update(lists);
      ((ListAdapter) adapter).notifyDataSetChanged();
    }
  }

  private void updateTodoAdapter(Bundle... params) {
    if (isPredefinedList(params[0])) {
      String selectFromDB = params[0].getString("selectFromDB");
      todos = dbLoader.getPredefinedListTodos(selectFromDB);
    } else {
      String listOnlineId = params[0].getString("listOnlineId");
      todos = dbLoader.getTodosByListOnlineId(listOnlineId);
    }

    // TODO: Remove dummy data
//    ArrayList<Todo> dummyData = new ArrayList<>();
//    dummyData.add(new Todo(0, "0", "0", "0", "0", false, 0L, 0L, "0", false, 0, false, false, 0));
//
//    todos = dummyData;
  }

  private boolean isPredefinedList(Bundle param) {
    return param.get("listOnlineId") == null;
  }

  private void updatePredefinedListAdapter() {
    String todayPredefinedListWhere = dbLoader.prepareTodayPredefinedListWhere();
    String next7DaysPredefinedListWhere = dbLoader.prepareNext7DaysPredefinedListWhere();
    String allPredefinedListWhere = dbLoader.prepareAllPredefinedListWhere();
    String completedPredefinedListWhere = dbLoader.prepareCompletedPredefinedListWhere();
    PredefinedList predefinedListToday = new PredefinedList(
        "0",
        todayPredefinedListWhere/*,
        0*/
    );
    PredefinedList predefinedListNext7Days = new PredefinedList(
        "1",
        next7DaysPredefinedListWhere/*,
        0*/
    );
    PredefinedList predefinedListAll = new PredefinedList(
        "2",
        allPredefinedListWhere/*,
        0*/);
    PredefinedList predefinedListCompleted = new PredefinedList(
        "3",
        completedPredefinedListWhere/*,
        0*/);
    PredefinedListAdapter predefinedListAdapter = (PredefinedListAdapter) adapter;
    predefinedListAdapter.addItem(predefinedListToday);
    predefinedListAdapter.addItem(predefinedListNext7Days);
    predefinedListAdapter.addItem(predefinedListAll);
    predefinedListAdapter.addItem(predefinedListCompleted);
  }

  private void updateCategoryAdapter() {
    categories = dbLoader.getCategories();
    hmCategories = new HashMap<>();
    for (Category category : categories) {
      String categoryOnlineId = category.getCategoryOnlineId();
      List<com.example.todocloud.data.List> listData = dbLoader.getListsByCategoryOnlineId(
          categoryOnlineId
      );
      hmCategories.put(category, listData);
    }
  }

  private void updateListAdapter() {
    lists = dbLoader.getListsNotInCategory();
  }

}

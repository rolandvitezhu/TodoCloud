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
import com.example.todocloud.datastorage.DbConstants;
import com.example.todocloud.datastorage.DbLoader;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

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
      todos = dbLoader.getTodos((String) params[0].get("selectFromDB"));
    } else {
      todos = dbLoader.getTodosByListOnlineId(params[0].getString("listOnlineId"));
    }
  }

  private boolean isPredefinedList(Bundle param) {
    return param.get("listOnlineId") == null;
  }

  private void updatePredefinedListAdapter() {
    PredefinedListAdapter predefinedListAdapter = (PredefinedListAdapter) adapter;
    PredefinedList predefinedListToday = new PredefinedList(
        "0",
        DbConstants.Todo.KEY_DUE_DATE + "='" + today() + "'",
        0
    );
    PredefinedList predefinedListNext7Days = new PredefinedList(
        "1",
        prepareNext7DaysWhere(),
        0
    );
    PredefinedList predefinedListAll = new PredefinedList(
        "2",
        null,
        0);
    PredefinedList predefinedListCompleted = new PredefinedList(
        "3",
        DbConstants.Todo.KEY_COMPLETED
            + "="
            + 1
            + " AND "
            + DbConstants.Todo.KEY_USER_ONLINE_ID
            + "='"
            + dbLoader.getUserOnlineId()
            + "'"
            + " AND "
            + DbConstants.Todo.KEY_DELETED
            + "="
            + 0,
        0);
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

  private String today() {
    String pattern = "yyyy.MM.dd.";
    Locale defaultLocale = Locale.getDefault();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
        pattern,
        defaultLocale
    );
    Date today = new Date();
    return simpleDateFormat.format(today);
  }

  private String prepareNext7DaysWhere() {
    String pattern = "yyyy.MM.dd.";
    Locale defaultLocale = Locale.getDefault();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
        pattern,
        defaultLocale
    );
    Date today = new Date();
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(today);

    StringBuilder whereStringBuilder = new StringBuilder();
    String todayString = simpleDateFormat.format(today);
    appendToday(whereStringBuilder, todayString);
    for (int i = 0; i < 6; i++) {
      String nextDayString = prepareNextDayStringWhere(simpleDateFormat, calendar);
      appendNextDay(whereStringBuilder, nextDayString);
    }
    prepareWhereStringBuilderPostfix(whereStringBuilder);
    String where = whereStringBuilder.toString();
    return where;
  }

  private void appendToday(StringBuilder whereStringBuilder, String todayString) {
    whereStringBuilder.append(
        "("
            + DbConstants.Todo.KEY_DUE_DATE
            + "='"
            + todayString
            + "' OR "
    );
  }

  private String prepareNextDayStringWhere(SimpleDateFormat simpleDateFormat, Calendar calendar) {
    calendar.roll(Calendar.DAY_OF_MONTH, true);
    Date nextDay = new Date();
    nextDay.setTime(calendar.getTimeInMillis());
    return simpleDateFormat.format(nextDay);
  }

  private void appendNextDay(StringBuilder whereStringBuilder, String nextDayString) {
    whereStringBuilder.append(
        DbConstants.Todo.KEY_DUE_DATE
            + "='"
            + nextDayString
            + "' OR "
    );
  }

  private void prepareWhereStringBuilderPostfix(StringBuilder whereStringBuilder) {
    whereStringBuilder.delete(whereStringBuilder.length()-4, whereStringBuilder.length());
    whereStringBuilder.append(')');
  }

}

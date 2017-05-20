package com.example.todocloud.datastorage.asynctask;

import android.os.AsyncTask;
import android.os.Bundle;

import com.example.todocloud.adapter.CategoryAdapter;
import com.example.todocloud.adapter.ListAdapter;
import com.example.todocloud.adapter.PredefinedListAdapter;
import com.example.todocloud.adapter.TodoAdapter;
import com.example.todocloud.data.Category;
import com.example.todocloud.data.PredefinedListItem;
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
    // Az adapter típusának megfelelő műveleteket hajtjuk végre.
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
    // Az adapter típusának megfelelő műveleteket hajtjuk végre.
    if (adapter instanceof TodoAdapter) {
      ((TodoAdapter) adapter).update(todos);
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

  /**
   * Frissíti a TodoAdapter-t.
   * @param params Paramétertől függően adott List Todo-it kérjük le, vagy az összes todo-t.
   */
  private void updateTodoAdapter(Bundle... params) {
    // Ha nem kapott listOnlineId-t, akkor az előre definiált lista elemein történt a kattintás, a
    // szerint kell feltölteni az ArrayList-et Todo-kkal.
    if (params[0].get("listOnlineId") == null) {
      todos = dbLoader.getTodos((String) params[0].get("selectFromDB"));
    } else {
      todos = dbLoader.getTodosByListOnlineId(params[0].getString("listOnlineId"));
    }
  }

  /**
   * Frissíti a PredefinedListAdapter-t.
   */
  private void updatePredefinedListAdapter() {
    PredefinedListAdapter predefinedListAdapter = (PredefinedListAdapter) adapter;
    predefinedListAdapter.addItem(new PredefinedListItem("0",
        DbConstants.Todo.KEY_DUE_DATE + "='" +today()+ "'", 0));
    predefinedListAdapter.addItem(new PredefinedListItem("1",
        next7Days(), 0));
    predefinedListAdapter.addItem(new PredefinedListItem("2", null, 0));
    predefinedListAdapter.addItem(new PredefinedListItem("3",
        DbConstants.Todo.KEY_COMPLETED + "=" + 1 + " AND " +
            DbConstants.Todo.KEY_USER_ONLINE_ID + "='" + dbLoader.getUserOnlineId() + "'" + " AND " +
            DbConstants.Todo.KEY_DELETED + "=" + 0, 0));
  }

  /**
   * Frissíti a CategoryAdapter-t.
   */
  private void updateCategoryAdapter() {
    categories = dbLoader.getCategories();
    hmCategories = new HashMap<>();
    List<com.example.todocloud.data.List> listData = new ArrayList<>();
    for (Category category : categories) {
      listData = null;
      listData = dbLoader.getListsByCategoryOnlineId(category.getCategoryOnlineId());
      // Ha az adott kategóriához tartozik lista, akkor a listData-ba tesszük, egyébként üres
      // listData-t hozunk létre.
      if (listData == null) {
        listData = new ArrayList<com.example.todocloud.data.List>();
      }
      hmCategories.put(category, listData);
    }
  }

  /**
   * Frissíti a ListAdapter-t.
   */
  private void updateListAdapter() {
    lists = dbLoader.getListsNotInCategory();
  }

  /**
   * Visszaadja a mai dátumot az adatbázisban tárolt formátumban.
   * @return A mai dátum String-ként.
   */
  private String today() {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
        "yyyy.MM.dd.", Locale.getDefault());
    return simpleDateFormat.format(new Date());
  }

  /**
   * Összeállítja a "Következő 7 nap" lista "where" feltételét.
   * @return A "Következő 7 nap" lista "where" feltétele.
   */
  private String next7Days() {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
        "yyyy.MM.dd.", Locale.getDefault());
    Date date = new Date();
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    StringBuilder sb = new StringBuilder();
    sb.append("(" + DbConstants.Todo.KEY_DUE_DATE + "='" +
        simpleDateFormat.format(date) + "' OR ");
    for (int i = 0; i < 6; i++) {
      calendar.roll(Calendar.DAY_OF_MONTH, true);
      date.setTime(calendar.getTimeInMillis());
      sb.append(DbConstants.Todo.KEY_DUE_DATE + "='" + simpleDateFormat.format(date) + "' OR ");
    }
    sb.delete(sb.length()-4, sb.length());
    sb.append(')');
    String where = sb.toString();
    return where;
  }

}

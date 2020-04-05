package com.rolandvitezhu.todocloud.helper;

import android.content.Context;
import android.content.SharedPreferences;

import com.rolandvitezhu.todocloud.app.AppController;

public class SharedPreferencesHelper {

  public static final String PREFERENCE_NAME_SORT = "Sort";
  public static final String KEY_SORT_BY_DUE_DATE_ASC = "sortByDueDateAsc";
  public static final String KEY_SORT_BY_PRIORITY = "sortByPriority";

  public static void setBooleanPreference(String preferenceName, String preferenceKey, boolean preferenceValue) {
    Context applicationContext = AppController.Companion.getAppContext();
    SharedPreferences sharedPreferences = null;
    if (applicationContext != null) {
      sharedPreferences = applicationContext.getSharedPreferences(
          preferenceName,
          Context.MODE_PRIVATE
      );
    }
    SharedPreferences.Editor editor = null;
    if (sharedPreferences != null) {
      editor = sharedPreferences.edit();
    }
    if (editor != null) {
      editor.putBoolean(preferenceKey, preferenceValue);
      editor.apply();
    }
  }

  public static boolean getPreference(String preferenceName, String preferenceKey) {
    Context applicationContext = AppController.Companion.getAppContext();
    SharedPreferences sharedPreferences = null;
    if (applicationContext != null) {
      sharedPreferences = applicationContext.getSharedPreferences(
          preferenceName,
          Context.MODE_PRIVATE
      );
    }

    if (sharedPreferences != null) {
      return sharedPreferences.getBoolean(preferenceKey, true);
    } else
      return false;
  }
}

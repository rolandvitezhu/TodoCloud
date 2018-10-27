package com.rolandvitezhu.todocloud.helper;

import android.content.Context;
import android.content.SharedPreferences;

import com.rolandvitezhu.todocloud.app.AppController;

public class SessionManager {

  private static SessionManager instance;

  private SharedPreferences sharedPreferences;
  private SharedPreferences.Editor editor;
  private static final String PREFERENCE_NAME = "Login";
  private static final String KEY_IS_LOGGED_IN = "isLoggedIn";

  public static synchronized SessionManager getInstance() {
    if (instance == null) instance = new SessionManager();
    return instance;
  }

  private SessionManager() {
    Context applicationContext = AppController.getAppContext();
    sharedPreferences = applicationContext.getSharedPreferences(
        PREFERENCE_NAME,
        Context.MODE_PRIVATE
    );
    editor = sharedPreferences.edit();
  }

  public void setLogin(boolean isLoggedIn) {
    editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn);
    editor.commit();
  }

  public boolean isLoggedIn() {
    return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
  }

}

package com.example.todocloud.app;

import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.support.v7.view.ActionMode;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class AppController extends Application {

  public static final String TAG = AppController.class.getSimpleName();
  private RequestQueue requestQueue;
  private static AppController instance;
  private static boolean actionModeEnabled;
  private static ActionMode actionMode;
  private static Context applicationContext;

  @Override
  public void onCreate() {
    super.onCreate();
    instance = this;
    applicationContext = getApplicationContext();
  }

  public static synchronized AppController getInstance() {
    return instance;
  }

  public static synchronized Context getAppContext() {
    return applicationContext;
  }

  public static boolean isActionModeEnabled() {
    return actionModeEnabled;
  }

  public static void setActionModeEnabled(boolean actionModeEnabled) {
    AppController.actionModeEnabled = actionModeEnabled;
  }

  public static void setActionMode(ActionMode actionMode) {
    AppController.actionMode = actionMode;
  }

  public static boolean isActionMode() {
    return actionMode != null;
  }

  public RequestQueue getRequestQueue() {
    if (requestQueue == null) {
      requestQueue = Volley.newRequestQueue(getApplicationContext());
    }
    return requestQueue;
  }

  public <T> void addToRequestQueue(Request<T> request, String tag) {
    request.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
    getRequestQueue().add(request);
  }

  public <T> void addToRequestQueue(Request<T> request) {
    request.setTag(TAG);
    getRequestQueue().add(request);
  }

  public void cancelPendingRequest(Object tag) {
    if (requestQueue != null) {
      requestQueue.cancelAll(tag);
    }
  }

  public static void showWhiteTextSnackbar(Snackbar snackbarToShow) {
    View snackbarView = snackbarToShow.getView();
    TextView snackbarText = (TextView)
        snackbarView.findViewById(android.support.design.R.id.snackbar_text);
    snackbarText.setTextColor(Color.WHITE);
    snackbarToShow.show();
  }

}

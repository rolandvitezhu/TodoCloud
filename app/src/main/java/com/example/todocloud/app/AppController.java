package com.example.todocloud.app;

import android.app.Application;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
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

  @Override
  public void onCreate() {
    super.onCreate();
    instance = this;
  }

  public static synchronized AppController getInstance() {
    return instance;
  }

  public static boolean isActionModeEnabled() {
    return actionModeEnabled;
  }

  public static void setActionModeEnabled(boolean actionModeEnabled) {
    AppController.actionModeEnabled = actionModeEnabled;
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

  /**
   * A megadott Snackbar betűszínét fehérre állítja és megjeleníti azt.
   * @param snackbar A Snackbar, amin a műveleteket végezzük.
   */
  public static void setStyleAndShowSnackbar(Snackbar snackbar) {
    View view = snackbar.getView();
    TextView textView = (TextView)
        view.findViewById(android.support.design.R.id.snackbar_text);
    textView.setTextColor(Color.WHITE);
    snackbar.show();
  }

}

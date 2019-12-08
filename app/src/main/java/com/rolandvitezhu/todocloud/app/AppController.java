package com.rolandvitezhu.todocloud.app;

import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.rolandvitezhu.todocloud.di.component.AppComponent;
import com.rolandvitezhu.todocloud.di.component.DaggerAppComponent;
import com.rolandvitezhu.todocloud.di.module.AppModule;

import androidx.appcompat.view.ActionMode;

public class AppController extends Application {

  private static AppController instance;

  private static boolean actionModeEnabled;
  private static ActionMode actionMode;
  private static Context applicationContext;

  private static Snackbar lastShownSnackbar;

  private AppComponent appComponent;

  @Override
  public void onCreate() {
    super.onCreate();

    instance = this;
    applicationContext = getApplicationContext();

    AndroidThreeTen.init(this);

    appComponent = DaggerAppComponent.builder()
        .appModule(new AppModule(this))
        .build();
  }

  public AppComponent getAppComponent() {
    return appComponent;
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

  public static void showWhiteTextSnackbar(Snackbar snackbar) {
    TextView snackbarTextView = snackbar.getView()
        .findViewById(com.google.android.material.R.id.snackbar_text);
    TextView lastShownSnackbarTextView;

    CharSequence snackbarText = snackbarTextView.getText();
    CharSequence lastShownSnackbarText = "";

    if (lastShownSnackbar != null) {
      lastShownSnackbarTextView = lastShownSnackbar.getView()
          .findViewById(com.google.android.material.R.id.snackbar_text);
      lastShownSnackbarText = lastShownSnackbarTextView.getText();
    }

    boolean shouldShowSnackbar =
        !(lastShownSnackbar != null
            && lastShownSnackbar.isShown()
            && snackbarText.equals(lastShownSnackbarText)
        );

    if (shouldShowSnackbar) {
      snackbarTextView.setTextColor(Color.WHITE);
      snackbar.show();

      lastShownSnackbar = snackbar;
    }
  }

  /**
   * Fix unnecessary TextInputEditText animation on set text.
   * @param text The text to set.
   * @param textInputEditText TextInputEditText to set text.
   * @param relatedTextInputLayout The TextInputLayout related to the textInputEditText.
   */
  public static void setText(
      String text, TextInputEditText textInputEditText, TextInputLayout relatedTextInputLayout
  ) {
    // Disable animation before set text. The unnecessary animation won't play in this case.
    relatedTextInputLayout.setHintAnimationEnabled(false);
    textInputEditText.setText(text);

    // Enable animation, because it should work on user interaction.
    relatedTextInputLayout.setHintAnimationEnabled(true);
  }

}

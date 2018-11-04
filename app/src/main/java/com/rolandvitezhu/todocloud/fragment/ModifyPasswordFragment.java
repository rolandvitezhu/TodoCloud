package com.rolandvitezhu.todocloud.fragment;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.datastorage.DbLoader;
import com.rolandvitezhu.todocloud.datasynchronizer.UserDataSynchronizer;

public class ModifyPasswordFragment extends Fragment
    implements UserDataSynchronizer.OnModifyPasswordListener {

  private CoordinatorLayout coordinatorLayout;
  private TextView tvFormSubmissionErrors;
  private TextInputLayout tilCurrentPassword, tilNewPassword, tilConfirmPassword;
  private TextInputEditText tietCurrentPassword, tietNewPassword, tietConfirmPassword;

  private DbLoader dbLoader;
  private IModifyPasswordFragment listener;
  private UserDataSynchronizer userDataSynchronizer;
  private Button btnChangePassword;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (IModifyPasswordFragment) context;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    dbLoader = new DbLoader();
    userDataSynchronizer = new UserDataSynchronizer(dbLoader);
    userDataSynchronizer.setOnModifyPasswordListener(this);
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState
  ) {
    View view = inflater.inflate(R.layout.fragment_modifypassword, container, false);

    coordinatorLayout = view.findViewById(R.id.coordinatorlayout_modifypassword);
    tvFormSubmissionErrors = view.findViewById(
        R.id.textview_modifypassword_formsubmissionerrors
    );
    tilCurrentPassword = view.findViewById(R.id.textinputlayout_modifypassword_currentpassword);
    tilNewPassword = view.findViewById(R.id.textinputlayout_modifypassword_newpassword);
    tilConfirmPassword = view.findViewById(R.id.textinputlayout_modifypassword_confirmpassword);
    tietCurrentPassword = view.findViewById(R.id.textinputedittext_modifypassword_currentpassword);
    tietNewPassword = view.findViewById(R.id.textinputedittext_modifypassword_newpassword);
    tietConfirmPassword = view.findViewById(R.id.textinputedittext_modifypassword_confirmpassword);
    btnChangePassword = view.findViewById(R.id.button_changepassword);

    applyTextChangedEvents();
    applyEditorActionEvents();
    applyClickEvents();

    return view;
  }

  private void applyClickEvents() {
    btnChangePassword.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        hideSoftInput();
        handleChangePassword();
      }

    });
  }

  private void applyTextChangedEvents() {
    tietCurrentPassword.addTextChangedListener(new MyTextWatcher(tietCurrentPassword));
    tietNewPassword.addTextChangedListener(new MyTextWatcher(tietNewPassword));
    tietConfirmPassword.addTextChangedListener(new MyTextWatcher(tietConfirmPassword));
  }

  private void applyEditorActionEvents() {
    tietConfirmPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {

      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        boolean pressDone = actionId == EditorInfo.IME_ACTION_DONE;
        boolean pressEnter = false;
        if (event != null) {
          int keyCode = event.getKeyCode();
          pressEnter = keyCode == KeyEvent.KEYCODE_ENTER;
        }

        if (pressEnter || pressDone) {
          btnChangePassword.performClick();
          return true;
        }
        return false;
      }

    });
  }

  private void hideSoftInput() {
    FragmentActivity activity = getActivity();
    InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(
        Context.INPUT_METHOD_SERVICE
    );
    View currentlyFocusedView = activity.getCurrentFocus();
    if (currentlyFocusedView != null) {
      IBinder windowToken = currentlyFocusedView.getWindowToken();
      inputMethodManager.hideSoftInputFromWindow(
          windowToken,
          0
      );
    }
  }

  private void handleChangePassword() {
    boolean areFieldsValid = validateCurrentPassword()
        & validateNewPassword()
        & validateConfirmPassword();
    if (areFieldsValid) {
      String currentPassword = tietCurrentPassword.getText().toString().trim();
      String newPassword = tietNewPassword.getText().toString().trim();
      userDataSynchronizer.modifyPassword(currentPassword, newPassword);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    listener.onSetActionBarTitle(getString(R.string.all_change_password));
    applyOrientationPortrait();
  }

  private void applyOrientationPortrait() {
    if (getActivity() != null)
      getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
  }

  @Override
  public void onPause() {
    super.onPause();
    applyOrientationFullSensor();
  }

  private void applyOrientationFullSensor() {
    if (getActivity() != null)
      getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
  }

  private boolean validateCurrentPassword() {
    String givenName = tietCurrentPassword.getText().toString().trim();
    if (givenName.isEmpty()) {
      tilCurrentPassword.setError(getString(R.string.modifypassword_currentpassworderrorlabel));
      return false;
    } else {
      tilCurrentPassword.setErrorEnabled(false);
      return true;
    }
  }

  private boolean validateNewPassword() {
    String givenPassword = tietNewPassword.getText().toString().trim();
    boolean isGivenPasswordValid = !givenPassword.isEmpty() && isValidPassword(givenPassword);
    if (!isGivenPasswordValid) {
      tilNewPassword.setError(getString(R.string.registeruser_enterproperpasswordhint));
      return false;
    } else {
      tilNewPassword.setErrorEnabled(false);
      return true;
    }
  }

  private boolean validateConfirmPassword() {
    String givenPassword = tietNewPassword.getText().toString().trim();
    String givenConfirmPassword = tietConfirmPassword.getText().toString().trim();
    boolean isGivenConfirmPasswordValid = !givenConfirmPassword.isEmpty()
        && givenPassword.equals(givenConfirmPassword);
    if (!isGivenConfirmPasswordValid) {
      tilConfirmPassword.setError(getString(R.string.registeruser_confirmpassworderrorlabel));
      return false;
    } else {
      tilConfirmPassword.setErrorEnabled(false);
      return true;
    }
  }

  /**
   * Valid password should contain at least a lowercase letter, an uppercase letter, a number,
   * it should not contain whitespace character and it should be at least 8 characters long.
   */
  private boolean isValidPassword(String password) {
    String passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,}$";
    return password.matches(passwordRegex);
  }

  @Override
  public void onFinishModifyPassword() {
    hideFormSubmissionErrors();
    listener.onFinishModifyPassword();
  }

  @Override
  public void onSyncError(String errorMessage) {
    showErrorMessage(errorMessage);
  }

  private void showErrorMessage(String errorMessage) {
    if (errorMessage.contains("failed to connect")) {
      hideFormSubmissionErrors();
      showFailedToConnectError();
    } else if (errorMessage.contains("Your current password is incorrect.")) {
      showIncorrectCurrentPasswordError();
    } else {
      hideFormSubmissionErrors();
      showAnErrorOccurredError();
    }
  }

  private void hideFormSubmissionErrors() {
    tvFormSubmissionErrors.setText("");
    tvFormSubmissionErrors.setVisibility(View.GONE);
  }

  private void showFailedToConnectError() {
    Snackbar snackbar = Snackbar.make(
        coordinatorLayout,
        R.string.all_failedtoconnect,
        Snackbar.LENGTH_LONG
    );
    AppController.showWhiteTextSnackbar(snackbar);
  }

  private void showIncorrectCurrentPasswordError() {
    tvFormSubmissionErrors.setText(R.string.modifypassword_incorrectcurrentpassword);
    tvFormSubmissionErrors.setVisibility(View.VISIBLE);
  }

  private void showAnErrorOccurredError() {
    Snackbar snackbar = Snackbar.make(
        coordinatorLayout,
        R.string.all_anerroroccurred,
        Snackbar.LENGTH_LONG
    );
    AppController.showWhiteTextSnackbar(snackbar);
  }

  private class MyTextWatcher implements TextWatcher {

    private View view;

    private MyTextWatcher(View view) {
      this.view = view;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
      switch (view.getId()) {
        case R.id.textinputedittext_modifypassword_currentpassword:
          validateCurrentPassword();
          break;
        case R.id.textinputedittext_modifypassword_newpassword:
          validateNewPassword();
          break;
        case R.id.textinputedittext_modifypassword_confirmpassword:
          break;
      }
    }

  }

  public interface IModifyPasswordFragment {
    void onFinishModifyPassword();
    void onSetActionBarTitle(String title);
  }

}

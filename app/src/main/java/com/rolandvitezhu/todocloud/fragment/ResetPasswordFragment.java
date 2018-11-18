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
import android.util.Patterns;
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

public class ResetPasswordFragment extends Fragment
    implements UserDataSynchronizer.OnResetPasswordListener {

  private CoordinatorLayout coordinatorLayout;
  private TextView tvFormSubmissionErrors;
  private TextInputLayout tilEmail;
  private TextInputEditText tietEmail;

  private DbLoader dbLoader;
  private IResetPasswordFragment listener;
  private UserDataSynchronizer userDataSynchronizer;
  private Button btnSubmit;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (IResetPasswordFragment) context;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    dbLoader = new DbLoader();
    userDataSynchronizer = new UserDataSynchronizer(dbLoader);
    userDataSynchronizer.setOnResetPasswordListener(this);
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState
  ) {
    View view = inflater.inflate(R.layout.fragment_resetpassword, container, false);

    coordinatorLayout = view.findViewById(R.id.coordinatorlayout_resetpassword);
    tvFormSubmissionErrors = view.findViewById(
        R.id.textview_resetpassword_formsubmissionerrors
    );
    tilEmail = view.findViewById(R.id.textinputlayout_resetpassword_email);
    tietEmail = view.findViewById(R.id.textinputedittext_resetpassword_email);
    btnSubmit = view.findViewById(R.id.button_resetpassword);

    applyTextChangedEvents();
    applyEditorActionEvents();
    applyClickEvents();

    return view;
  }

  private void applyClickEvents() {
    btnSubmit.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        hideSoftInput();
        handleResetPassword();
      }

    });
  }

  private void applyTextChangedEvents() {
    tietEmail.addTextChangedListener(new MyTextWatcher(tietEmail));
  }

  private void applyEditorActionEvents() {
    tietEmail.setOnEditorActionListener(new TextView.OnEditorActionListener() {

      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        boolean pressDone = actionId == EditorInfo.IME_ACTION_DONE;
        boolean pressEnter = false;
        if (event != null) {
          int keyCode = event.getKeyCode();
          pressEnter = keyCode == KeyEvent.KEYCODE_ENTER;
        }

        if (pressEnter || pressDone) {
          btnSubmit.performClick();
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

  private void handleResetPassword() {
    if (validateEmail()) {
      String email = tietEmail.getText().toString().trim();
      userDataSynchronizer.resetPassword(email);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    listener.onSetActionBarTitle(getString(R.string.all_reset_password));
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

  private boolean validateEmail() {
    String givenEmail = tietEmail.getText().toString().trim();
    boolean isGivenEmailValid = !givenEmail.isEmpty() && isValidEmail(givenEmail);
    if (!isGivenEmailValid) {
      tilEmail.setError(getString(R.string.registeruser_entervalidemailhint));
      return false;
    } else {
      tilEmail.setErrorEnabled(false);
      return true;
    }
  }

  private boolean isValidEmail(String email) {
    return !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches();
  }

  @Override
  public void onFinishResetPassword() {
    hideFormSubmissionErrors();
    listener.onFinishResetPassword();
  }

  @Override
  public void onSyncError(String errorMessage) {
    showErrorMessage(errorMessage);
  }

  private void showErrorMessage(String errorMessage) {
    if (errorMessage.contains("failed to connect")) {
      hideFormSubmissionErrors();
      showFailedToConnectError();
    } else if (errorMessage.contains("Failed to reset password. Please try again!")) {
      showFailedToResetPasswordError();
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

  private void showFailedToResetPasswordError() {
    tvFormSubmissionErrors.setText(R.string.modifypassword_failedtoresetpassword);
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
        case R.id.textinputedittext_resetpassword_email:
          validateEmail();
          break;
      }
    }

  }

  public interface IResetPasswordFragment {
    void onFinishResetPassword();
    void onSetActionBarTitle(String title);
  }

}

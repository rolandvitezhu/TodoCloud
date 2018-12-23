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

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class ResetPasswordFragment extends Fragment
    implements UserDataSynchronizer.OnResetPasswordListener {

  @Inject
  DbLoader dbLoader;
  @Inject
  UserDataSynchronizer userDataSynchronizer;

  @BindView(R.id.coordinatorlayout_resetpassword)
  CoordinatorLayout coordinatorLayout;
  @BindView(R.id.textview_resetpassword_formsubmissionerrors)
  TextView tvFormSubmissionErrors;

  @BindView(R.id.textinputlayout_resetpassword_email)
  TextInputLayout tilEmail;
  @BindView(R.id.textinputedittext_resetpassword_email)
  TextInputEditText tietEmail;

  @BindView(R.id.button_resetpassword)
  Button btnSubmit;

  private IResetPasswordFragment listener;

  Unbinder unbinder;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (IResetPasswordFragment) context;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ((AppController) getActivity().getApplication()).getAppComponent().inject(this);

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
    unbinder = ButterKnife.bind(this, view);

    applyTextChangedEvents();
    applyEditorActionEvents();

    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    listener.onSetActionBarTitle(getString(R.string.all_reset_password));
    applyOrientationPortrait();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
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

  @OnClick(R.id.button_resetpassword)
  public void onBtnSubmitClick(View view) {
    hideSoftInput();
    handleResetPassword();
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

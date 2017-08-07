package com.example.todocloud.fragment;

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

import com.example.todocloud.R;
import com.example.todocloud.app.AppController;
import com.example.todocloud.datastorage.DbLoader;
import com.example.todocloud.datasynchronizer.UserDataSynchronizer;
import com.example.todocloud.helper.SessionManager;

public class LoginUserFragment extends Fragment
    implements UserDataSynchronizer.OnLoginUserListener {

  private CoordinatorLayout coordinatorLayout;
  private TextView formSubmissionErrors;
  private TextInputLayout tilEmail, tilPassword;
  private TextInputEditText tietEmail, tietPassword;

  private SessionManager sessionManager;
  private DbLoader dbLoader;
  private ILoginUserFragment listener;
  private UserDataSynchronizer userDataSynchronizer;
  private Button btnLogin;
  private Button btnLinkToRegister;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (ILoginUserFragment) context;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    sessionManager = SessionManager.getInstance();

    if (sessionManager.isLoggedIn()) {
      listener.onFinishLoginUser();
    } else {
      dbLoader = new DbLoader();
      userDataSynchronizer = new UserDataSynchronizer(dbLoader);
      userDataSynchronizer.setOnLoginUserListener(this);
    }
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_loginuser, container, false);
    coordinatorLayout = (CoordinatorLayout) view.findViewById(R.id.coordinatorlayout_loginuser);
    formSubmissionErrors = (TextView) view.findViewById(
        R.id.textview_loginuser_formsubmissionerrors
    );
    tilEmail = (TextInputLayout) view.findViewById(R.id.textinputlayout_loginuser_email);
    tilPassword = (TextInputLayout) view.findViewById(R.id.textinputlayout_loginuser_password);
    tietEmail = (TextInputEditText) view.findViewById(R.id.textinputedittext_loginuser_email);
    tietPassword = (TextInputEditText) view.findViewById(
        R.id.textinputedittext_loginuser_password
    );
    btnLogin = (Button) view.findViewById(R.id.button_loginuser_login);
    btnLinkToRegister = (Button) view.findViewById(R.id.button_loginuser_linktoregister);

    applyTextChangedEvents();
    applyEditorActionEvents();
    applyClickEvents();
    preventButtonTextCapitalization();

    return view;
  }

  private void applyClickEvents() {
    btnLogin.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        hideSoftInput();
        handleLoginUser();
      }

    });
    btnLinkToRegister.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        listener.onClickLinkToRegisterUser();
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

  private void handleLoginUser() {
    boolean areFieldsValid = validateEmail() & validatePassword();
    if (areFieldsValid) {
      dbLoader.reCreateDb();
      String email = tietEmail.getText().toString().trim();
      String password = tietPassword.getText().toString().trim();
      userDataSynchronizer.loginUser(email, password);
    }
  }

  private void preventButtonTextCapitalization() {
    btnLinkToRegister.setTransformationMethod(null);
  }

  private void applyEditorActionEvents() {
    tietPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {

      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        boolean pressDone = actionId == EditorInfo.IME_ACTION_DONE;
        boolean pressEnter = false;
        if (event != null) {
          int keyCode = event.getKeyCode();
          pressEnter = keyCode == KeyEvent.KEYCODE_ENTER;
        }

        if (pressEnter || pressDone) {
          btnLogin.performClick();
          return true;
        }
        return false;
      }

    });
  }

  private void applyTextChangedEvents() {
    tietEmail.addTextChangedListener(new MyTextWatcher(tietEmail));
    tietPassword.addTextChangedListener(new MyTextWatcher(tietPassword));
  }

  @Override
  public void onResume() {
    super.onResume();
    if (getActivity() != null)
    listener.onSetActionBarTitle(getString(R.string.all_login));
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
    if (givenEmail.isEmpty()) {
      tilEmail.setError(getString(R.string.registeruser_enteremailhint));
      return false;
    } else {
      tilEmail.setErrorEnabled(false);
      return true;
    }
  }

  private boolean validatePassword() {
    String givenPassword = tietPassword.getText().toString().trim();
    if (givenPassword.isEmpty()) {
      tilPassword.setError(getString(R.string.registeruser_enterpasswordhint));
      return false;
    } else {
      tilPassword.setErrorEnabled(false);
      return true;
    }
  }

  @Override
  public void onFinishLoginUser() {
    hideFormSubmissionErrors();
    listener.onFinishLoginUser();
  }

  @Override
  public void onSyncError(String errorMessage) {
    showErrorMessage(errorMessage);
  }

  private void showErrorMessage(String errorMessage) {
    if (errorMessage.contains("failed to connect")) {
      hideFormSubmissionErrors();
      showFailedToConnectError();
    } else if (errorMessage.contains("Login failed. Incorrect credentials")) {
      showIncorrectCredentialsError();
    } else {
      hideFormSubmissionErrors();
      showAnErrorOccurredError();
    }
  }

  private void hideFormSubmissionErrors() {
    formSubmissionErrors.setText("");
    formSubmissionErrors.setVisibility(View.GONE);
  }

  private void showFailedToConnectError() {
    Snackbar snackbar = Snackbar.make(
        coordinatorLayout,
        R.string.all_failedtoconnect,
        Snackbar.LENGTH_LONG
    );
    AppController.showWhiteTextSnackbar(snackbar);
  }

  private void showIncorrectCredentialsError() {
    formSubmissionErrors.setText(R.string.loginuser_error);
    formSubmissionErrors.setVisibility(View.VISIBLE);
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
        case R.id.textinputedittext_loginuser_email:
          validateEmail();
          break;
        case R.id.textinputedittext_loginuser_password:
          validatePassword();
          break;
      }
    }

  }

  public interface ILoginUserFragment {
    void onClickLinkToRegisterUser();
    void onFinishLoginUser();
    void onSetActionBarTitle(String title);
  }

}

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
import android.util.Patterns;
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
import com.example.todocloud.data.User;
import com.example.todocloud.datastorage.DbLoader;
import com.example.todocloud.datasynchronizer.UserDataSynchronizer;
import com.example.todocloud.helper.OnlineIdGenerator;
import com.example.todocloud.helper.SessionManager;

public class RegisterUserFragment extends Fragment
    implements UserDataSynchronizer.OnRegisterUserListener {

  private CoordinatorLayout coordinatorLayout;
  private TextView formSubmissionErrors;
  private TextInputLayout tilName, tilEmail, tilPassword, tilConfirmPassword;
  private TextInputEditText tietName, tietEmail, tietPassword, tietConfirmPassword;

  private SessionManager sessionManager;
  private DbLoader dbLoader;
  private IRegisterUserFragment listener;
  private UserDataSynchronizer userDataSynchronizer;
  private Button btnRegister;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (IRegisterUserFragment) context;
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
      userDataSynchronizer.setOnRegisterUserListener(this);
    }
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState
  ) {
    View view = inflater.inflate(R.layout.register_user, container, false);

    coordinatorLayout = (CoordinatorLayout) view.findViewById(R.id.coordinatorLayout);
    formSubmissionErrors = (TextView) view.findViewById(R.id.tvFormSubmissionErrors);
    tilName = (TextInputLayout) view.findViewById(R.id.tilName);
    tilEmail = (TextInputLayout) view.findViewById(R.id.tilEmail);
    tilPassword = (TextInputLayout) view.findViewById(R.id.tilPassword);
    tilConfirmPassword = (TextInputLayout) view.findViewById(R.id.tilConfirmPassword);
    tietName = (TextInputEditText) view.findViewById(R.id.tietName);
    tietEmail = (TextInputEditText) view.findViewById(R.id.tietEmail);
    tietPassword = (TextInputEditText) view.findViewById(R.id.tietPassword);
    tietConfirmPassword = (TextInputEditText) view.findViewById(R.id.tietConfirmPassword);
    btnRegister = (Button) view.findViewById(R.id.btnRegister);

    applyTextChangedEvents();
    applyEditorActionEvents();
    applyClickEvents();

    return view;
  }

  private void applyClickEvents() {
    btnRegister.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        hideSoftInput();
        handleRegisterUser();
      }

    });
  }

  private void applyTextChangedEvents() {
    tietName.addTextChangedListener(new MyTextWatcher(tietName));
    tietEmail.addTextChangedListener(new MyTextWatcher(tietEmail));
    tietPassword.addTextChangedListener(new MyTextWatcher(tietPassword));
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
          btnRegister.performClick();
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

  private void handleRegisterUser() {
    boolean areFieldsValid = validateName()
        & validateEmail()
        & validatePassword()
        & validateConfirmPassword();
    if (areFieldsValid) {
      dbLoader.reCreateDb();
      User user = new User();
      long _id = dbLoader.createUser(user);
      String user_online_id = OnlineIdGenerator.generateUserOnlineId(_id);
      String name = tietName.getText().toString().trim();
      String email = tietEmail.getText().toString().trim();
      String password = tietPassword.getText().toString().trim();
      userDataSynchronizer.registerUser(user_online_id, name, email, password, _id);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    listener.onSetActionBarTitle(getString(R.string.register));
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

  private boolean validateName() {
    String givenName = tietName.getText().toString().trim();
    if (givenName.isEmpty()) {
      tilName.setError(getString(R.string.enter_name));
      return false;
    } else {
      tilName.setErrorEnabled(false);
      return true;
    }
  }

  private boolean validateEmail() {
    String givenEmail = tietEmail.getText().toString().trim();
    boolean isGivenEmailValid = !givenEmail.isEmpty() && isValidEmail(givenEmail);
    if (!isGivenEmailValid) {
      tilEmail.setError(getString(R.string.enter_valid_email));
      return false;
    } else {
      tilEmail.setErrorEnabled(false);
      return true;
    }
  }

  private boolean validatePassword() {
    String givenPassword = tietPassword.getText().toString().trim();
    boolean isGivenPasswordValid = !givenPassword.isEmpty() && isValidPassword(givenPassword);
    if (!isGivenPasswordValid) {
      tilPassword.setError(getString(R.string.enter_proper_password));
      return false;
    } else {
      tilPassword.setErrorEnabled(false);
      return true;
    }
  }

  private boolean validateConfirmPassword() {
    String givenPassword = tietPassword.getText().toString().trim();
    String givenConfirmPassword = tietConfirmPassword.getText().toString().trim();
    boolean isGivenConfirmPasswordValid = !givenConfirmPassword.isEmpty()
        && givenPassword.equals(givenConfirmPassword);
    if (!isGivenConfirmPasswordValid) {
      tilConfirmPassword.setError(getString(R.string.passwords_does_not_match));
      return false;
    } else {
      tilConfirmPassword.setErrorEnabled(false);
      return true;
    }
  }

  private boolean isValidEmail(String email) {
    return !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches();
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
  public void onFinishRegisterUser() {
    hideFormSubmissionErrors();
    listener.onFinishRegisterUser();
  }

  @Override
  public void onSyncError(String errorMessage) {
    showErrorMessage(errorMessage);
  }

  private void showErrorMessage(String errorMessage) {
    if (errorMessage.contains("failed to connect")) {
      hideFormSubmissionErrors();
      showFailedToConnectError();
    } else if (errorMessage.contains("Sorry, this email already existed")) {
      showThisEmailAlreadyExistedError();
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
        R.string.failed_to_connect,
        Snackbar.LENGTH_LONG
    );
    AppController.showWhiteTextSnackbar(snackbar);
  }

  private void showThisEmailAlreadyExistedError() {
    formSubmissionErrors.setText(R.string.this_email_already_existed);
    formSubmissionErrors.setVisibility(View.VISIBLE);
  }

  private void showAnErrorOccurredError() {
    Snackbar snackbar = Snackbar.make(
        coordinatorLayout,
        R.string.an_error_occurred,
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
        case R.id.tietName:
          validateName();
          break;
        case R.id.tietEmail:
          validateEmail();
          break;
        case R.id.tietPassword:
          validatePassword();
          break;
        case R.id.tietConfirmPassword:
          validateConfirmPassword();
          break;
      }
    }

  }

  public interface IRegisterUserFragment {
    void onFinishRegisterUser();
    void onFinishLoginUser();
    void onSetActionBarTitle(String title);
  }

}

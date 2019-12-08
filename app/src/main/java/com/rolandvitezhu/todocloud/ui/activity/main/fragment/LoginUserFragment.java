package com.rolandvitezhu.todocloud.ui.activity.main.fragment;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.data.User;
import com.rolandvitezhu.todocloud.datastorage.DbLoader;
import com.rolandvitezhu.todocloud.helper.SessionManager;
import com.rolandvitezhu.todocloud.network.ApiService;
import com.rolandvitezhu.todocloud.network.api.user.dto.LoginUserRequest;
import com.rolandvitezhu.todocloud.network.api.user.dto.LoginUserResponse;
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;

public class LoginUserFragment extends Fragment {

  private final String TAG = getClass().getSimpleName();

  private ApiService apiService;

  private CompositeDisposable disposable = new CompositeDisposable();

  @Inject
  SessionManager sessionManager;
  @Inject
  DbLoader dbLoader;
  @Inject
  Retrofit retrofit;

  @BindView(R.id.coordinatorlayout_loginuser)
  CoordinatorLayout coordinatorLayout;
  @BindView(R.id.textview_loginuser_formsubmissionerrors)
  TextView tvFormSubmissionErrors;

  @BindView(R.id.textinputlayout_loginuser_email)
  TextInputLayout tilEmail;
  @BindView(R.id.textinputlayout_loginuser_password)
  TextInputLayout tilPassword;

  @BindView(R.id.textinputedittext_loginuser_email)
  TextInputEditText tietEmail;
  @BindView(R.id.textinputedittext_loginuser_password)
  TextInputEditText tietPassword;

  @BindView(R.id.button_loginuser_login)
  Button btnLogin;
  @BindView(R.id.button_loginuser_linktoregister)
  Button btnLinkToRegister;
  @BindView(R.id.button_loginuser_linktoresetpassword)
  Button btnLinkToResetPassword;

  Unbinder unbinder;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ((AppController) getActivity().getApplication()).getAppComponent().inject(this);

    apiService = retrofit.create(ApiService.class);

    if (sessionManager.isLoggedIn()) {
      ((MainActivity)getActivity()).onFinishLoginUser();
    }
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_loginuser, container, false);
    unbinder = ButterKnife.bind(this, view);

    applyTextChangedEvents();
    applyEditorActionEvents();
    preventButtonTextCapitalization();

    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    try {
      ((MainActivity)getActivity()).onSetActionBarTitle(getString(R.string.all_login));
    } catch (NullPointerException e) {
      // Activity doesn't exists already.
    }
    applyOrientationPortrait();
  }

  @Override
  public void onPause() {
    super.onPause();
    applyOrientationFullSensor();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
    disposable.clear();
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

      LoginUserRequest loginUserRequest = new LoginUserRequest();

      loginUserRequest.setEmail(email);
      loginUserRequest.setPassword(password);

      disposable.add(
          apiService
          .loginUser(loginUserRequest)
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribeWith(createLoginUserDisposableSingleObserver())
      );
    }
  }

  private DisposableSingleObserver<LoginUserResponse>
  createLoginUserDisposableSingleObserver() {
    return new DisposableSingleObserver<LoginUserResponse>() {

      @Override
      public void onSuccess(LoginUserResponse loginUserResponse) {
        Log.d(TAG, "Login Response: " + loginUserResponse);

        if (loginUserResponse != null && loginUserResponse.error.equals("false")) {
          handleLogin(loginUserResponse);
          onFinishLoginUser();
        } else if (loginUserResponse != null) {
          String message = loginUserResponse.getMessage();

          if (message == null) message = "Unknown error";
          onSyncError(message);
        }
      }

      @Override
      public void onError(Throwable throwable) {
        Log.d(TAG, "Login Response - onFailure: " + throwable.toString());
      }
    };
  }

  private void handleLogin(LoginUserResponse loginUserResponse) {
    User user = new User(loginUserResponse);

    dbLoader.createUser(user);
    sessionManager.setLogin(true);
  }

  private void preventButtonTextCapitalization() {
    btnLinkToRegister.setTransformationMethod(null);
    btnLinkToResetPassword.setTransformationMethod(null);
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

  private void applyOrientationPortrait() {
    if (getActivity() != null)
      getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
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

  public void onFinishLoginUser() {
    hideFormSubmissionErrors();
    ((MainActivity)getActivity()).onFinishLoginUser();
  }

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
    try {
      tvFormSubmissionErrors.setText("");
      tvFormSubmissionErrors.setVisibility(View.GONE);
    } catch (NullPointerException e) {
      // TextView doesn't exists already.
    }
  }

  private void showFailedToConnectError() {
    try {
      Snackbar snackbar = Snackbar.make(
          coordinatorLayout,
          R.string.all_failedtoconnect,
          Snackbar.LENGTH_LONG
      );
      AppController.showWhiteTextSnackbar(snackbar);
    } catch (NullPointerException e) {
      // Snackbar or coordinatorLayout doesn't exists already.
    }
  }

  private void showIncorrectCredentialsError() {
    try {
      tvFormSubmissionErrors.setText(R.string.loginuser_error);
      tvFormSubmissionErrors.setVisibility(View.VISIBLE);
    } catch (NullPointerException e) {
      // TextView doesn't exists already.
    }
  }

  private void showAnErrorOccurredError() {
    try {
      Snackbar snackbar = Snackbar.make(
          coordinatorLayout,
          R.string.all_anerroroccurred,
          Snackbar.LENGTH_LONG
      );
      AppController.showWhiteTextSnackbar(snackbar);
    } catch (NullPointerException e) {
      // Snackbar or coordinatorLayout doesn't exists already.
    }
  }

  @OnClick(R.id.button_loginuser_login)
  public void onBtnLoginClick(View view) {
    hideSoftInput();
    handleLoginUser();
  }

  @OnClick(R.id.button_loginuser_linktoregister)
  public void onBtnLinkToRegisterClick(View view) {
    ((MainActivity)getActivity()).onClickLinkToRegisterUser();
  }

  @OnClick(R.id.button_loginuser_linktoresetpassword)
  public void onBtnLinkToResetPasswordClick(View view) {
    ((MainActivity)getActivity()).onClickLinkToResetPassword();
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

}

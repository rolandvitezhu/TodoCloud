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
import android.util.Log;
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
import com.rolandvitezhu.todocloud.data.User;
import com.rolandvitezhu.todocloud.datastorage.DbLoader;
import com.rolandvitezhu.todocloud.helper.InstallationIdHelper;
import com.rolandvitezhu.todocloud.helper.OnlineIdGenerator;
import com.rolandvitezhu.todocloud.helper.SessionManager;
import com.rolandvitezhu.todocloud.network.api.user.dto.RegisterUserRequest;
import com.rolandvitezhu.todocloud.network.api.user.dto.RegisterUserResponse;
import com.rolandvitezhu.todocloud.network.api.user.service.RegisterUserService;
import com.rolandvitezhu.todocloud.network.helper.RetrofitResponseHelper;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class RegisterUserFragment extends Fragment {

  private final String TAG = getClass().getSimpleName();

  @Inject
  SessionManager sessionManager;
  @Inject
  DbLoader dbLoader;
  @Inject
  Retrofit retrofit;

  @BindView(R.id.coordinatorlayout_registeruser)
  CoordinatorLayout coordinatorLayout;
  @BindView(R.id.textview_registeruser_formsubmissionerrors)
  TextView tvFormSubmissionErrors;

  @BindView(R.id.textinputlayout_registeruser_name)
  TextInputLayout tilName;
  @BindView(R.id.textinputlayout_registeruser_email)
  TextInputLayout tilEmail;
  @BindView(R.id.textinputlayout_registeruser_password)
  TextInputLayout tilPassword;
  @BindView(R.id.textinputlayout_registeruser_confirmpassword)
  TextInputLayout tilConfirmPassword;

  @BindView(R.id.textinputedittext_registeruser_name)
  TextInputEditText tietName;
  @BindView(R.id.textinputedittext_registeruser_email)
  TextInputEditText tietEmail;
  @BindView(R.id.textinputedittext_registeruser_password)
  TextInputEditText tietPassword;
  @BindView(R.id.textinputedittext_registeruser_confirmpassword)
  TextInputEditText tietConfirmPassword;

  @BindView(R.id.button_registeruser)
  Button btnRegister;

  private IRegisterUserFragment listener;

  Unbinder unbinder;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (IRegisterUserFragment) context;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ((AppController) getActivity().getApplication()).getAppComponent().inject(this);

    if (sessionManager.isLoggedIn()) {
      listener.onFinishLoginUser();
    }
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState
  ) {
    View view = inflater.inflate(R.layout.fragment_registeruser, container, false);
    unbinder = ButterKnife.bind(this, view);

    applyTextChangedEvents();
    applyEditorActionEvents();

    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    listener.onSetActionBarTitle(getString(R.string.all_register));
    applyOrientationPortrait();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
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

      RegisterUserService registerUserService = retrofit.create(RegisterUserService.class);

      RegisterUserRequest registerUserRequest = new RegisterUserRequest();

      registerUserRequest.setUserOnlineId(user_online_id);
      registerUserRequest.setName(name);
      registerUserRequest.setEmail(email);
      registerUserRequest.setPassword(password);

      Call<RegisterUserResponse> call = registerUserService.registerUser(registerUserRequest);

      Callback<RegisterUserResponse> registerUserCallback = new Callback<RegisterUserResponse>() {
        @Override
        public void onResponse(Call<RegisterUserResponse> call, Response<RegisterUserResponse> response) {
          Log.d(TAG, "Register Response: " + RetrofitResponseHelper.ResponseToJson(response));

          if (RetrofitResponseHelper.IsNoError(response)) {
            onFinishRegisterUser();
          } else if (response.body() != null) {
            String message = response.body().getMessage();
            if (message == null) message = "Unknown error";
            if (message.contains("Oops! An error occurred while registering")) {
              handleError(_id, registerUserRequest, registerUserService, this);
            } else {
              onSyncError(message);
            }
          }
        }

        @Override
        public void onFailure(Call<RegisterUserResponse> call, Throwable t) {
          Log.d(TAG, "Register Response - onFailure: " + t.toString());
        }
      };

      call.enqueue(registerUserCallback);
    }
  }

    /**
     * Generally the cause of error is, that the userOnlineId generated by the client is
     * already registered in the remote database. In this case, it generate a different
     * userOnlineId, and send the registration request again.
     */
    private void handleError(
        long _id,
        RegisterUserRequest registerUserRequest,
        RegisterUserService registerUserService,
        Callback<RegisterUserResponse> registerUserCallback
    ) {
      InstallationIdHelper.getNewInstallationId();
      String new_user_online_id = OnlineIdGenerator.generateUserOnlineId(_id);

      registerUserRequest.setUserOnlineId(new_user_online_id);

      Call<RegisterUserResponse> call = registerUserService.registerUser(registerUserRequest);

      call.enqueue(registerUserCallback);
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
      tilName.setError(getString(R.string.registeruser_nameerrorlabel));
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
      tilEmail.setError(getString(R.string.registeruser_entervalidemailhint));
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
      tilPassword.setError(getString(R.string.registeruser_enterproperpasswordhint));
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
      tilConfirmPassword.setError(getString(R.string.registeruser_confirmpassworderrorlabel));
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

  public void onFinishRegisterUser() {
    hideFormSubmissionErrors();
    listener.onFinishRegisterUser();
  }

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

  private void showThisEmailAlreadyExistedError() {
    try {
      tvFormSubmissionErrors.setText(R.string.registeruser_thisemailalreadyexisted);
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

  @OnClick(R.id.button_registeruser)
  public void onBtnRegisterClick(View view) {
    hideSoftInput();
    handleRegisterUser();
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
        case R.id.textinputedittext_registeruser_name:
          validateName();
          break;
        case R.id.textinputedittext_registeruser_email:
          validateEmail();
          break;
        case R.id.textinputedittext_registeruser_password:
          validatePassword();
          break;
        case R.id.textinputedittext_registeruser_confirmpassword:
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

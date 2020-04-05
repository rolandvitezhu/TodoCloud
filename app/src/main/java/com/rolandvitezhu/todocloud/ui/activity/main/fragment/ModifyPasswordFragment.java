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
import com.rolandvitezhu.todocloud.datastorage.DbLoader;
import com.rolandvitezhu.todocloud.network.ApiService;
import com.rolandvitezhu.todocloud.network.api.user.dto.ModifyPasswordRequest;
import com.rolandvitezhu.todocloud.network.api.user.dto.ModifyPasswordResponse;
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity;

import java.util.Objects;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
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

public class ModifyPasswordFragment extends Fragment {

  private final String TAG = getClass().getSimpleName();

  private ApiService apiService;

  private CompositeDisposable disposable = new CompositeDisposable();

  @Inject
  DbLoader dbLoader;
  @Inject
  Retrofit retrofit;

  @BindView(R.id.constraintlayout_modifypassword)
  ConstraintLayout constraintLayout;

  @BindView(R.id.textview_modifypassword_formsubmissionerrors)
  TextView tvFormSubmissionErrors;

  @BindView(R.id.textinputlayout_modifypassword_currentpassword)
  TextInputLayout tilCurrentPassword;
  @BindView(R.id.textinputlayout_modifypassword_newpassword)
  TextInputLayout tilNewPassword;
  @BindView(R.id.textinputlayout_modifypassword_confirmpassword)
  TextInputLayout tilConfirmPassword;

  @BindView(R.id.textinputedittext_modifypassword_currentpassword)
  TextInputEditText tietCurrentPassword;
  @BindView(R.id.textinputedittext_modifypassword_newpassword)
  TextInputEditText tietNewPassword;
  @BindView(R.id.textinputedittext_modifypassword_confirmpassword)
  TextInputEditText tietConfirmPassword;

  @BindView(R.id.button_changepassword)
  Button btnChangePassword;

  Unbinder unbinder;

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    ((AppController) Objects.requireNonNull(getActivity()).getApplication()).getAppComponent().
        fragmentComponent().create().inject(this);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    apiService = retrofit.create(ApiService.class);
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState
  ) {
    View view = inflater.inflate(R.layout.fragment_modifypassword, container, false);
    unbinder = ButterKnife.bind(this, view);

    applyTextChangedEvents();
    applyEditorActionEvents();

    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    ((MainActivity)getActivity()).onSetActionBarTitle(getString(R.string.all_change_password));
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

      ModifyPasswordRequest modifyPasswordRequest = new ModifyPasswordRequest();

      modifyPasswordRequest.setCurrentPassword(currentPassword);
      modifyPasswordRequest.setNewPassword(newPassword);

      disposable.add(
          apiService
          .modifyPassword(modifyPasswordRequest)
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribeWith(createModifyPasswordDisposableSingleObserver())
      );
    }
  }

  private DisposableSingleObserver<ModifyPasswordResponse>
  createModifyPasswordDisposableSingleObserver() {
    return new DisposableSingleObserver<ModifyPasswordResponse>() {

      @Override
      public void onSuccess(ModifyPasswordResponse modifyPasswordResponse) {
          Log.d(TAG, "Modify Password Response: " + modifyPasswordResponse);

          if (modifyPasswordResponse != null && modifyPasswordResponse.error.equals("false")) {
            onFinishModifyPassword();
          } else if (modifyPasswordResponse != null) {
            String message = modifyPasswordResponse.getMessage();

            if (message == null) message = "Unknown error";
            onSyncError(message);
          }
      }

      @Override
      public void onError(Throwable throwable) {
        Log.d(TAG, "Modify Password Response - onFailure: " + throwable.toString());
      }
    };
  }

  private void applyOrientationPortrait() {
    if (getActivity() != null)
      getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
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

  public void onFinishModifyPassword() {
    hideFormSubmissionErrors();
    ((MainActivity)getActivity()).onFinishModifyPassword();
  }

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
          constraintLayout,
          R.string.all_failedtoconnect,
          Snackbar.LENGTH_LONG
      );
      AppController.Companion.showWhiteTextSnackbar(snackbar);
    } catch (NullPointerException e) {
      // Snackbar or constraintLayout doesn't exists already.
    }
  }

  private void showIncorrectCurrentPasswordError() {
    try {
      tvFormSubmissionErrors.setText(R.string.modifypassword_incorrectcurrentpassword);
      tvFormSubmissionErrors.setVisibility(View.VISIBLE);
    } catch (NullPointerException e) {
      // TextView doesn't exists already.
    }
  }

  private void showAnErrorOccurredError() {
    try {
      Snackbar snackbar = Snackbar.make(
          constraintLayout,
          R.string.all_anerroroccurred,
          Snackbar.LENGTH_LONG
      );
      AppController.Companion.showWhiteTextSnackbar(snackbar);
    } catch (NullPointerException e) {
      // Snackbar or constraintLayout doesn't exists already.
    }
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

  @OnClick(R.id.button_changepassword)
  public void onBtnChangePasswordClick(View view) {
    hideSoftInput();
    handleChangePassword();
  }

}

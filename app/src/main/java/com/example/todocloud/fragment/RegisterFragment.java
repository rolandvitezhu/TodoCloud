package com.example.todocloud.fragment;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
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

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.todocloud.R;
import com.example.todocloud.app.AppConfig;
import com.example.todocloud.app.AppController;
import com.example.todocloud.data.User;
import com.example.todocloud.datastorage.DbConstants;
import com.example.todocloud.datastorage.DbLoader;
import com.example.todocloud.helper.InstallationIdHelper;
import com.example.todocloud.helper.OnlineIdGenerator;
import com.example.todocloud.helper.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

public class RegisterFragment extends Fragment {

  private static final String TAG = RegisterFragment.class.getSimpleName();

  private CoordinatorLayout coordinatorLayout;
  private TextView tvFormSubmissionErrors;
  private TextInputLayout tilName, tilEmail, tilPassword, tilConfirmPassword;
  private TextInputEditText tietName, tietEmail, tietPassword, tietConfirmPassword;

  private DbLoader dbLoader;
  private IRegisterFragment listener;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (IRegisterFragment) context;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    SessionManager sessionManager = new SessionManager(getActivity());

    // A felhasználó bejelentkezett, a MainListFragment-et jelenítjük meg.
    if (sessionManager.isLoggedIn()) {
      listener.onLogin();
    }

    dbLoader = new DbLoader(getActivity());
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_register, container, false);
    coordinatorLayout = (CoordinatorLayout) view.findViewById(R.id.coordinatorLayout);
    tvFormSubmissionErrors = (TextView) view.findViewById(R.id.tvFormSubmissionErrors);
    tilName = (TextInputLayout) view.findViewById(R.id.tilName);
    tilEmail = (TextInputLayout) view.findViewById(R.id.tilEmail);
    tilPassword = (TextInputLayout) view.findViewById(R.id.tilPassword);
    tilConfirmPassword = (TextInputLayout) view.findViewById(R.id.tilConfirmPassword);
    tietName = (TextInputEditText) view.findViewById(R.id.tietName);
    tietEmail = (TextInputEditText) view.findViewById(R.id.tietEmail);
    tietPassword = (TextInputEditText) view.findViewById(R.id.tietPassword);
    tietConfirmPassword = (TextInputEditText) view.findViewById(R.id.tietConfirmPassword);
    final Button btnRegister = (Button) view.findViewById(R.id.btnRegister);

    tietName.addTextChangedListener(new MyTextWatcher(tietName));
    tietEmail.addTextChangedListener(new MyTextWatcher(tietEmail));
    tietPassword.addTextChangedListener(new MyTextWatcher(tietPassword));
    tietConfirmPassword.addTextChangedListener(new MyTextWatcher(tietConfirmPassword));
    tietConfirmPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {

      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))
            || (actionId == EditorInfo.IME_ACTION_DONE)) {
          btnRegister.performClick();
          return true;
        }
        return false;
      }

    });
    btnRegister.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {

        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().
            getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getActivity().getCurrentFocus() != null)
          inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().
              getWindowToken(), 0);

        if (validateName() & validateEmail() & validatePassword() & validateConfirmPassword()) {
          dbLoader.reCreateDb();
          long _id = dbLoader.createUser(new User());
          String user_online_id = OnlineIdGenerator.generateOnlineId(
              DbConstants.User.DATABASE_TABLE, _id);
          String name = tietName.getText().toString().trim();
          String email = tietEmail.getText().toString().trim();
          String password = tietPassword.getText().toString().trim();
          register(user_online_id, name, email, password, _id);
        }
      }

    });

    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    listener.onSetActionBarTitle(getString(R.string.register));
    if (getActivity() != null)
      // Ha előtérbe kerül a Fragment, akkor a kijelző mindig portré módban lesz.
      getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
  }

  @Override
  public void onPause() {
    super.onPause();
    if (getActivity() != null)
      // Ha háttérbe kerül a Fragment, akkor a kijelző elforgathatóvá válik.
      getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
  }

  /**
   * Regisztrálja a felhasználót a szerveren.
   * @param user_online_id Felhasználó online_id-ja.
   * @param name Felhasználó neve.
   * @param email Felhasználó email-je.
   * @param _id Felhasználó _id-ja.
   * @param password Felhasználó jelszava.
   */
  private void register(final String user_online_id, final String name, final String email,
                        final String password, final long _id) {
    String tag_json_object_request = "request_register";

    JSONObject jsonRequest = new JSONObject();
    try {
      jsonRequest.put("user_online_id", user_online_id);
      jsonRequest.put("name", name);
      jsonRequest.put("email", email);
      jsonRequest.put("password", password);
    } catch (JSONException e) {
      e.printStackTrace();
    }

    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(JsonObjectRequest.Method.POST,
        AppConfig.URL_REGISTER, jsonRequest, new Response.Listener<JSONObject>() {

      @Override
      public void onResponse(JSONObject response) {
        Log.d(TAG, "Register Response: " + response);
        try {
          boolean error = response.getBoolean("error");

          if (!error) {
            listener.onClickLinkToLogin();
          } else {
            String message = response.getString("message");
            if (message != null && message.contains("Sorry, this email already existed")) {
              tvFormSubmissionErrors.setText(R.string.this_email_already_existed);
              tvFormSubmissionErrors.setVisibility(View.VISIBLE);
            } else if (
                message != null && message.contains(
                    "Oops! An error occurred while registereing")) {
              // A hiba oka általában, hogy már regisztrált userOnlineId-t generált a rendszer. Ez
              // esetben ettől különböző userOnlineId-t generálunk és ismét megkíséreljük a
              // regisztrációt.
              InstallationIdHelper.getNewInstallationId();
              String new_user_online_id =
                  OnlineIdGenerator.generateOnlineId(
                      DbConstants.User.DATABASE_TABLE, _id);
              register(new_user_online_id, name, email, password, _id);
            }
          }
        } catch (JSONException e) {
          e.printStackTrace();
          hideTVFormSubmissionErrors();
        }
      }

    }, new Response.ErrorListener() {

      @Override
      public void onErrorResponse(VolleyError error) {
        Log.e(TAG, "Register Error: " + error.getMessage());
        if (error.getMessage() != null && error.getMessage().contains("failed to connect")) {
          hideTVFormSubmissionErrors();
          // Sikertelen kapcsolódás.
          AppController.showWhiteTextSnackbar(
              Snackbar.make(coordinatorLayout,
                  R.string.failed_to_connect, Snackbar.LENGTH_LONG)
          );
        } else {
          hideTVFormSubmissionErrors();
          AppController.showWhiteTextSnackbar(
              Snackbar.make(coordinatorLayout,
                  R.string.an_error_occurred, Snackbar.LENGTH_LONG)
          );
        }
      }

    });

    AppController.getInstance().addToRequestQueue(jsonObjectRequest, tag_json_object_request);
  }

  /**
   * Validálja a Name mezőt.
   * @return Kitöltött mező esetén true, egyébként false.
   */
  private boolean validateName() {
    if (tietName.getText().toString().trim().isEmpty()) {
      tilName.setError(getString(R.string.enter_name));
      return false;
    } else {
      tilName.setErrorEnabled(false);
      return true;
    }
  }

  /**
   * Validálja az Email mezőt.
   * @return Érvényes email cím megadása esetén true, egyébként false.
   */
  private boolean validateEmail() {
    String email = tietEmail.getText().toString().trim();
    if (email.isEmpty() || !isValidEmail(email)) {
      tilEmail.setError(getString(R.string.enter_valid_email));
      return false;
    } else {
      tilEmail.setErrorEnabled(false);
      return true;
    }
  }

  /**
   * Validálja a Password mezőt.
   * @return Megfelelő jelszó megadása esetén true, egyébként false.
   */
  private boolean validatePassword() {
    String password = tietPassword.getText().toString().trim();
    if (password.isEmpty() || !isValidPassword(password)) {
      tilPassword.setError(getString(R.string.enter_proper_password));
      return false;
    } else {
      tilPassword.setErrorEnabled(false);
      return true;
    }
  }

  /**
   * Validálja a ConfirmPassword mezőt.
   * @return Kitöltött mező és egyező jelszavak megadása esetén true, egyébként false.
   */
  private boolean validateConfirmPassword() {
    String password = tietPassword.getText().toString().trim();
    String confirmPassword = tietConfirmPassword.getText().toString().trim();
    if (confirmPassword.isEmpty() || !password.equals(confirmPassword)) {
      tilConfirmPassword.setError(getString(R.string.passwords_does_not_match));
      return false;
    } else {
      tilConfirmPassword.setErrorEnabled(false);
      return true;
    }
  }

  /**
   * Megvizsgálja, hogy a megadott email cím megfelelő formátumú-e.
   * @param email A vizsgálandó email cím.
   * @return Megfelelő formátumó email cím esetén true, egyébként false.
   */
  private boolean isValidEmail(String email) {
    return !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches();
  }

  /**
   * Megvizsgálja, hogy a megadott jelszó megfelelő erősségű-e.
   * A jelszónak tartalmaznia kell kisbetűt, nagybetűt, számot, nem tartalmazhat fehér szóközt,
   * hossza pedig minimum 8 karakter.
   * @param password A vizsgálandó jelszó.
   * @return Megfelelő erősségű jelszó esetén true, egyébként false.
   */
  private boolean isValidPassword(String password) {
    return password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,}$");
  }

  private void hideTVFormSubmissionErrors() {
    tvFormSubmissionErrors.setText("");
    tvFormSubmissionErrors.setVisibility(View.GONE);
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

  public interface IRegisterFragment {
    void onClickLinkToLogin();
    void onLogin();
    void onSetActionBarTitle(String title);
  }

}

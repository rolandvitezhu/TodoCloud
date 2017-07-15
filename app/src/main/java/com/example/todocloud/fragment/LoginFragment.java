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
import com.example.todocloud.datastorage.DbLoader;
import com.example.todocloud.helper.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginFragment extends Fragment {

  private static final String TAG = LoginFragment.class.getSimpleName();

  private CoordinatorLayout coordinatorLayout;
  private TextView tvFormSubmissionErrors;
  private TextInputLayout tilEmail, tilPassword;
  private TextInputEditText tietEmail, tietPassword;

  private SessionManager sessionManager;
  private DbLoader dbLoader;
  private ILoginFragment listener;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (ILoginFragment) context;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    sessionManager = new SessionManager(getActivity());

    if (sessionManager.isLoggedIn()) {
      // A felhasználó bejelentkezett, a MainListFragment-et jelenítjük meg.
      listener.onIsLoggedIn();
    }

    dbLoader = new DbLoader(getActivity());
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_login, container, false);
    coordinatorLayout = (CoordinatorLayout) view.findViewById(R.id.coordinatorLayout);
    tvFormSubmissionErrors = (TextView) view.findViewById(R.id.tvFormSubmissionErrors);
    tilEmail = (TextInputLayout) view.findViewById(R.id.tilEmail);
    tilPassword = (TextInputLayout) view.findViewById(R.id.tilPassword);
    tietEmail = (TextInputEditText) view.findViewById(R.id.tietEmail);
    tietPassword = (TextInputEditText) view.findViewById(R.id.tietPassword);
    final Button btnLogin = (Button) view.findViewById(R.id.btnLogin);
    Button btnLinkToRegister = (Button) view.findViewById(R.id.btnLinkToRegister);

    tietEmail.addTextChangedListener(new MyTextWatcher(tietEmail));
    tietPassword.addTextChangedListener(new MyTextWatcher(tietPassword));
    tietPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {

      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))
            || (actionId == EditorInfo.IME_ACTION_DONE)) {
          btnLogin.performClick();
          return true;
        }
        return false;
      }

    });
    btnLogin.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {

        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().
            getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getActivity().getCurrentFocus() != null)
          inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().
              getWindowToken(), 0);

        if (validateEmail() & validatePassword()) {
          dbLoader.reCreateDb();
          String email = tietEmail.getText().toString().trim();
          String password = tietPassword.getText().toString().trim();
          login(email, password);
        }
      }

    });

    // A Button szövege nem lesz csupa nagybetűs.
    btnLinkToRegister.setTransformationMethod(null);

    btnLinkToRegister.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        // A RegisterFragment-et jelenítjük meg.
        listener.onLinkToRegisterClicked();
      }

    });

    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    if (getActivity() != null)
      // Ha előtérbe kerül a Fragment, akkor a kijelző mindig portré módban lesz.
      getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    listener.setActionBarTitle(getString(R.string.login));
  }

  @Override
  public void onPause() {
    super.onPause();
    if (getActivity() != null)
      // Ha háttérbe kerül a Fragment, akkor a kijelző elforgathatóvá válik.
      getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
  }

  /**
   * Validálja az Email mezőt.
   * @return Kitöltött mező esetén true, egyébként false.
   */
  private boolean validateEmail() {
    if (tietEmail.getText().toString().trim().isEmpty()) {
      tilEmail.setError(getString(R.string.enter_email));
      return false;
    } else {
      tilEmail.setErrorEnabled(false);
      return true;
    }
  }

  /**
   * Validálja a Password mezőt.
   * @return Kitöltött mező esetén true, egyébként false.
   */
  private boolean validatePassword() {
    if (tietPassword.getText().toString().trim().isEmpty()) {
      tilPassword.setError(getString(R.string.enter_password));
      return false;
    } else {
      tilPassword.setErrorEnabled(false);
      return true;
    }
  }

  /**
   * Bejelentkezteti a felhasználót.
   * @param email Felhasználó email-je.
   * @param password Felhasználó jelszava.
   */
  private void login(String email, String password) {
    String tag_json_object_request = "request_login";

    JSONObject jsonRequest = new JSONObject();
    try {
      jsonRequest.put("email", email);
      jsonRequest.put("password", password);
    } catch (JSONException e) {
      e.printStackTrace();
    }

    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(JsonObjectRequest.Method.POST,
        AppConfig.URL_LOGIN, jsonRequest, new Response.Listener<JSONObject>() {

      @Override
      public void onResponse(JSONObject response) {
        Log.d(TAG, "Login Response: " + response);
        try {
          boolean error = response.getBoolean("error");

          if (!error) {
            hideTVFormSubmissionErrors();

            User user = new User();
            user.setUserOnlineId(response.getString("user_online_id"));
            user.setName(response.getString("name"));
            user.setEmail(response.getString("email"));
            user.setApiKey(response.getString("api_key"));

            dbLoader.createUser(user);
            sessionManager.setLogin(true);
            listener.onIsLoggedIn();
          } else {
            String message = response.getString("message");
            if (message != null && message.contains("Login failed. Incorrect credentials")) {
              tvFormSubmissionErrors.setText(R.string.invalid_username_or_password);
              tvFormSubmissionErrors.setVisibility(View.VISIBLE);
            } else if (
                message != null && message.contains("An error occurred. Please try again")) {
              hideTVFormSubmissionErrors();
              AppController.showWhiteTextSnackbar(
                  Snackbar.make(coordinatorLayout,
                      R.string.an_error_occurred, Snackbar.LENGTH_LONG)
              );
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
        Log.e(TAG, "Login Error: " + error.getMessage());
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
   * Elrejti a tvFormSubmissionErrors komponenst.
   */
  private void hideTVFormSubmissionErrors() {
    tvFormSubmissionErrors.setText("");
    tvFormSubmissionErrors.setVisibility(View.GONE);
  }

  /**
   * Interfész, a MainActivity-vel való kommunikációra.
   */
  public interface ILoginFragment {
    void onLinkToRegisterClicked();
    void onIsLoggedIn();
    void setActionBarTitle(String title);
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
        case R.id.tietEmail:
          validateEmail();
          break;
        case R.id.tietPassword:
          validatePassword();
          break;
      }
    }

  }

}

package com.rolandvitezhu.todocloud.datasynchronizer;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.rolandvitezhu.todocloud.app.AppConfig;
import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.data.User;
import com.rolandvitezhu.todocloud.helper.InstallationIdHelper;
import com.rolandvitezhu.todocloud.helper.OnlineIdGenerator;
import com.rolandvitezhu.todocloud.helper.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class UserDataSynchronizer extends BaseDataSynchronizer {

  private static final String TAG = UserDataSynchronizer.class.getSimpleName();

  @Inject
  SessionManager sessionManager;

  private OnRegisterUserListener onRegisterUserListener;
  private OnLoginUserListener onLoginUserListener;
  private OnModifyPasswordListener onModifyPasswordListener;
  private OnResetPasswordListener onResetPasswordListener;

  public void setOnRegisterUserListener(OnRegisterUserListener onRegisterUserListener) {
    this.onRegisterUserListener = onRegisterUserListener;
  }

  public void setOnLoginUserListener(OnLoginUserListener onLoginUserListener) {
    this.onLoginUserListener = onLoginUserListener;
  }

  public void setOnModifyPasswordListener(OnModifyPasswordListener onModifyPasswordListener) {
    this.onModifyPasswordListener = onModifyPasswordListener;
  }

  public void setOnResetPasswordListener(OnResetPasswordListener onResetPasswordListener) {
    this.onResetPasswordListener = onResetPasswordListener;
  }

  public UserDataSynchronizer() {
    AppController.getInstance().getAppComponent().inject(this);
  }

  public void registerUser(
      final String user_online_id,
      final String name,
      final String email,
      final String password,
      final long _id
  ) {
    AppController appController = AppController.getInstance();
    String tag_json_object_request = "request_register";
    JsonObjectRequest registerUserRequest = prepareRegisterUserRequest(
        user_online_id,
        name,
        email,
        password,
        _id
    );
    appController.addToRequestQueue(registerUserRequest, tag_json_object_request);
  }

  public void loginUser(String email, String password) {
    AppController appController = AppController.getInstance();
    String tag_json_object_request = "request_login";
    JsonObjectRequest loginUserRequest = prepareLoginUserRequest(email, password);
    appController.addToRequestQueue(loginUserRequest, tag_json_object_request);
  }

  public void modifyPassword(
      final String currentPassword,
      final String newPassword
  ) {
    AppController appController = AppController.getInstance();
    String tag_json_object_request = "request_modify_password";
    JsonObjectRequest modifyPasswordRequest = prepareModifyPasswordRequest(
        currentPassword,
        newPassword
    );
    appController.addToRequestQueue(modifyPasswordRequest, tag_json_object_request);
  }

  public void resetPassword(
      final String email
  ) {
    AppController appController = AppController.getInstance();
    String tag_json_object_request = "request_reset_password";
    JsonObjectRequest resetPasswordRequest = prepareResetPasswordRequest(
        email
    );
    appController.addToRequestQueue(resetPasswordRequest, tag_json_object_request);
  }

  private JsonObjectRequest prepareRegisterUserRequest(
      final String user_online_id,
      final String name,
      final String email,
      final String password,
      final long _id
  ) {
    JSONObject registerUserJsonRequest = prepareRegisterUserJsonRequest(
        user_online_id,
        name,
        email,
        password
    );
    JsonObjectRequest registerUserRequest = new JsonObjectRequest(
        JsonObjectRequest.Method.POST,
        AppConfig.URL_REGISTER,
        registerUserJsonRequest,
        new Response.Listener<JSONObject>() {

          @Override
          public void onResponse(JSONObject response) {
            Log.d(TAG, "Register Response: " + response);
            try {
              boolean error = response.getBoolean("error");

              if (!error) {
                onRegisterUserListener.onFinishRegisterUser();
              } else {
                String message = response.getString("message");
                if (message == null) message = "Unknown error";
                if (message.contains("Oops! An error occurred while registereing")) {
                  handleError();
                } else {
                  onRegisterUserListener.onSyncError(message);
                }
              }
            } catch (JSONException e) {
              e.printStackTrace();
              String errorMessage = "Unknown error";
              onRegisterUserListener.onSyncError(errorMessage);
            }
          }

          /**
           * Generally the cause of error is, that the userOnlineId generated by the client is
           * already registered in the remote database. In this case, it generate a different
           * userOnlineId, and send the registration request again.
           */
          private void handleError() {
            InstallationIdHelper.getNewInstallationId();
            String new_user_online_id = OnlineIdGenerator.generateUserOnlineId(_id);
            registerUser(new_user_online_id, name, email, password, _id);
          }

        },
        new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError error) {
            String errorMessage = error.getMessage();
            Log.e(TAG, "Register Error: " + errorMessage);
            if (errorMessage == null) errorMessage = "Unknown error";
            onRegisterUserListener.onSyncError(errorMessage);
          }

        }
    );
    return registerUserRequest;
  }

  private JSONObject prepareRegisterUserJsonRequest(
      final String user_online_id,
      final String name,
      final String email,
      final String password
  ) {
    JSONObject registerUserJsonRequest = new JSONObject();
    try {
      putUserRegisterData(user_online_id, name, email, password, registerUserJsonRequest);
    } catch (JSONException e) {
      e.printStackTrace();
      String errorMessage = "Unknown error";
      onRegisterUserListener.onSyncError(errorMessage);
    }
    return registerUserJsonRequest;
  }

  private void putUserRegisterData(
      String user_online_id,
      String name,
      String email,
      String password,
      JSONObject jsonRequest
  ) throws JSONException {
    jsonRequest.put("user_online_id", user_online_id);
    jsonRequest.put("name", name);
    jsonRequest.put("email", email);
    jsonRequest.put("password", password);
  }

  private JsonObjectRequest prepareLoginUserRequest(String email, String password) {
    JSONObject loginUserJsonRequest = prepareLoginUserJsonRequest(email, password);
    JsonObjectRequest loginUserRequest = new JsonObjectRequest(
        JsonObjectRequest.Method.POST,
        AppConfig.URL_LOGIN,
        loginUserJsonRequest,
        new Response.Listener<JSONObject>() {

          @Override
          public void onResponse(JSONObject response) {
            Log.d(TAG, "Login Response: " + response);
            try {
              boolean error = response.getBoolean("error");

              if (!error) {
                handleLogin(response);
                onLoginUserListener.onFinishLoginUser();
              } else {
                String message = response.getString("message");
                if (message == null) message = "Unknown error";
                onLoginUserListener.onSyncError(message);
              }
            } catch (JSONException e) {
              e.printStackTrace();
              String errorMessage = "Unknown error";
              onLoginUserListener.onSyncError(errorMessage);
            }
          }

          private void handleLogin(JSONObject response) throws JSONException {
            User user = new User(response);
            dbLoader.createUser(user);
            sessionManager.setLogin(true);
          }

        },
        new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError error) {
            String errorMessage = error.getMessage();
            Log.e(TAG, "Login Error: " + errorMessage);
            if (errorMessage == null) errorMessage = "Unknown error";
            onLoginUserListener.onSyncError(errorMessage);
          }

        }
    );
    return loginUserRequest;
  }

  private JSONObject prepareLoginUserJsonRequest(String email, String password) {
    JSONObject loginUserJsonRequest = new JSONObject();
    try {
      putUserLoginData(email, password, loginUserJsonRequest);
    } catch (JSONException e) {
      e.printStackTrace();
      String errorMessage = "Unknown error";
      onLoginUserListener.onSyncError(errorMessage);
    }
    return loginUserJsonRequest;
  }

  private void putUserLoginData(
      String email,
      String password,
      JSONObject jsonRequest
  ) throws JSONException {
    jsonRequest.put("email", email);
    jsonRequest.put("password", password);
  }

  private JsonObjectRequest prepareModifyPasswordRequest(
      final String currentPassword,
      final String newPassword
  ) {
    JSONObject modifyPasswordJsonRequest = prepareModifyPasswordJsonRequest(
        currentPassword,
        newPassword
    );
    JsonObjectRequest modifyPasswordRequest = new JsonObjectRequest(
        JsonObjectRequest.Method.POST,
        AppConfig.URL_MODIFY_PASSWORD,
        modifyPasswordJsonRequest,
        new Response.Listener<JSONObject>() {

          @Override
          public void onResponse(JSONObject response) {
            Log.d(TAG, "Modify password response: " + response);
            try {
              boolean error = response.getBoolean("error");

              if (!error) {
                onModifyPasswordListener.onFinishModifyPassword();
              } else {
                String message = response.getString("message");
                if (message == null)
                  message = "Unknown error";
                else {
                  onModifyPasswordListener.onSyncError(message);
                }
              }
            } catch (JSONException e) {
              e.printStackTrace();
              String errorMessage = "Unknown error";
              onModifyPasswordListener.onSyncError(errorMessage);
            }
          }

        },
        new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError error) {
            String errorMessage = error.getMessage();
            Log.e(TAG, "Modify password response: " + errorMessage);
            if (errorMessage == null) errorMessage = "Unknown error";
            onModifyPasswordListener.onSyncError(errorMessage);
          }

        }
    ) {

      @Override
      public Map<String, String> getHeaders() throws AuthFailureError {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("authorization", dbLoader.getApiKey());
        return headers;
      }

    };
    return modifyPasswordRequest;
  }

  private JsonObjectRequest prepareResetPasswordRequest(
      final String email
  ) {
    JSONObject resetPasswordJsonRequest = prepareResetPasswordJsonRequest(
        email
    );
    JsonObjectRequest resetPasswordRequest = new JsonObjectRequest(
        JsonObjectRequest.Method.POST,
        AppConfig.URL_RESET_PASSWORD,
        resetPasswordJsonRequest,
        new Response.Listener<JSONObject>() {

          @Override
          public void onResponse(JSONObject response) {
            Log.d(TAG, "Reset password response: " + response);
            try {
              boolean error = response.getBoolean("error");

              if (!error) {
                onResetPasswordListener.onFinishResetPassword();
              } else {
                String message = response.getString("message");
                if (message == null)
                  message = "Unknown error";
                else {
                  onResetPasswordListener.onSyncError(message);
                }
              }
            } catch (JSONException e) {
              e.printStackTrace();
              String errorMessage = "Unknown error";
              onResetPasswordListener.onSyncError(errorMessage);
            }
          }

        },
        new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError error) {
            String errorMessage = error.getMessage();
            Log.e(TAG, "Reset password response: " + errorMessage);
            if (errorMessage == null) errorMessage = "Unknown error";
            onResetPasswordListener.onSyncError(errorMessage);
          }

        }
    );
    return resetPasswordRequest;
  }

  private JSONObject prepareModifyPasswordJsonRequest(
      final String currentPassword,
      final String newPassword
  ) {
    JSONObject modifyPasswordJsonRequest = new JSONObject();
    try {
      putModifyPasswordData(currentPassword, newPassword, modifyPasswordJsonRequest);
    } catch (JSONException e) {
      e.printStackTrace();
      String errorMessage = "Unknown error";
      onModifyPasswordListener.onSyncError(errorMessage);
    }
    return modifyPasswordJsonRequest;
  }

  private JSONObject prepareResetPasswordJsonRequest(
      final String email
  ) {
    JSONObject resetPasswordJsonRequest = new JSONObject();
    try {
      putResetPasswordData(email, resetPasswordJsonRequest);
    } catch (JSONException e) {
      e.printStackTrace();
      String errorMessage = "Unknown error";
      onResetPasswordListener.onSyncError(errorMessage);
    }
    return resetPasswordJsonRequest;
  }

  private void putModifyPasswordData(
      String currentPassword,
      String newPassword,
      JSONObject jsonRequest
  ) throws JSONException {
    jsonRequest.put("current_password", currentPassword);
    jsonRequest.put("new_password", newPassword);
  }

  private void putResetPasswordData(
      String email,
      JSONObject jsonRequest
  ) throws JSONException {
    jsonRequest.put("email", email);
  }

  public interface OnRegisterUserListener {
    void onFinishRegisterUser();
    void onSyncError(String errorMessage);
  }

  public interface OnLoginUserListener {
    void onFinishLoginUser();
    void onSyncError(String errorMessage);
  }

  public interface OnModifyPasswordListener {
    void onFinishModifyPassword();
    void onSyncError(String errorMessage);
  }

  public interface OnResetPasswordListener {
    void onFinishResetPassword();
    void onSyncError(String errorMessage);
  }

}
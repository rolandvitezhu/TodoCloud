package com.rolandvitezhu.todocloud.data;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

public class User implements Parcelable {

  private long _id;
  private String userOnlineId;
  private String name;
  private String email;
  private String apiKey;

  public User() {
  }

  public User(long _id, String userOnlineId, String name, String email, String apiKey) {
    this._id = _id;
    this.userOnlineId = userOnlineId;
    this.name = name;
    this.email = email;
    this.apiKey = apiKey;
  }

  protected User(Parcel in) {
    _id = in.readLong();
    userOnlineId = in.readString();
    name = in.readString();
    email = in.readString();
    apiKey = in.readString();
  }

  public User(Cursor cursor) {
    _id = cursor.getLong(0);
    userOnlineId = cursor.getString(1);
    name = cursor.getString(2);
    email = cursor.getString(3);
    apiKey = cursor.getString(4);
  }

  public User(JSONObject jsonUser) throws JSONException {
    userOnlineId = jsonUser.getString("user_online_id");
    name = jsonUser.getString("name");
    email = jsonUser.getString("email");
    apiKey = jsonUser.getString("api_key");
  }

  public long get_id() {
    return _id;
  }

  public void set_id(long _id) {
    this._id = _id;
  }

  public String getUserOnlineId() {
    return userOnlineId;
  }

  public void setUserOnlineId(String userOnlineId) {
    this.userOnlineId = userOnlineId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public static final Creator<User> CREATOR = new Creator<User>() {
    @Override
    public User createFromParcel(Parcel in) {
      return new User(in);
    }

    @Override
    public User[] newArray(int size) {
      return new User[size];
    }
  };

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(_id);
    dest.writeString(userOnlineId);
    dest.writeString(name);
    dest.writeString(email);
    dest.writeString(apiKey);
  }

}

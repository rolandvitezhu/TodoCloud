package com.rolandvitezhu.todocloud.data;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import static com.rolandvitezhu.todocloud.datastorage.DbConstants.List.KEY_CATEGORY_ONLINE_ID;
import static com.rolandvitezhu.todocloud.datastorage.DbConstants.List.KEY_DELETED;
import static com.rolandvitezhu.todocloud.datastorage.DbConstants.List.KEY_LIST_ONLINE_ID;
import static com.rolandvitezhu.todocloud.datastorage.DbConstants.List.KEY_POSITION;
import static com.rolandvitezhu.todocloud.datastorage.DbConstants.List.KEY_ROW_VERSION;
import static com.rolandvitezhu.todocloud.datastorage.DbConstants.List.KEY_TITLE;
import static com.rolandvitezhu.todocloud.datastorage.DbConstants.List.KEY_USER_ONLINE_ID;

public class List implements Parcelable {

  private long _id;
  private String listOnlineId;
  private String userOnlineId;
  private String categoryOnlineId;
  private String title;
  private int rowVersion;
  private Boolean deleted;
  private Boolean dirty;
  private int position;

//  private int numberOfTodos;

  public List() {
  }

  public List(long _id, String listOnlineId, String userOnlineId, String categoryOnlineId,
              String title, int rowVersion, Boolean deleted, Boolean dirty, int position) {
    this._id = _id;
    this.listOnlineId = listOnlineId;
    this.userOnlineId = userOnlineId;
    this.categoryOnlineId = categoryOnlineId;
    this.title = title;
    this.rowVersion = rowVersion;
    this.deleted = deleted;
    this.dirty = dirty;
    this.position = position;
  }

  protected List(Parcel in) {
    _id = in.readLong();
    listOnlineId = in.readString();
    userOnlineId = in.readString();
    categoryOnlineId = in.readString();
    title = in.readString();
    rowVersion = in.readInt();
    deleted = in.readByte() != 0;
    dirty = in.readByte() != 0;
    position = in.readInt();

//    numberOfTodos = in.readInt();
  }

  public List(Cursor cursor) {
    _id = cursor.getLong(0);
    listOnlineId = cursor.getString(1);
    userOnlineId = cursor.getString(2);
    categoryOnlineId = cursor.getString(3);
    title = cursor.getString(4);
    rowVersion = cursor.getInt(5);
    deleted = cursor.getInt(6) != 0;
    dirty = cursor.getInt(7) != 0;
    position = cursor.getInt(8);
  }

  public List(JSONObject jsonList) throws JSONException {
    listOnlineId = jsonList.getString(KEY_LIST_ONLINE_ID);
    userOnlineId = jsonList.getString(KEY_USER_ONLINE_ID);
    categoryOnlineId = jsonList.getString(KEY_CATEGORY_ONLINE_ID);
    title = jsonList.getString(KEY_TITLE);
    rowVersion = jsonList.getInt(KEY_ROW_VERSION);
    deleted = jsonList.getInt(KEY_DELETED) != 0;
    dirty = false;
    position = jsonList.getInt(KEY_POSITION);
  }

  public static final Creator<List> CREATOR = new Creator<List>() {
    @Override
    public List createFromParcel(Parcel in) {
      return new List(in);
    }

    @Override
    public List[] newArray(int size) {
      return new List[size];
    }
  };

  public long get_id() {
    return _id;
  }

  public void set_id(long _id) {
    this._id = _id;
  }

  public String getListOnlineId() {
    return listOnlineId;
  }

  public void setListOnlineId(String listOnlineId) {
    this.listOnlineId = listOnlineId;
  }

  public String getUserOnlineId() {
    return userOnlineId;
  }

  public void setUserOnlineId(String userOnlineId) {
    this.userOnlineId = userOnlineId;
  }

  public String getCategoryOnlineId() {
    return categoryOnlineId;
  }

  public void setCategoryOnlineId(String categoryOnlineId) {
    this.categoryOnlineId = categoryOnlineId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public int getRowVersion() {
    return rowVersion;
  }

  public void setRowVersion(int rowVersion) {
    this.rowVersion = rowVersion;
  }

  public Boolean getDeleted() {
    return deleted;
  }

  public void setDeleted(Boolean deleted) {
    this.deleted = deleted;
  }

  public Boolean getDirty() {
    return dirty;
  }

  public void setDirty(Boolean dirty) {
    this.dirty = dirty;
  }

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  //  public int getNumberOfTodos() {
//    return numberOfTodos;
//  }
//
//  public void setNumberOfTodos(int numberOfTodos) {
//    this.numberOfTodos = numberOfTodos;
//  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(_id);
    dest.writeString(listOnlineId);
    dest.writeString(userOnlineId);
    dest.writeString(categoryOnlineId);
    dest.writeString(title);
    dest.writeInt(rowVersion);
    dest.writeByte((byte) (deleted ? 1 : 0));
    dest.writeByte((byte) (dirty ? 1 : 0));
    dest.writeInt(position);

//    dest.writeInt(numberOfTodos);
  }

}

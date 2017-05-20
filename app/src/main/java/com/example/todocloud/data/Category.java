package com.example.todocloud.data;

import android.os.Parcel;
import android.os.Parcelable;

public class Category implements Parcelable {

  private long _id;
  private String categoryOnlineId;
  private String userOnlineId;
  private String title;
  private int rowVersion;
  private Boolean deleted;
  private Boolean dirty;

  public Category() {
  }

  public Category(String title) {
    this.title = title;
  }

  public Category(long _id, String categoryOnlineId, String userOnlineId, String title,
                  int rowVersion, Boolean deleted, Boolean dirty) {
    this._id = _id;
    this.categoryOnlineId = categoryOnlineId;
    this.userOnlineId = userOnlineId;
    this.title = title;
    this.rowVersion = rowVersion;
    this.deleted = deleted;
    this.dirty = dirty;
  }

  protected Category(Parcel in) {
    _id = in.readLong();
    categoryOnlineId = in.readString();
    userOnlineId = in.readString();
    title = in.readString();
    rowVersion = in.readInt();
    deleted = in.readByte() != 0;
    dirty = in.readByte() != 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(_id);
    dest.writeString(categoryOnlineId);
    dest.writeString(userOnlineId);
    dest.writeString(title);
    dest.writeInt(rowVersion);
    dest.writeByte((byte) (deleted ? 1 : 0));
    dest.writeByte((byte) (dirty ? 1 : 0));
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<Category> CREATOR = new Creator<Category>() {
    @Override
    public Category createFromParcel(Parcel in) {
      return new Category(in);
    }

    @Override
    public Category[] newArray(int size) {
      return new Category[size];
    }
  };

  public long get_id() {
    return _id;
  }

  public void set_id(long _id) {
    this._id = _id;
  }

  public String getCategoryOnlineId() {
    return categoryOnlineId;
  }

  public void setCategoryOnlineId(String categoryOnlineId) {
    this.categoryOnlineId = categoryOnlineId;
  }

  public String getUserOnlineId() {
    return userOnlineId;
  }

  public void setUserOnlineId(String userOnlineId) {
    this.userOnlineId = userOnlineId;
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

  @Override
  public String toString() {
    return title.toString();
  }

  @Override
  public boolean equals(Object other) {
    boolean result = false;
    if (other instanceof Category) {
      Category that = (Category) other;
      result = (this.get_id() == that.get_id() && this.getTitle().equals(that.getTitle()));
    }
    return result;
  }

}

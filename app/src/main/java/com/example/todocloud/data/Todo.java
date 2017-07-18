package com.example.todocloud.data;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Todo implements Parcelable {

  private long _id;
  private String todoOnlineId;
  private String userOnlineId;
  private String listOnlineId;
	private String title;
	private Boolean priority;
	private String dueDate;
  private String reminderDateTime;
	private String description;
  private Boolean completed;
  private int rowVersion;
  private Boolean deleted;
  private Boolean dirty;

  public Todo() {
  }

	public Todo(long _id, String todoOnlineId, String userOnlineId, String listOnlineId,
              String title, Boolean priority, String dueDate, String reminderDateTime,
              String description, Boolean completed, int rowVersion, Boolean deleted,
              Boolean dirty) {
	  this._id = _id;
    this.todoOnlineId = todoOnlineId;
    this.userOnlineId = userOnlineId;
    this.listOnlineId = listOnlineId;
	  this.title = title;
	  this.priority = priority;
	  this.dueDate = dueDate;
    this.reminderDateTime = reminderDateTime;
	  this.description = description;
    this.completed = completed;
    this.rowVersion = rowVersion;
    this.deleted = deleted;
    this.dirty = dirty;
  }

  protected Todo(Parcel in) {
    _id = in.readLong();
    todoOnlineId = in.readString();
    userOnlineId = in.readString();
    listOnlineId = in.readString();
    title = in.readString();
    priority = in.readByte() != 0;
    dueDate = in.readString();
    reminderDateTime = in.readString();
    description = in.readString();
    completed = in.readByte() != 0;
    rowVersion = in.readInt();
    deleted = in.readByte() != 0;
    dirty = in.readByte() != 0;
  }

  public Todo(JSONObject jsonTodo) throws JSONException {
    todoOnlineId = jsonTodo.getString("todo_online_id");
    userOnlineId = jsonTodo.getString("user_online_id");
    listOnlineId = jsonTodo.getString("list_online_id");
    title = jsonTodo.getString("title");
    priority = jsonTodo.getInt("priority") != 0;
    dueDate = jsonTodo.getString("due_date");
    reminderDateTime = jsonTodo.getString("reminder_datetime");
    description = jsonTodo.getString("description");
    completed = jsonTodo.getInt("completed") != 0;
    rowVersion = jsonTodo.getInt("row_version");
    deleted = jsonTodo.getInt("deleted") != 0;
    dirty = false;
  }

	public long get_id() {
		return _id;
	}

	public void set_id(long _id) {
		this._id = _id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Boolean isPriority() {
		return priority;
	}

	public void setPriority(Boolean priority) {
		this.priority = priority;
	}

	public String getDueDate() {
		return dueDate;
	}

	public void setDueDate(String dueDate) {
		this.dueDate = dueDate;
	}

  public String getReminderDateTime() {
    return reminderDateTime;
  }

  public void setReminderDateTime(String reminderDateTime) {
    this.reminderDateTime = reminderDateTime;
  }

  public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

  public Boolean isCompleted() {
    return completed;
  }

  public void setCompleted(Boolean completed) {
    this.completed = completed;
  }

  public String getTodoOnlineId() {
    return todoOnlineId;
  }

  public void setTodoOnlineId(String todoOnlineId) {
    this.todoOnlineId = todoOnlineId;
  }

  public String getUserOnlineId() {
    return userOnlineId;
  }

  public void setUserOnlineId(String userOnlineId) {
    this.userOnlineId = userOnlineId;
  }

  public String getListOnlineId() {
    return listOnlineId;
  }

  public void setListOnlineId(String listOnlineId) {
    this.listOnlineId = listOnlineId;
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

  public long getReminderDateTimeInLong() {
    SimpleDateFormat reminderDateFormat = new SimpleDateFormat(
        "yyyy.MM.dd HH:mm", Locale.getDefault());
    long reminderDate = 0;

    if (!reminderDateTime.equals("-1")) {
      try {
        reminderDate = reminderDateFormat.parse(reminderDateTime).getTime();
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }

    return reminderDate;
  }

  public static final Creator<Todo> CREATOR = new Creator<Todo>() {
    @Override
    public Todo createFromParcel(Parcel in) {
      return new Todo(in);
    }

    @Override
    public Todo[] newArray(int size) {
      return new Todo[size];
    }
  };

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(_id);
    dest.writeString(todoOnlineId);
    dest.writeString(userOnlineId);
    dest.writeString(listOnlineId);
    dest.writeString(title);
    dest.writeByte((byte) (priority ? 1 : 0));
    dest.writeString(dueDate);
    dest.writeString(reminderDateTime);
    dest.writeString(description);
    dest.writeByte((byte) (completed ? 1 : 0));
    dest.writeInt(rowVersion);
    dest.writeByte((byte) (deleted ? 1 : 0));
    dest.writeByte((byte) (dirty ? 1 : 0));
  }
	
}

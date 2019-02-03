package com.rolandvitezhu.todocloud.data;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;

import com.google.gson.annotations.SerializedName;
import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.app.AppController;

import org.json.JSONException;
import org.json.JSONObject;

import static com.rolandvitezhu.todocloud.datastorage.DbConstants.Todo.KEY_COMPLETED;
import static com.rolandvitezhu.todocloud.datastorage.DbConstants.Todo.KEY_DELETED;
import static com.rolandvitezhu.todocloud.datastorage.DbConstants.Todo.KEY_DESCRIPTION;
import static com.rolandvitezhu.todocloud.datastorage.DbConstants.Todo.KEY_DUE_DATE;
import static com.rolandvitezhu.todocloud.datastorage.DbConstants.Todo.KEY_LIST_ONLINE_ID;
import static com.rolandvitezhu.todocloud.datastorage.DbConstants.Todo.KEY_POSITION;
import static com.rolandvitezhu.todocloud.datastorage.DbConstants.Todo.KEY_PRIORITY;
import static com.rolandvitezhu.todocloud.datastorage.DbConstants.Todo.KEY_REMINDER_DATE_TIME;
import static com.rolandvitezhu.todocloud.datastorage.DbConstants.Todo.KEY_ROW_VERSION;
import static com.rolandvitezhu.todocloud.datastorage.DbConstants.Todo.KEY_TITLE;
import static com.rolandvitezhu.todocloud.datastorage.DbConstants.Todo.KEY_TODO_ONLINE_ID;
import static com.rolandvitezhu.todocloud.datastorage.DbConstants.Todo.KEY_USER_ONLINE_ID;

public class Todo implements Parcelable {

  private long _id;
  @SerializedName("todo_online_id")
  private String todoOnlineId;
  @SerializedName("user_online_id")
  private String userOnlineId;
  @SerializedName("list_online_id")
  private String listOnlineId;
  @SerializedName("title")
	private String title;
  @SerializedName("priority")
	private Boolean priority;
  @SerializedName("due_date")
	private Long dueDate;
  @SerializedName("reminder_date_time")
  private Long reminderDateTime;
  @SerializedName("description")
	private String description;
  @SerializedName("completed")
  private Boolean completed;
  @SerializedName("row_version")
  private int rowVersion;
  @SerializedName("deleted")
  private Boolean deleted;
  private Boolean dirty;
  @SerializedName("position")
  private int position;
  private boolean isSelected;

  public Todo() {
  }

	public Todo(long _id, String todoOnlineId, String userOnlineId, String listOnlineId,
              String title, Boolean priority, Long dueDate, Long reminderDateTime,
              String description, Boolean completed, int rowVersion, Boolean deleted,
              Boolean dirty, int position) {
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
    this.position = position;

    this.isSelected = false;
  }

  public Todo(long _id, String todoOnlineId, String userOnlineId, String listOnlineId,
              String title, Boolean priority, Long dueDate, Long reminderDateTime,
              String description, Boolean completed, int rowVersion, Boolean deleted,
              Boolean dirty, int position, boolean isSelected) {
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
    this.position = position;
    this.isSelected = isSelected;
  }

  public Todo(Todo todo) {
    _id = todo.get_id();
    todoOnlineId = todo.getTodoOnlineId();
    userOnlineId = todo.getUserOnlineId();
    listOnlineId = todo.getListOnlineId();
    title = todo.getTitle();
    priority = todo.isPriority();
    dueDate = todo.getDueDate();
    reminderDateTime = todo.getReminderDateTime();
    description = todo.getDescription();
    completed = todo.isCompleted();
    rowVersion = todo.getRowVersion();
    deleted = todo.getDeleted();
    dirty = todo.getDirty();
    position = todo.getPosition();
    isSelected = todo.isSelected();
  }

  protected Todo(Parcel in) {
    _id = in.readLong();
    todoOnlineId = in.readString();
    userOnlineId = in.readString();
    listOnlineId = in.readString();
    title = in.readString();
    priority = in.readByte() != 0;
    dueDate = in.readLong();
    reminderDateTime = in.readLong();
    description = in.readString();
    completed = in.readByte() != 0;
    rowVersion = in.readInt();
    deleted = in.readByte() != 0;
    dirty = in.readByte() != 0;
    position = in.readInt();

    isSelected = in.readByte() != 0;
  }

  public Todo(Cursor cursor) {
    _id = cursor.getLong(0);
    todoOnlineId = cursor.getString(1);
    userOnlineId = cursor.getString(2);
    listOnlineId = cursor.getString(3);
    title = cursor.getString(4);
    priority = cursor.getInt(5) != 0;
    dueDate = cursor.getLong(6);
    reminderDateTime = cursor.getLong(7);
    description = cursor.getString(8);
    completed = cursor.getInt(9) != 0;
    rowVersion = cursor.getInt(10);
    deleted = cursor.getInt(11) != 0;
    dirty = cursor.getInt(12) != 0;
    position = cursor.getInt(13);

    isSelected = false;
  }

  public Todo(JSONObject jsonTodo) throws JSONException {
    todoOnlineId = jsonTodo.getString(KEY_TODO_ONLINE_ID);
    userOnlineId = jsonTodo.getString(KEY_USER_ONLINE_ID);
    listOnlineId = jsonTodo.getString(KEY_LIST_ONLINE_ID);
    title = jsonTodo.getString(KEY_TITLE);
    priority = jsonTodo.getInt(KEY_PRIORITY) != 0;
    dueDate = jsonTodo.getLong(KEY_DUE_DATE);
    reminderDateTime = jsonTodo.getLong(KEY_REMINDER_DATE_TIME);
    description = jsonTodo.getString(KEY_DESCRIPTION);
    completed = jsonTodo.getInt(KEY_COMPLETED) != 0;
    rowVersion = jsonTodo.getInt(KEY_ROW_VERSION);
    deleted = jsonTodo.getInt(KEY_DELETED) != 0;
    dirty = false;
    position = jsonTodo.getInt(KEY_POSITION);

    isSelected = false;
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

	public Long getDueDate() {
		return dueDate;
	}

	public void setDueDate(Long dueDate) {
		this.dueDate = dueDate;
	}

	public String getFormattedDueDate() {
    if (dueDate != null) {
      return DateUtils.formatDateTime(
          AppController.getAppContext(),
          dueDate,
          DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR
      );
    } else {
      return AppController.getAppContext().getString(R.string.all_noduedate);
    }
  }

  public String getFormattedDueDateForListItem() {
    if (dueDate != null && dueDate != 0) {
      return DateUtils.formatDateTime(
          AppController.getAppContext(),
          dueDate,
          DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR
      );
    } else {
      return "";
    }
  }

  public Long getReminderDateTime() {
    return reminderDateTime;
  }

  public void setReminderDateTime(Long reminderDateTime) {
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
    return dirty != null ? dirty : false;
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

  public boolean isSelected() {
    return isSelected;
  }

  public void setSelected(boolean selected) {
    isSelected = selected;
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
    dest.writeLong(dueDate);
    dest.writeLong(reminderDateTime);
    dest.writeString(description);
    dest.writeByte((byte) (completed ? 1 : 0));
    dest.writeInt(rowVersion);
    dest.writeByte((byte) (deleted ? 1 : 0));
    dest.writeByte((byte) (dirty ? 1 : 0));
    dest.writeInt(position);

    dest.writeByte((byte) (isSelected ? 1 : 0));
  }
	
}

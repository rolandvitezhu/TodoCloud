package com.rolandvitezhu.todocloud.network.api.todo.dto;

import com.google.gson.annotations.SerializedName;

public class UpdateTodoRequest {

  @SerializedName("todo_online_id")
  public String todoOnlineId;
  @SerializedName("list_online_id")
  public String listOnlineId;
  @SerializedName("title")
  public String title;
  @SerializedName("priority")
  public Boolean priority;
  @SerializedName("due_date")
  public Long dueDate;
  @SerializedName("reminder_date_time")
  public Long reminderDateTime;
  @SerializedName("description")
  public String description;
  @SerializedName("completed")
  public Boolean completed;
  @SerializedName("row_version")
  public int rowVersion;
  @SerializedName("deleted")
  public Boolean deleted;
  @SerializedName("position")
  public int position;

  public String getTodoOnlineId() {
    return todoOnlineId;
  }

  public void setTodoOnlineId(String todoOnlineId) {
    this.todoOnlineId = todoOnlineId;
  }

  public String getListOnlineId() {
    return listOnlineId;
  }

  public void setListOnlineId(String listOnlineId) {
    this.listOnlineId = listOnlineId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Boolean getPriority() {
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

  public Boolean getCompleted() {
    return completed;
  }

  public void setCompleted(Boolean completed) {
    this.completed = completed;
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

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }
}

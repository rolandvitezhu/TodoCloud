package com.rolandvitezhu.todocloud.data

import android.database.Cursor
import android.os.Parcelable
import android.text.format.DateUtils
import com.google.gson.annotations.SerializedName
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController.Companion.appContext
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Todo (
        var _id: Long?,
        @SerializedName("todo_online_id")
        var todoOnlineId: String?,
        @SerializedName("user_online_id")
        var userOnlineId: String?,
        @SerializedName("list_online_id")
        var listOnlineId: String?,
        @SerializedName("title")
        var title: String?,
        @SerializedName("priority")
        var priority: Boolean?,
        @SerializedName("due_date")
        var dueDate: Long,
        @SerializedName("reminder_date_time")
        var reminderDateTime: Long,
        @SerializedName("description")
        var description: String?,
        @SerializedName("completed")
        var completed: Boolean?,
        @SerializedName("row_version")
        var rowVersion: Int,
        @SerializedName("deleted")
        var deleted: Boolean?,
        var dirty: Boolean,
        @SerializedName("position")
        var position: Double,
        var isSelected: Boolean = false
) : Parcelable {
    constructor() : this(
            null,
            null,
            null,
            null,
            null,
            false,
            0,
            0,
            null,
            false,
            0,
            false,
            false,
            5.0
    )
    constructor(cursor: Cursor) : this(
            cursor.getLong(0),
            cursor.getString(1),
            cursor.getString(2),
            cursor.getString(3),
            cursor.getString(4),
            cursor.getInt(5) != 0,
            cursor.getLong(6),
            cursor.getLong(7),
            cursor.getString(8),
            cursor.getInt(9) != 0,
            cursor.getInt(10),
            cursor.getInt(11) != 0,
            cursor.getInt(12) != 0,
            if (cursor.getDouble(13) == 0.0) 5.0 else cursor.getDouble(13)
    )
    constructor(todo: Todo) : this(
            todo._id,
            todo.todoOnlineId,
            todo.userOnlineId,
            todo.listOnlineId,
            todo.title,
            todo.priority,
            todo.dueDate,
            todo.reminderDateTime,
            todo.description,
            todo.completed,
            todo.rowVersion,
            todo.deleted,
            todo.dirty,
            todo.position,
            todo.isSelected
    )
    val formattedDueDate: String
        get() {
            if (dueDate != 0L) {
                return DateUtils.formatDateTime(
                        appContext,
                        dueDate,
                        (DateUtils.FORMAT_SHOW_DATE or
                                DateUtils.FORMAT_NUMERIC_DATE or
                                DateUtils.FORMAT_SHOW_YEAR)
                )
            } else {
                return appContext!!.getString(R.string.all_noduedate)
            }
        }
    val formattedReminderDateTime: String
        get() {
            if (reminderDateTime != 0L) {
                return DateUtils.formatDateTime(
                        appContext,
                        reminderDateTime,
                        (DateUtils.FORMAT_SHOW_DATE
                                or DateUtils.FORMAT_NUMERIC_DATE
                                or DateUtils.FORMAT_SHOW_YEAR
                                or DateUtils.FORMAT_SHOW_TIME)
                )
            } else {
                return appContext!!.getString(R.string.all_noreminder)
            }
        }
    val onPredefinedList: Boolean
        get() = listOnlineId == null
}
package com.rolandvitezhu.todocloud.data

import android.database.Cursor
import android.os.Parcelable
import android.text.format.DateUtils
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController.Companion.appContext
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "todo")
@Parcelize
data class Todo (
        @PrimaryKey(autoGenerate = true)
        var _id: Long?,
        @ColumnInfo(name = "todo_online_id")
        @SerializedName("todo_online_id")
        var todoOnlineId: String?,
        @ColumnInfo(name = "user_online_id")
        @SerializedName("user_online_id")
        var userOnlineId: String?,
        @ColumnInfo(name = "list_online_id")
        @SerializedName("list_online_id")
        var listOnlineId: String?,
        @ColumnInfo(name = "title")
        @SerializedName("title")
        var title: String?,
        @ColumnInfo(name = "priority")
        @SerializedName("priority")
        var priority: Boolean? = false,
        @ColumnInfo(name = "due_date")
        @SerializedName("due_date")
        var dueDate: Long = 0,
        @ColumnInfo(name = "reminder_date_time")
        @SerializedName("reminder_date_time")
        var reminderDateTime: Long = 0,
        @ColumnInfo(name = "description")
        @SerializedName("description")
        var description: String?,
        @ColumnInfo(name = "completed")
        @SerializedName("completed")
        var completed: Boolean? = false,
        @ColumnInfo(name = "row_version")
        @SerializedName("row_version")
        var rowVersion: Int = 0,
        @ColumnInfo(name = "deleted")
        @SerializedName("deleted")
        var deleted: Boolean? = false,
        @ColumnInfo(name = "dirty")
        var dirty: Boolean = false,
        @ColumnInfo(name = "position")
        @SerializedName("position")
        var position: Double = 5.0,
        @Ignore
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
            5.0,
            false
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
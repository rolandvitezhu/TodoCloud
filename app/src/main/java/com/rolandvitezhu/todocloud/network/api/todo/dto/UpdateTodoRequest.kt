package com.rolandvitezhu.todocloud.network.api.todo.dto

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UpdateTodoRequest (
        @SerializedName("todo_online_id")
        var todoOnlineId: String?,
        @SerializedName("list_online_id")
        var listOnlineId: String?,
        @SerializedName("title")
        var title: String?,
        @SerializedName("priority")
        var priority: Boolean?,
        @SerializedName("due_date")
        var dueDate: Long?,
        @SerializedName("reminder_date_time")
        var reminderDateTime: Long?,
        @SerializedName("description")
        var description: String?,
        @SerializedName("completed")
        var completed: Boolean?,
        @SerializedName("row_version")
        var rowVersion: Int?,
        @SerializedName("deleted")
        var deleted: Boolean?,
        @SerializedName("position")
        var position: Double?
) : Parcelable {
    constructor() : this(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
    )
}
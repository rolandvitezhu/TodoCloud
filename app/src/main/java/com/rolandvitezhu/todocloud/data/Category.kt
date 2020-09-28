package com.rolandvitezhu.todocloud.data

import android.database.Cursor
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Category (
        var _id: Long?,
        @SerializedName("category_online_id")
        var categoryOnlineId: String?,
        @SerializedName("user_online_id")
        var userOnlineId: String?,
        @SerializedName("title")
        var title: String,
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
            "",
            0,
            false,
            false,
            5.0
    )
    constructor(title: String) : this(
            null,
            null,
            null,
            title,
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
        cursor.getInt(4),
        cursor.getInt(5) != 0,
        cursor.getInt(6) != 0,
        if (cursor.getDouble(7) == 0.0) 5.0 else cursor.getDouble(7)
    )

    override fun toString(): String {
        return title
    }
}
package com.rolandvitezhu.todocloud.data

import android.database.Cursor
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "list")
@Parcelize
data class List (
        @PrimaryKey(autoGenerate = true)
        var _id: Long?,
        @ColumnInfo(name = "list_online_id")
        @SerializedName("list_online_id")
        var listOnlineId: String?,
        @ColumnInfo(name = "user_online_id")
        @SerializedName("user_online_id")
        var userOnlineId: String?,
        @ColumnInfo(name = "category_online_id")
        @SerializedName("category_online_id")
        var categoryOnlineId: String?,
        @ColumnInfo(name = "title")
        @SerializedName("title")
        var title: String = "",
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
    constructor() : this (
            null,
            null,
            null,
            null,
            "",
            0,
            false,
            false,
            5.0
    )
    constructor(cursor: Cursor) : this (
        cursor.getLong(0),
        cursor.getString(1),
        cursor.getString(2),
        cursor.getString(3),
        cursor.getString(4),
        cursor.getInt(5),
        cursor.getInt(6) != 0,
        cursor.getInt(7) != 0,
            if (cursor.getDouble(8) == 0.0) 5.0 else cursor.getDouble(8)
    )
}
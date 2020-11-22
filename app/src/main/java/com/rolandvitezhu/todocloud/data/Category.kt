package com.rolandvitezhu.todocloud.data

import android.database.Cursor
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "category")
@Parcelize
data class Category (
        @PrimaryKey(autoGenerate = true)
        var _id: Long?,
        @ColumnInfo(name = "category_online_id")
        @SerializedName("category_online_id")
        var categoryOnlineId: String?,
        @ColumnInfo(name = "user_online_id")
        @SerializedName("user_online_id")
        var userOnlineId: String?,
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
    constructor() : this("")
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
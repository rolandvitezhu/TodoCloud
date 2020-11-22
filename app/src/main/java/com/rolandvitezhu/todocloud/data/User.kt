package com.rolandvitezhu.todocloud.data

import android.database.Cursor
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.rolandvitezhu.todocloud.network.api.user.dto.LoginUserResponse
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "user")
@Parcelize
data class User(
    @PrimaryKey(autoGenerate = true)
    var _id: Long?,
    @ColumnInfo(name = "user_online_id")
    @SerializedName("user_online_id")
    var userOnlineId: String?,
    @ColumnInfo(name = "name")
    @SerializedName("name")
    var name: String = "",
    @ColumnInfo(name = "email")
    @SerializedName("email")
    var email: String = "",
    @ColumnInfo(name = "api_key")
    @SerializedName("api_key")
    var apiKey: String?,
    @Ignore
    @SerializedName("password")
    var password: String = ""
) : Parcelable {
    constructor() : this(
            null,
            null,
            "",
            "",
            "",
            ""
    )
    constructor(cursor: Cursor) : this(
            cursor.getLong(0),
            cursor.getString(1),
            cursor.getString(2),
            cursor.getString(3),
            cursor.getString(4),
            ""
    )
    constructor(loginUserResponse: LoginUserResponse) : this(
            null,
            loginUserResponse.userOnlineId,
            loginUserResponse.name,
            loginUserResponse.email,
            loginUserResponse.apiKey,
            ""
    )
}
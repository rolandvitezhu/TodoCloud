package com.rolandvitezhu.todocloud.data

import android.database.Cursor
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.rolandvitezhu.todocloud.network.api.user.dto.LoginUserResponse
import kotlinx.android.parcel.Parcelize

@Parcelize
data class User(
    var _id: Long?,
    @SerializedName("user_online_id")
    var userOnlineId: String?,
    @SerializedName("name")
    var name: String,
    @SerializedName("email")
    var email: String,
    @SerializedName("api_key")
    var apiKey: String?,
    @SerializedName("password")
    var password: String
) : Parcelable {
    constructor() : this(
            null,
            null,
            "",
            "",
            null,
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
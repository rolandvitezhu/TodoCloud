package com.rolandvitezhu.todocloud.network.api.user.dto

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RegisterUserRequest (
        @SerializedName("user_online_id")
        var userOnlineId: String?,
        @SerializedName("name")
        var name: String?,
        @SerializedName("email")
        var email: String?,
        @SerializedName("password")
        var password: String?
) : Parcelable {
    constructor() : this(
            null,
            null,
            null,
            null
    )
}
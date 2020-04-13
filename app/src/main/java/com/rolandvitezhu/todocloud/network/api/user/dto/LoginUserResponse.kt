package com.rolandvitezhu.todocloud.network.api.user.dto

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.rolandvitezhu.todocloud.network.api.dto.BaseResponse
import kotlinx.android.parcel.Parcelize

@Parcelize
data class LoginUserResponse (
        @SerializedName("user_online_id")
        var userOnlineId: String?,
        @SerializedName("name")
        var name: String?,
        @SerializedName("email")
        var email: String?,
        @SerializedName("api_key")
        var apiKey: String?
) : Parcelable, BaseResponse() {
    constructor() : this(
            null,
            null,
            null,
            null
    )
}
package com.rolandvitezhu.todocloud.network.api.user.dto

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ModifyPasswordRequest (
        @SerializedName("current_password")
        var currentPassword: String?,
        @SerializedName("new_password")
        var newPassword: String?
) : Parcelable {
    constructor() : this(
            null,
            null
    )
}
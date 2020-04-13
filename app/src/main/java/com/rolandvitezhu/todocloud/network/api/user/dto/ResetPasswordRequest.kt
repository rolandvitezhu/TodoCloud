package com.rolandvitezhu.todocloud.network.api.user.dto

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ResetPasswordRequest (
        @SerializedName("email")
        var email: String?
) : Parcelable {
    constructor() : this(
            null
    )
}
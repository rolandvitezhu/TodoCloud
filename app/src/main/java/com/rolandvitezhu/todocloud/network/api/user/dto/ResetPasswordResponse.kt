package com.rolandvitezhu.todocloud.network.api.user.dto

import android.os.Parcelable
import com.rolandvitezhu.todocloud.network.api.dto.BaseResponse
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ResetPasswordResponse (
        var mandatoryField: String?
) : Parcelable, BaseResponse() {
    constructor() : this(
            null
    )
}
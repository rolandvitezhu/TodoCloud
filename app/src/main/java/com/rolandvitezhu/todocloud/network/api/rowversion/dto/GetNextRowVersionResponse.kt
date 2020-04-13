package com.rolandvitezhu.todocloud.network.api.rowversion.dto

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.rolandvitezhu.todocloud.network.api.dto.BaseResponse
import kotlinx.android.parcel.Parcelize

@Parcelize
data class GetNextRowVersionResponse (
        @SerializedName("next_row_version")
        var nextRowVersion: Int?
) : Parcelable, BaseResponse() {
    constructor() : this(
            null
    )
}
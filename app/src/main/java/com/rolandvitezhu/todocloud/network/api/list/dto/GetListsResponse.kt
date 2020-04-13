package com.rolandvitezhu.todocloud.network.api.list.dto

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.rolandvitezhu.todocloud.data.List
import com.rolandvitezhu.todocloud.network.api.dto.BaseResponse
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class GetListsResponse (
        @SerializedName("lists")
        var lists: ArrayList<List?>?
) : Parcelable, BaseResponse() {
    constructor() : this(
            null
    )
}
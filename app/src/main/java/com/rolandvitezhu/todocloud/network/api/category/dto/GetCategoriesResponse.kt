package com.rolandvitezhu.todocloud.network.api.category.dto

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.rolandvitezhu.todocloud.data.Category
import com.rolandvitezhu.todocloud.network.api.dto.BaseResponse
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class GetCategoriesResponse (
        @SerializedName("categories")
        var categories: ArrayList<Category?>?
) : Parcelable, BaseResponse() {
    constructor() : this(
            null
    )
}
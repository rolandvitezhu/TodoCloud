package com.rolandvitezhu.todocloud.network.api.category.dto

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UpdateCategoryRequest (
        @SerializedName("category_online_id")
        var categoryOnlineId: String?,
        @SerializedName("title")
        var title: String?,
        @SerializedName("row_version")
        var rowVersion: Int?,
        @SerializedName("deleted")
        var deleted: Boolean?,
        @SerializedName("position")
        var position: Double?
) : Parcelable {
    constructor() : this(
            null,
            null,
            null,
            null,
            null
    )
}
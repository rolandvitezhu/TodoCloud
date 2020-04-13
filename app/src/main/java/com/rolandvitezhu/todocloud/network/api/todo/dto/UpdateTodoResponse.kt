package com.rolandvitezhu.todocloud.network.api.todo.dto

import android.os.Parcelable
import com.rolandvitezhu.todocloud.network.api.dto.BaseResponse
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UpdateTodoResponse (
        var mandatoryField: String?
) : Parcelable, BaseResponse() {
    constructor() : this(
            null
    )
}
package com.rolandvitezhu.todocloud.network.api.todo.dto

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.rolandvitezhu.todocloud.data.Todo
import com.rolandvitezhu.todocloud.network.api.dto.BaseResponse
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class GetTodosResponse (
        @SerializedName("todos")
        var todos: ArrayList<Todo?>?
) : Parcelable, BaseResponse() {
    constructor() : this(
            null
    )
}
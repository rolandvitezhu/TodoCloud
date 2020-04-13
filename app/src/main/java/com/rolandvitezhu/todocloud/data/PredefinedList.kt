package com.rolandvitezhu.todocloud.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PredefinedList (
        var title: String?,
        var selectFromDB: String?
) : Parcelable {
    constructor() : this(
            null,
            null
    )
}
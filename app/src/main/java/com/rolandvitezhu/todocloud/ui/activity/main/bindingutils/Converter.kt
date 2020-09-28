@file:JvmName("Converter")
package com.rolandvitezhu.todocloud.ui.activity.main.bindingutils

import android.text.format.DateUtils
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController

fun longDateToText(date: Long): String {
    if (date != 0L) {
        return DateUtils.formatDateTime(
                AppController.appContext,
                date,
                (DateUtils.FORMAT_SHOW_DATE or
                        DateUtils.FORMAT_NUMERIC_DATE or
                        DateUtils.FORMAT_SHOW_YEAR)
        )
    } else {
        return AppController.appContext?.getString(R.string.all_noduedate) ?: ""
    }
}

fun longDateTimeToText(dateTime: Long): String {
    if (dateTime != 0L) {
        return DateUtils.formatDateTime(
                AppController.appContext,
                dateTime,
                (DateUtils.FORMAT_SHOW_DATE
                        or DateUtils.FORMAT_NUMERIC_DATE
                        or DateUtils.FORMAT_SHOW_YEAR
                        or DateUtils.FORMAT_SHOW_TIME)
        )
    } else {
        return AppController.appContext?.getString(R.string.all_noreminder) ?: ""
    }
}
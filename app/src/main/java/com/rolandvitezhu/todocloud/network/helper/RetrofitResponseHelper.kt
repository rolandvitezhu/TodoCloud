package com.rolandvitezhu.todocloud.network.helper

import com.google.gson.GsonBuilder
import com.rolandvitezhu.todocloud.network.api.dto.BaseResponse
import retrofit2.Response

open class RetrofitResponseHelper {

    companion object {
        fun ResponseToJson(response: Response<*>): String? {
            return try {
                GsonBuilder().disableHtmlEscaping().serializeNulls().create().toJson(response.body())
            } catch (e: Exception) {
                e.message
            }
        }

        fun IsNoError(response: Response<*>): Boolean {
            return if (response.body() != null) {
                val baseResponse = response.body() as BaseResponse?
                baseResponse!!.getError() == "false"
            } else {
                false
            }
        }
    }
}
package com.rolandvitezhu.todocloud.network

import com.rolandvitezhu.todocloud.data.Category
import com.rolandvitezhu.todocloud.data.List
import com.rolandvitezhu.todocloud.data.Todo
import com.rolandvitezhu.todocloud.data.User
import com.rolandvitezhu.todocloud.network.api.category.dto.GetCategoriesResponse
import com.rolandvitezhu.todocloud.network.api.dto.BaseResponse
import com.rolandvitezhu.todocloud.network.api.list.dto.GetListsResponse
import com.rolandvitezhu.todocloud.network.api.rowversion.dto.GetNextRowVersionResponse
import com.rolandvitezhu.todocloud.network.api.todo.dto.GetTodosResponse
import com.rolandvitezhu.todocloud.network.api.user.dto.LoginUserResponse
import com.rolandvitezhu.todocloud.network.api.user.dto.ModifyPasswordRequest
import retrofit2.http.*

interface ApiService {
    @POST("todo_cloud/v3/user/register")
    suspend fun registerUser(@Body request: User): BaseResponse

    @POST("todo_cloud/v3/user/login")
    suspend fun loginUser(@Body request: User): LoginUserResponse

    @POST("todo_cloud/v3/user/reset_password")
    suspend fun resetPassword(@Body request: User): BaseResponse

    @POST("todo_cloud/v3/user/modify_password")
    suspend fun modifyPassword(@Body request: ModifyPasswordRequest): BaseResponse

    @GET("todo_cloud/v3/todo/{row_version}")
    suspend fun getTodos(@Path("row_version") rowVersion: Int): GetTodosResponse

    @POST("todo_cloud/v3/todo/insert")
    suspend fun insertTodo(@Body request: Todo): BaseResponse

    // 000webhost.com does not allow PUT and DELETE requests for free accounts
    @POST("todo_cloud/v3/todo/update")
    suspend fun updateTodo(@Body request: Todo): BaseResponse

    @GET("todo_cloud/v3/list/{row_version}")
    suspend fun getLists(@Path("row_version") rowVersion: Int): GetListsResponse

    @POST("todo_cloud/v3/list/insert")
    suspend fun insertList(@Body request: List): BaseResponse

    // 000webhost.com does not allow PUT and DELETE requests for free accounts
    @POST("todo_cloud/v3/list/update")
    suspend fun updateList(@Body request: List): BaseResponse

    @GET("todo_cloud/v3/category/{row_version}")
    suspend fun getCategories(@Path("row_version") rowVersion: Int): GetCategoriesResponse

    @POST("todo_cloud/v3/category/insert")
    suspend fun insertCategory(@Body request: Category): BaseResponse

    // 000webhost.com does not allow PUT and DELETE requests for free accounts
    @POST("todo_cloud/v3/category/update")
    suspend fun updateCategory(@Body request: Category): BaseResponse

    @GET("todo_cloud/v3/get_next_row_version/{table}")
    suspend fun getNextRowVersion(
            @Path("table") table: String?, @Header("authorization") apiKey: String?
    ): GetNextRowVersionResponse
}
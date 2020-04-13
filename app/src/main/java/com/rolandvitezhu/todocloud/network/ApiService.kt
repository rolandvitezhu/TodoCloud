package com.rolandvitezhu.todocloud.network

import com.rolandvitezhu.todocloud.network.api.category.dto.*
import com.rolandvitezhu.todocloud.network.api.list.dto.*
import com.rolandvitezhu.todocloud.network.api.todo.dto.*
import com.rolandvitezhu.todocloud.network.api.user.dto.*
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("todo_cloud/v1/user/register")
    fun registerUser(@Body request: RegisterUserRequest?): Single<RegisterUserResponse?>?

    @POST("todo_cloud/v1/user/login")
    fun loginUser(@Body request: LoginUserRequest?): Single<LoginUserResponse?>?

    @POST("todo_cloud/v1/user/reset_password")
    fun resetPassword(@Body request: ResetPasswordRequest?): Single<ResetPasswordResponse?>?

    @POST("todo_cloud/v1/user/modify_password")
    fun modifyPassword(@Body request: ModifyPasswordRequest?): Single<ModifyPasswordResponse?>?

    @GET("todo_cloud/v1/todo/{row_version}")
    fun getTodos(@Path("row_version") rowVersion: Int): Single<GetTodosResponse?>?

    @POST("todo_cloud/v1/todo/insert")
    fun insertTodo(@Body request: InsertTodoRequest?): Single<InsertTodoResponse?>?

    // 000webhost.com doesn't allow PUT and DELETE requests for free accounts
    @POST("todo_cloud/v1/todo/update")
    fun updateTodo(@Body request: UpdateTodoRequest?): Single<UpdateTodoResponse?>?

    @GET("todo_cloud/v1/list/{row_version}")
    fun getLists(@Path("row_version") rowVersion: Int): Single<GetListsResponse?>?

    @POST("todo_cloud/v1/list/insert")
    fun insertList(@Body request: InsertListRequest?): Single<InsertListResponse?>?

    // 000webhost.com doesn't allow PUT and DELETE requests for free accounts
    @POST("todo_cloud/v1/list/update")
    fun updateList(@Body request: UpdateListRequest?): Single<UpdateListResponse?>?

    @GET("todo_cloud/v1/category/{row_version}")
    fun getCategories(@Path("row_version") rowVersion: Int): Single<GetCategoriesResponse?>?

    @POST("todo_cloud/v1/category/insert")
    fun insertCategory(@Body request: InsertCategoryRequest?): Single<InsertCategoryResponse?>?

    // 000webhost.com doesn't allow PUT and DELETE requests for free accounts
    @POST("todo_cloud/v1/category/update")
    fun updateCategory(@Body request: UpdateCategoryRequest?): Single<UpdateCategoryResponse?>?
}
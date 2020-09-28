package com.rolandvitezhu.todocloud.repository

import com.rolandvitezhu.todocloud.app.AppController.Companion.instance
import com.rolandvitezhu.todocloud.datastorage.DbLoader
import com.rolandvitezhu.todocloud.network.ApiService
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class BaseRepository @Inject constructor() {

    @Inject
    lateinit var dbLoader: DbLoader
    @Inject
    lateinit var apiService: ApiService

    var nextRowVersion = 0

    init {
        Objects.requireNonNull(instance)?.appComponent?.inject(this)
    }
}
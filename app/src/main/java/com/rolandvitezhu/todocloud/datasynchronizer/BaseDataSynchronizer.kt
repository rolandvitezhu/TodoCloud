package com.rolandvitezhu.todocloud.datasynchronizer

import com.rolandvitezhu.todocloud.app.AppController.Companion.instance
import com.rolandvitezhu.todocloud.datastorage.DbLoader
import retrofit2.Retrofit
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class BaseDataSynchronizer @Inject constructor() {

    @Inject
    lateinit var dbLoader: DbLoader

    @Inject
    lateinit var retrofit: Retrofit

    var nextRowVersion = 0

    init {
        Objects.requireNonNull(instance)?.appComponent?.inject(this)
    }
}
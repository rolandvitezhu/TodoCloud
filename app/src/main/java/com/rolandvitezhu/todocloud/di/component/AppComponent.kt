package com.rolandvitezhu.todocloud.di.component

import android.content.Context
import com.rolandvitezhu.todocloud.datastorage.DbHelper
import com.rolandvitezhu.todocloud.datastorage.asynctask.UpdateViewModelTask
import com.rolandvitezhu.todocloud.datasynchronizer.BaseDataSynchronizer
import com.rolandvitezhu.todocloud.datasynchronizer.DataSynchronizer
import com.rolandvitezhu.todocloud.di.AppSubcomponents
import com.rolandvitezhu.todocloud.di.module.NetworkModule
import com.rolandvitezhu.todocloud.service.ReminderService
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [NetworkModule::class, AppSubcomponents::class])
interface AppComponent {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): AppComponent
    }

    fun inject(mainActivity: MainActivity?)
    fun inject(updateViewModelTask: UpdateViewModelTask?)
    fun inject(baseDataSynchronizer: BaseDataSynchronizer?)
    fun inject(reminderService: ReminderService?)
    fun inject(dataSynchronizer: DataSynchronizer?)
    fun inject(dbHelper: DbHelper?)

    fun fragmentComponent(): FragmentComponent.Factory
}
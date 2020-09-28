package com.rolandvitezhu.todocloud.di.component

import android.content.Context
import com.rolandvitezhu.todocloud.datastorage.DbHelper
import com.rolandvitezhu.todocloud.di.AppSubcomponents
import com.rolandvitezhu.todocloud.di.module.NetworkModule
import com.rolandvitezhu.todocloud.repository.BaseRepository
import com.rolandvitezhu.todocloud.service.ReminderService
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.*
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

    // We can inject other objects into these classes.
    fun inject(mainActivity: MainActivity?)
    fun inject(baseRepository: BaseRepository?)
    fun inject(reminderService: ReminderService?)
    fun inject(dbHelper: DbHelper?)
    fun inject(categoriesViewModel: CategoriesViewModel)
    fun inject(listsViewModel: ListsViewModel)
    fun inject(todosViewModel: TodosViewModel)
    fun inject(predefinedListsViewModel: PredefinedListsViewModel)
    fun inject(userViewModel: UserViewModel)

    // We can inject other objects into the classes which we have inject functions for in
    // the FragmentComponent.
    fun fragmentComponent(): FragmentComponent.Factory
}
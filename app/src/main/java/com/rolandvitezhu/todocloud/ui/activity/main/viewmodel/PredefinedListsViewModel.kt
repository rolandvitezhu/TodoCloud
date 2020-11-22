package com.rolandvitezhu.todocloud.ui.activity.main.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController.Companion.appContext
import com.rolandvitezhu.todocloud.app.AppController.Companion.instance
import com.rolandvitezhu.todocloud.data.PredefinedList
import com.rolandvitezhu.todocloud.database.TodoCloudDatabaseDao
import java.util.*
import javax.inject.Inject

class PredefinedListsViewModel : ViewModel() {

    @Inject
    lateinit var todoCloudDatabaseDao: TodoCloudDatabaseDao

    private val _predefinedLists = MutableLiveData<List<PredefinedList>>()
    val predefinedLists: LiveData<List<PredefinedList>>
        get() = _predefinedLists
    var predefinedList = PredefinedList()

    /**
     * Set the list of predefined lists.
     */
    suspend fun updatePredefinedListsViewModel() {
        val todayPredefinedListWhere: String = todoCloudDatabaseDao.prepareTodayPredefinedListWhere()
        val next7DaysPredefinedListWhere: String = todoCloudDatabaseDao.prepareNext7DaysPredefinedListWhere()
        val allPredefinedListWhere: String = todoCloudDatabaseDao.prepareAllPredefinedListWhere()
        val completedPredefinedListWhere: String = todoCloudDatabaseDao.prepareCompletedPredefinedListWhere()

        val predefinedLists = ArrayList<PredefinedList>()

        predefinedLists.add(
                PredefinedList(appContext.getString(R.string.all_today), todayPredefinedListWhere))
        predefinedLists.add(
                PredefinedList(appContext.getString(R.string.all_next7days), next7DaysPredefinedListWhere))
        predefinedLists.add(
                PredefinedList(appContext.getString(R.string.all_all), allPredefinedListWhere))
        predefinedLists.add(
                PredefinedList(appContext.getString(R.string.all_completed), completedPredefinedListWhere))

        _predefinedLists.value = predefinedLists
    }

    init {
        instance?.appComponent?.inject(this)
    }
}
package com.rolandvitezhu.todocloud.repository

import com.rolandvitezhu.todocloud.app.AppController.Companion.instance
import com.rolandvitezhu.todocloud.data.List
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListRepository @Inject constructor() : BaseRepository() {

    private var listsToUpdate: ArrayList<List>? = null
    private var listsToInsert: ArrayList<List>? = null

    suspend fun syncListData() {
        val response = apiService.getLists(todoCloudDatabaseDao.getLastListRowVersion())
        if (response.error.toUpperCase(Locale.getDefault()).equals("FALSE")) {
            // Process the response
            updateListsInLocalDatabase(response.lists)

            updateAndOrInsertLists()
        } else {
            // Process the error response
            throw Throwable(response.message)
        }
    }

    private suspend fun updateAndOrInsertLists() {
        val response =
                apiService.getNextRowVersion("list", todoCloudDatabaseDao.getCurrentApiKey())
        if (response.error.toUpperCase(Locale.getDefault()).equals("FALSE")) {
            // Process the response
            nextRowVersion = response.nextRowVersion ?: 0
            listsToUpdate = todoCloudDatabaseDao.getListsToUpdate()
            listsToInsert = todoCloudDatabaseDao.getListsToInsert()

            setRowVersionsForLists(listsToUpdate)
            setRowVersionsForLists(listsToInsert)

            updateLists()
            insertLists()
        } else {
            // Process the error response
            throw Throwable(response.message)
        }
    }

    private suspend fun updateListsInLocalDatabase(lists: ArrayList<List?>?) {
        if (!lists.isNullOrEmpty()) {
            for (list in lists) {
                val exists = list!!.listOnlineId?.let { todoCloudDatabaseDao.isListExists(it) }
                if (exists == true) {
                    todoCloudDatabaseDao.updateList(list)
                } else {
                    todoCloudDatabaseDao.insertList(list)
                }
            }
        }
    }

    private fun setRowVersionsForLists(lists: ArrayList<List>?) {
        if (!lists.isNullOrEmpty()) {
            for (list in lists) {
                list.rowVersion = nextRowVersion++
            }
        }
    }

    private suspend fun updateLists() {
        if (!listsToUpdate.isNullOrEmpty()) {
            // Process the list
            for (listToUpdate in listsToUpdate!!) {
                // Process the list item
                val response = apiService.updateList(listToUpdate)
                if (response.error.toUpperCase(Locale.getDefault()).equals("FALSE")) {
                    // Process the response
                    makeListUpToDate(listToUpdate)
                } else {
                    // Process the error response
                    throw Throwable(response.message)
                }
            }
        }
    }

    private suspend fun insertLists() {
        if (!listsToInsert.isNullOrEmpty()) {
            // Process the list
            for (listToInsert in listsToInsert!!) {
                // Process the list item
                val response = apiService.insertList(listToInsert)
                if (response.error.toUpperCase(Locale.getDefault()).equals("FALSE")) {
                    // Process the response
                    makeListUpToDate(listToInsert)
                } else {
                    // Process the error response
                    throw Throwable(response.message)
                }
            }
        }
    }

    private suspend fun makeListUpToDate(listToUpdate: List) {
        listToUpdate.dirty = false
        todoCloudDatabaseDao.updateList(listToUpdate)
    }

    companion object {
        private val TAG = ListRepository::class.java.simpleName
    }

    init {
        Objects.requireNonNull(instance)?.appComponent?.inject(this)
    }
}
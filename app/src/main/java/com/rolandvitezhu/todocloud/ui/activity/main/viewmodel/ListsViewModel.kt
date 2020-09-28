package com.rolandvitezhu.todocloud.ui.activity.main.viewmodel

import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rolandvitezhu.todocloud.app.AppController.Companion.instance
import com.rolandvitezhu.todocloud.datastorage.DbConstants
import com.rolandvitezhu.todocloud.datastorage.DbLoader
import com.rolandvitezhu.todocloud.helper.OnlineIdGenerator
import com.rolandvitezhu.todocloud.repository.ListRepository
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.MainListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

class ListsViewModel(val categoriesViewModel: CategoriesViewModel?) : ObservableViewModel() {

    @Inject
    lateinit var listRepository: ListRepository
    @Inject
    lateinit var dbLoader: DbLoader

    private val _lists = MutableLiveData<List<com.rolandvitezhu.todocloud.data.List>>()
    val lists: LiveData<List<com.rolandvitezhu.todocloud.data.List>>
        get() = _lists
    var list: com.rolandvitezhu.todocloud.data.List = com.rolandvitezhu.todocloud.data.List()
    var isInCategory = false
    var isListNotInCategoryBeforeMove: Boolean = false

    fun isMoveListOutsideCategory() =
            categoriesViewModel?.category?.categoryOnlineId == null
    fun isListNotInCategory() = list.categoryOnlineId == null

    /**
     * Get all the lists which are not related to categories from the local database and update
     * the adapter to show these lists on the UI.
     */
    suspend fun updateListsViewModel() {
        withContext(Dispatchers.IO) {
            val listsNotInCategory = dbLoader.listsNotInCategory
            withContext(Dispatchers.Main) {
                _lists.value = listsNotInCategory
            }
        }
    }

    /**
     * Synchronizes the lists between the local and remote databases. Refreshes the UI - the lists
     * on the main list screen.
     */
    suspend fun onSynchronization() {
        listRepository.syncListData()
        updateListsViewModel()
    }

    /**
     * Set the initial values of the list.
     */
    fun initializeList() {
        list = com.rolandvitezhu.todocloud.data.List()
    }

    /**
     * Set the online id values of the list, insert it into the local database and update the lists.
     */
    fun onCreateList() {
        list.dirty = true
        viewModelScope.launch {
            createListInLocalDatabase()
            updateListsViewModel()
        }
    }

    /**
     * Set the online id values of the list and insert it into the local database.
     */
    private fun createListInLocalDatabase() {
        list.userOnlineId = dbLoader.userOnlineId
        list._id = dbLoader.createList(list)
        val listOnlineId: String = OnlineIdGenerator.generateOnlineId(
                DbConstants.List.DATABASE_TABLE,
                list._id!!,
                dbLoader.apiKey
        )
        list.listOnlineId = listOnlineId
        dbLoader.updateList(list)
    }

    /**
     * Update the list in the local database. Update the categories, if the list is in a category.
     * Update the lists, if the list is not in a category.
     */
    fun onModifyList() {
        list.dirty = true

        viewModelScope.launch {
            dbLoader.updateList(list)
            if (isInCategory)
                categoriesViewModel?.updateCategoriesViewModel()
            else
                updateListsViewModel()
        }
    }

    /**
     * Create a list which is in a category, insert this list into the local database and update
     * the categories.
     */
    fun onCreateListInCategory() {
        viewModelScope.launch {
            createListInCategoryInLocalDatabase()
            categoriesViewModel?.updateCategoriesViewModel()
        }
    }

    /**
     * Create a list which is in a category and insert it into the local database.
     */
    private fun createListInCategoryInLocalDatabase() {
        list.userOnlineId = dbLoader.userOnlineId
        list.categoryOnlineId = categoriesViewModel?.category?.categoryOnlineId
        list.dirty = true
        list._id = dbLoader.createList(list)
        val listOnlineId: String = OnlineIdGenerator.generateOnlineId(
                DbConstants.List.DATABASE_TABLE,
                list._id!!,
                dbLoader.apiKey
        )
        list.listOnlineId = listOnlineId
        dbLoader.updateList(list)
    }

    /**
     * Move a list from it's original place to the chosen place, update the list in the local
     * database, update the categories and lists.
     */
    fun onMoveList(mainListFragment: MainListFragment) {
        when (
            if (isListNotInCategoryBeforeMove)
                "isListNotInCategoryBeforeMove"
            else
                "isListInCategoryBeforeMove"
            )
        {
            "isListNotInCategoryBeforeMove" -> if (isMoveListOutsideCategory()) {
                mainListFragment.finishActionMode()
            } else {
                moveListIntoCategory()
                mainListFragment.finishActionMode()
            }
            "isListInCategoryBeforeMove" -> if (isMoveListOutsideCategory()) {
                moveListOutsideCategory()
                mainListFragment.finishActionMode()
            } else {
                moveListIntoAnotherCategory()
                mainListFragment.finishActionMode()
            }
        }
    }

    /**
     * Move list into a category, update the list in the local database, update the lists and
     * the categories.
     */
    private fun moveListIntoCategory() {
        list.categoryOnlineId = categoriesViewModel?.category?.categoryOnlineId
        list.dirty = true

        viewModelScope.launch {
            dbLoader.updateList(list)
            updateListsViewModel()
            categoriesViewModel?.updateCategoriesViewModel()
        }
    }

    /**
     * Move list into a category, update the list in the local database and update the categories.
     */
    private fun moveListIntoAnotherCategory() {
        list.categoryOnlineId = categoriesViewModel?.category?.categoryOnlineId
        list.dirty = true

        viewModelScope.launch {
            dbLoader.updateList(list)
            categoriesViewModel?.updateCategoriesViewModel()
        }
    }

    /**
     * Move list from a category to outside of that category, update the list in the local
     * database, update the categories and the lists.
     */
    private fun moveListOutsideCategory() {
        list.categoryOnlineId = null
        list.dirty = true

        viewModelScope.launch {
            dbLoader.updateList(list)
            categoriesViewModel?.updateCategoriesViewModel()
            updateListsViewModel()
        }
    }

    /**
     * Mark lists as deleted, update them in the local database and update the lists.
     */
    fun onSoftDelete(itemsToDelete: ArrayList<*>?, targetFragment: Fragment?) {
        val lists: ArrayList<com.rolandvitezhu.todocloud.data.List> =
                itemsToDelete as ArrayList<com.rolandvitezhu.todocloud.data.List>

        viewModelScope.launch {
            for (list: com.rolandvitezhu.todocloud.data.List in lists) {
                dbLoader.softDeleteListAndRelatedTodos(list.listOnlineId)
            }
            if (isInCategory)
                categoriesViewModel?.updateCategoriesViewModel()
            else
                updateListsViewModel()
        }

        when (targetFragment) {
            is MainListFragment? -> targetFragment?.finishActionMode()
        }
    }

    /**
     * Mark a list as deleted, update it in the local database and update the lists.
     */
    fun onSoftDelete(onlineId: String?, targetFragment: Fragment?) {
        viewModelScope.launch {
            dbLoader.softDeleteListAndRelatedTodos(onlineId)
            if (isInCategory)
                categoriesViewModel?.updateCategoriesViewModel()
            else
                updateListsViewModel()
        }

        when (targetFragment) {
            is MainListFragment? -> targetFragment?.finishActionMode()
        }
    }

    init {
        instance?.appComponent?.inject(this)
    }
}
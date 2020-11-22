package com.rolandvitezhu.todocloud.ui.activity.main.viewmodel

import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController.Companion.appContext
import com.rolandvitezhu.todocloud.app.AppController.Companion.instance
import com.rolandvitezhu.todocloud.data.Category
import com.rolandvitezhu.todocloud.database.TodoCloudDatabaseDao
import com.rolandvitezhu.todocloud.helper.OnlineIdGenerator
import com.rolandvitezhu.todocloud.repository.CategoryRepository
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.MainListFragment
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*
import javax.inject.Inject

class CategoriesViewModel : ViewModel() {

    @Inject
    lateinit var categoryRepository: CategoryRepository
    @Inject
    lateinit var todoCloudDatabaseDao: TodoCloudDatabaseDao

    private val _lhmCategories =
            MutableLiveData<LinkedHashMap<Category, List<com.rolandvitezhu.todocloud.data.List>>>()
    val lhmCategories: LiveData<LinkedHashMap<Category, List<com.rolandvitezhu.todocloud.data.List>>>
        get() = _lhmCategories
    var category: Category = Category()
    var categoriesForSpinner = ArrayList<Category>()

    /**
     * Toggles the "isSelected" property for the category passed as a parameter in the view model.
     * @param category
     */
    fun toggleCategorySelected(category: Category) {
        for (entry in _lhmCategories.value!!.entries) {
            if (entry.key._id == category._id) {
                entry.key.isSelected = category.isSelected.not()
                _lhmCategories.value = _lhmCategories.value
                break
            }
        }
    }

    /**
     * Toggles the "isSelected" property for the list passed as a parameter in the view model.
     * @param list
     */
    fun toggleListSelected(list: com.rolandvitezhu.todocloud.data.List) {
        for (entry in _lhmCategories.value!!.entries) {
            for (listItem in entry.value) {
                if (listItem._id === list._id) {
                    listItem.isSelected = !list.isSelected
                    _lhmCategories.value = _lhmCategories.value
                    return
                }
            }
        }
    }

    /**
     * Set the "isSelected" property of all the Category and List objects in the dictionary data
     * structure.
     */
    fun deselectItems() {
        for (entry in _lhmCategories.value!!.entries) {
            entry.key.isSelected = false
            for (listItem in entry.value) {
                listItem.isSelected = false
            }
        }
        _lhmCategories.value = _lhmCategories.value
    }

    /**
     * Get all the categories related to the current user and all the lists related to these
     * categories from the local database and update the adapter to show them on the UI.
     */
    suspend fun updateCategoriesViewModel() {
        val categoriesAndLists =
                todoCloudDatabaseDao.getCategoriesAndLists()
        _lhmCategories.value = categoriesAndLists
    }

    /**
     * Make API calls to synchronize the categories between the local databases and the remote
     * database. Refreshes the UI - the categories on the main list screen.
     */
    suspend fun onSynchronization() {
        categoryRepository.syncCategoryData()
        updateCategoriesViewModel()
    }

    /**
     * Persist the modifications of the category to the local database and update the categories.
     */
    fun onModifyCategory() {
        category.dirty = true
        viewModelScope.launch {
            todoCloudDatabaseDao.updateCategory(category)
            updateCategoriesViewModel()
        }
    }

    /**
     * Insert the new category into the local database and update the categories.
     */
    fun onCreateCategory() {
        category.dirty = true
        viewModelScope.launch {
            createCategoryInLocalDatabase(category)
            updateCategoriesViewModel()
        }
    }

    /**
     * Set the initial values of the new category.
     */
    fun initializeCategory() {
        category = Category()
    }

    /**
     * Set the online id values of the category and insert the category into the local database.
     */
    private suspend fun createCategoryInLocalDatabase(category: Category) {
        category.userOnlineId = todoCloudDatabaseDao.getCurrentUserOnlineId()
        category._id = todoCloudDatabaseDao.insertCategory(category)
        val categoryOnlineId: String = OnlineIdGenerator.generateOnlineId(
                "category",
                category._id!!,
                todoCloudDatabaseDao.getCurrentApiKey()
        )
        category.categoryOnlineId = categoryOnlineId
        todoCloudDatabaseDao.updateCategory(category)
    }

    /**
     * Mark categories as deleted, update them in the local database and update the categories.
     */
    fun onSoftDelete(itemsToDelete: ArrayList<*>?, targetFragment: Fragment?) {
        val categories: ArrayList<Category> = itemsToDelete as ArrayList<Category>

        viewModelScope.launch {
            for (category: Category in categories) {
                category.categoryOnlineId?.let {
                    todoCloudDatabaseDao.softDeleteCategoryAndListsAndTodos(it)
                }
            }
            updateCategoriesViewModel()
        }

        when (targetFragment) {
            is MainListFragment? -> targetFragment?.finishActionMode()
        }
    }

    /**
     * Mark category as deleted, update it in the local database and update the categories.
     */
    fun onSoftDelete(onlineId: String?, targetFragment: Fragment?) {
        viewModelScope.launch {
            onlineId?.let{ todoCloudDatabaseDao.softDeleteCategoryAndListsAndTodos(it) }
            updateCategoriesViewModel()
        }

        when (targetFragment) {
            is MainListFragment? -> targetFragment?.finishActionMode()
        }
    }

    /**
     * Add an "Outside category" item and all the categories to the array which the spinner will use.
     */
    fun initializeCategoriesForSpinner() {
        runBlocking {
            categoriesForSpinner.clear()
            val outsideCategory = Category(
                    appContext.getString(R.string.movelist_spinneritemlistnotincategory)
            )
            val categoriesFromDatabase = todoCloudDatabaseDao.getCategories()

            categoriesForSpinner.add(outsideCategory)
            categoriesForSpinner.addAll(categoriesFromDatabase)
        }
    }

    init {
        instance?.appComponent?.inject(this)
    }
}
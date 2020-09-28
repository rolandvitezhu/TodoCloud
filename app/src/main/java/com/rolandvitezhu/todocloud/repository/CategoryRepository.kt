package com.rolandvitezhu.todocloud.repository

import com.rolandvitezhu.todocloud.app.AppController.Companion.instance
import com.rolandvitezhu.todocloud.data.Category
import com.rolandvitezhu.todocloud.datastorage.DbConstants
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor() : BaseRepository() {

    private var categoriesToUpdate: ArrayList<Category>? = null
    private var categoriesToInsert: ArrayList<Category>? = null

    /**
     * Synchronizes the categories between the local database and the remote database. First it
     * will fetch the categories from the remote database and persist them in the local database.
     * After it has completed it would update the modified categories and insert the new categories
     * to the remote database.
     */
    suspend fun syncCategoryData() {
        val response = apiService.getCategories(dbLoader.lastCategoryRowVersion)
        if (response.error.toUpperCase(Locale.getDefault()).equals("FALSE")) {
            // Process the response.
            persistCategoriesInLocalDatabase(response.categories)

            // Update and/or insert categories, if there are.
            persistCategoriesInRemoteDatabase()
        } else {
            // Process the error response.
            throw Throwable(response.message)
        }
    }

    /**
     * Inserts the categories into the remote database which are not exists. Updates the categories
     * in the remote database which are already exists.
     */
    private suspend fun persistCategoriesInRemoteDatabase() {
        val response = apiService.getNextRowVersion(DbConstants.Category.DATABASE_TABLE, dbLoader.apiKey)
        if (response.error.toUpperCase(Locale.getDefault()).equals("FALSE")) {
            // Process the response
            nextRowVersion = response.nextRowVersion ?: 0
            categoriesToUpdate = dbLoader.categoriesToUpdate
            categoriesToInsert = dbLoader.categoriesToInsert

            setRowVersionsForCategories(categoriesToUpdate)
            setRowVersionsForCategories(categoriesToInsert)

            updateCategories()
            insertCategories()
        } else {
            // Process the error response
            throw Throwable(response.message)
        }
    }

    private fun setRowVersionsForCategories(categories: ArrayList<Category>?) {
        if (!categories.isNullOrEmpty()) {
            for (category in categories) {
                category.rowVersion = nextRowVersion++
            }
        }
    }

    private suspend fun updateCategories() {
        if (!categoriesToUpdate.isNullOrEmpty()) {
            // Process the list
            for (categoryToUpdate in categoriesToUpdate!!) {
                // Process the list item
                val response = apiService.updateCategory(categoryToUpdate)
                if (response.error.toUpperCase(Locale.getDefault()).equals("FALSE")) {
                    // Process the response
                    makeCategoryUpToDate(categoryToUpdate)
                } else {
                    // Process the error response
                    throw Throwable(response.message)
                }
            }
        }
    }

    private suspend fun insertCategories() {
        if (!categoriesToInsert.isNullOrEmpty()) {
            // Process the list
            for (categoryToInsert in categoriesToInsert!!) {
                // Process the list item
                val response = apiService.insertCategory(categoryToInsert)
                if (response.error.toUpperCase(Locale.getDefault()).equals("FALSE")) {
                    // Process the response
                    makeCategoryUpToDate(categoryToInsert)
                } else {
                    // Process the error response
                    throw Throwable(response.message)
                }
            }
        }
    }

    /**
     * Creates the categories in the local database which are not exists. Updates the categories
     * in the local database which are already exists.
     */
    private fun persistCategoriesInLocalDatabase(categories: ArrayList<Category?>?) {
        if (!categories.isNullOrEmpty()) {
            for (category in categories) {
                val exists = dbLoader.isCategoryExists(category!!.categoryOnlineId)
                if (!exists) {
                    dbLoader.createCategory(category)
                } else {
                    dbLoader.updateCategory(category)
                }
            }
        }
    }

    private fun makeCategoryUpToDate(categoryToUpdate: Category) {
        categoryToUpdate.dirty = false
        dbLoader.updateCategory(categoryToUpdate)
    }

    companion object {
        private val TAG = CategoryRepository::class.java.simpleName
    }

    init {
        Objects.requireNonNull(instance)?.appComponent?.inject(this)
    }
}
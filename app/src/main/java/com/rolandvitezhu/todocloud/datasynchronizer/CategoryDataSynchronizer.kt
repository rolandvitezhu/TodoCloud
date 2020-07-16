package com.rolandvitezhu.todocloud.datasynchronizer

import android.util.Log
import com.rolandvitezhu.todocloud.app.AppController.Companion.instance
import com.rolandvitezhu.todocloud.data.Category
import com.rolandvitezhu.todocloud.datastorage.DbConstants
import com.rolandvitezhu.todocloud.network.ApiService
import com.rolandvitezhu.todocloud.network.api.category.dto.*
import com.rolandvitezhu.todocloud.network.api.rowversion.dto.GetNextRowVersionResponse
import com.rolandvitezhu.todocloud.network.helper.RetrofitResponseHelper.Companion.IsNoError
import com.rolandvitezhu.todocloud.network.helper.RetrofitResponseHelper.Companion.ResponseToJson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryDataSynchronizer @Inject constructor() : BaseDataSynchronizer() {

    private val apiService: ApiService

    private var disposable: CompositeDisposable? = null

    private var onSyncCategoryDataListener: OnSyncCategoryDataListener? = null
    private var isUpdateCategoryRequestsFinished = false
    private var updateCategoryRequestCount = 0
    private var currentUpdateCategoryRequest = 0
    private var isInsertCategoryRequestsFinished = false
    private var insertCategoryRequestCount = 0
    private var currentInsertCategoryRequest = 0

    private var categoriesToUpdate: ArrayList<Category>? = null
    private var categoriesToInsert: ArrayList<Category>? = null

    fun setOnSyncCategoryDataListener(
            onSyncCategoryDataListener: OnSyncCategoryDataListener?
    ) {
        this.onSyncCategoryDataListener = onSyncCategoryDataListener
    }

    fun syncCategoryData(disposable: CompositeDisposable?) {
        this.disposable = disposable
        initCategoryRequestsStates()

        apiService
                .getCategories(dbLoader.lastCategoryRowVersion)
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribeWith(createGetCategoriesDisposableSingleObserver())?.let {
                    this.disposable!!.add(
                            it
                    )
                }
    }

    private fun createGetCategoriesDisposableSingleObserver(): DisposableSingleObserver<GetCategoriesResponse?> {
        return object : DisposableSingleObserver<GetCategoriesResponse?>() {
            override fun onSuccess(getCategoriesResponse: GetCategoriesResponse) {
                Log.d(TAG, "Get Categories Response: $getCategoriesResponse")

                if (getCategoriesResponse != null && getCategoriesResponse.error == "false") {
                    var categories: ArrayList<Category?>? = null

                    categories = getCategoriesResponse.categories

                    if (!categories!!.isEmpty())
                        updateCategoriesInLocalDatabase(categories)

                    val shouldUpdateOrInsertCategories = !categoriesToUpdate!!.isEmpty() || !categoriesToInsert!!.isEmpty()

                    if (shouldUpdateOrInsertCategories)
                        updateOrInsertCategories()
                    else
                        onSyncCategoryDataListener!!.onFinishSyncCategoryData()
                } else if (getCategoriesResponse != null) {
                    // Handle error, if any
                    var message = getCategoriesResponse.getMessage()

                    if (message == null) message = "Unknown error"
                    onSyncCategoryDataListener!!.onSyncError(message)
                }
            }

            override fun onError(throwable: Throwable) {
                Log.d(TAG, "Get Next Row Version Response - onFailure: $throwable")
            }
        }
    }

    private fun updateOrInsertCategories() {
        val call = apiService.getNextRowVersion(
                DbConstants.Category.DATABASE_TABLE, dbLoader.apiKey
        )

        call!!.enqueue(object : Callback<GetNextRowVersionResponse?> {
            override fun onResponse(
                    call: Call<GetNextRowVersionResponse?>,
                    response: Response<GetNextRowVersionResponse?>
            ) {
                Log.d(
                        TAG, "Get Next Row Version Response: "
                        + ResponseToJson(response))

                if (IsNoError(response)) {
                    nextRowVersion = if (response.body() != null) response.body()!!.nextRowVersion!! else 0

                    setRowVersionsForCategories(categoriesToUpdate)
                    setRowVersionsForCategories(categoriesToInsert)

                    updateCategories()
                    insertCategories()
                } else if (response.body() != null) {
                    var message = response.body()!!.getMessage()

                    if (message == null) message = "Unknown error"
                    onSyncCategoryDataListener!!.onSyncError(message)
                }
            }

            override fun onFailure(call: Call<GetNextRowVersionResponse?>, t: Throwable) {
                Log.d(TAG, "Get Next Row Version Response - onFailure: $t")
            }
        })
    }

    private fun initCategoryRequestsStates() {
        isUpdateCategoryRequestsFinished = false
        isInsertCategoryRequestsFinished = false
        categoriesToUpdate = dbLoader.categoriesToUpdate
        categoriesToInsert = dbLoader.categoriesToInsert
        nextRowVersion = 0
    }

    private fun setRowVersionsForCategories(categories: ArrayList<Category>?) {
        for (category in categories!!) {
            category.rowVersion = nextRowVersion++
        }
    }

    private fun updateCategories() {
        // Process list
        if (!categoriesToUpdate!!.isEmpty()) {
            updateCategoryRequestCount = categoriesToUpdate!!.size
            currentUpdateCategoryRequest = 1

            // Process list item
            for (categoryToUpdate in categoriesToUpdate!!) {
                val updateCategoryRequest = UpdateCategoryRequest()

                updateCategoryRequest.categoryOnlineId = categoryToUpdate.categoryOnlineId
                updateCategoryRequest.title = categoryToUpdate.title
                updateCategoryRequest.rowVersion = categoryToUpdate.rowVersion
                updateCategoryRequest.deleted = categoryToUpdate.deleted
                updateCategoryRequest.position = categoryToUpdate.position

                apiService
                        .updateCategory(updateCategoryRequest)
                        ?.subscribeOn(Schedulers.io())
                        ?.observeOn(AndroidSchedulers.mainThread())
                        ?.subscribeWith(createUpdateCategoryDisposableSingleObserver(categoryToUpdate))?.let {
                            disposable!!.add(
                                    it
                        )
                        }
            }
            // Sync finished - there are no list items
        } else {
            isUpdateCategoryRequestsFinished = true
            if (isAllCategoryRequestsFinished) {
                onSyncCategoryDataListener!!.onFinishSyncCategoryData()
            }
        }
    }

    private fun createUpdateCategoryDisposableSingleObserver(categoryToUpdate: Category):
            DisposableSingleObserver<UpdateCategoryResponse?> {
        return object : DisposableSingleObserver<UpdateCategoryResponse?>() {
            override fun onSuccess(updateCategoryResponse: UpdateCategoryResponse) {
                Log.d(TAG, "Update Category Response: $updateCategoryResponse")

                if (updateCategoryResponse != null && updateCategoryResponse.error == "false") {
                    makeCategoryUpToDate(categoryToUpdate)
                    if (isLastUpdateCategoryRequest) {
                        isUpdateCategoryRequestsFinished = true
                        if (isAllCategoryRequestsFinished) {
                            onSyncCategoryDataListener!!.onFinishSyncCategoryData()
                        }
                    }
                } else if (updateCategoryResponse != null) {
                    var message = updateCategoryResponse.getMessage()

                    if (message == null) message = "Unknown error"
                    onSyncCategoryDataListener!!.onSyncError(message)

                    if (isLastUpdateCategoryRequest) {
                        isUpdateCategoryRequestsFinished = true
                        if (isAllCategoryRequestsFinished) {
                            onSyncCategoryDataListener!!.onFinishSyncCategoryData()
                        }
                    }
                }
            }

            override fun onError(throwable: Throwable) {
                Log.d(TAG, "Update Category Response - onFailure: $throwable")

                onSyncCategoryDataListener!!.onSyncError(throwable.toString())

                if (isLastUpdateCategoryRequest) {
                    isUpdateCategoryRequestsFinished = true
                    if (isAllCategoryRequestsFinished) {
                        onSyncCategoryDataListener!!.onFinishSyncCategoryData()
                    }
                }
            }
        }
    }

    private fun insertCategories() {
        // Process list
        if (!categoriesToInsert!!.isEmpty()) {
            insertCategoryRequestCount = categoriesToInsert!!.size
            currentInsertCategoryRequest = 1

            // Process list item
            for (categoryToInsert in categoriesToInsert!!) {
                val insertCategoryRequest = InsertCategoryRequest()

                insertCategoryRequest.categoryOnlineId = categoryToInsert.categoryOnlineId
                insertCategoryRequest.title = categoryToInsert.title
                insertCategoryRequest.rowVersion = categoryToInsert.rowVersion
                insertCategoryRequest.deleted = categoryToInsert.deleted
                insertCategoryRequest.position = categoryToInsert.position

                apiService
                        .insertCategory(insertCategoryRequest)
                        ?.subscribeOn(Schedulers.io())
                        ?.observeOn(AndroidSchedulers.mainThread())
                        ?.subscribeWith(createInsertCategoryDisposableSingleObserver(categoryToInsert))?.let {
                            disposable!!.add(
                                    it
                        )
                        }
            }
            // Sync finished - there are no list items
        } else {
            isInsertCategoryRequestsFinished = true
            if (isAllCategoryRequestsFinished) {
                onSyncCategoryDataListener!!.onFinishSyncCategoryData()
            }
        }
    }

    private fun createInsertCategoryDisposableSingleObserver(categoryToInsert: Category):
            DisposableSingleObserver<InsertCategoryResponse?> {
        return object : DisposableSingleObserver<InsertCategoryResponse?>() {
            override fun onSuccess(insertCategoryResponse: InsertCategoryResponse) {
                Log.d(TAG, "Insert Category Response: $insertCategoryResponse")

                if (insertCategoryResponse != null && insertCategoryResponse.error == "false") {
                    makeCategoryUpToDate(categoryToInsert)
                    if (isLastInsertCategoryRequest) {
                        isInsertCategoryRequestsFinished = true
                        if (isAllCategoryRequestsFinished) {
                            onSyncCategoryDataListener!!.onFinishSyncCategoryData()
                        }
                    }
                } else if (insertCategoryResponse != null) {
                    var message = insertCategoryResponse.getMessage()

                    if (message == null) message = "Unknown error"
                    onSyncCategoryDataListener!!.onSyncError(message)

                    if (isLastInsertCategoryRequest) {
                        isInsertCategoryRequestsFinished = true
                        if (isAllCategoryRequestsFinished) {
                            onSyncCategoryDataListener!!.onFinishSyncCategoryData()
                        }
                    }
                }
            }

            override fun onError(throwable: Throwable) {
                Log.d(TAG, "Insert Category Response - onFailure: $throwable")

                onSyncCategoryDataListener!!.onSyncError(throwable.toString())

                if (isLastInsertCategoryRequest) {
                    isInsertCategoryRequestsFinished = true
                    if (isAllCategoryRequestsFinished) {
                        onSyncCategoryDataListener!!.onFinishSyncCategoryData()
                    }
                }
            }
        }
    }

    private fun updateCategoriesInLocalDatabase(categories: ArrayList<Category?>?) {
        for (category in categories!!) {
            val exists = dbLoader.isCategoryExists(category!!.categoryOnlineId)
            if (!exists) {
                dbLoader.createCategory(category)
            } else {
                dbLoader.updateCategory(category)
            }
        }
    }

    private fun makeCategoryUpToDate(categoryToUpdate: Category) {
        categoryToUpdate.dirty = false
        dbLoader.updateCategory(categoryToUpdate)
    }

    private val isLastInsertCategoryRequest: Boolean
        private get() = if (currentInsertCategoryRequest++ == insertCategoryRequestCount) {
            true
        } else {
            false
        }

    private val isLastUpdateCategoryRequest: Boolean
        private get() = if (currentUpdateCategoryRequest++ == updateCategoryRequestCount) {
            true
        } else {
            false
        }

    private val isAllCategoryRequestsFinished: Boolean
        private get() = isUpdateCategoryRequestsFinished && isInsertCategoryRequestsFinished

    interface OnSyncCategoryDataListener {
        fun onFinishSyncCategoryData()
        fun onSyncError(errorMessage: String?)
    }

    companion object {
        private val TAG = CategoryDataSynchronizer::class.java.simpleName
    }

    init {
        Objects.requireNonNull(instance)?.appComponent?.inject(this)
        apiService = retrofit.create(ApiService::class.java)
    }
}
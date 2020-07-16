package com.rolandvitezhu.todocloud.datasynchronizer

import android.util.Log
import com.rolandvitezhu.todocloud.app.AppController.Companion.instance
import com.rolandvitezhu.todocloud.data.List
import com.rolandvitezhu.todocloud.datastorage.DbConstants
import com.rolandvitezhu.todocloud.network.ApiService
import com.rolandvitezhu.todocloud.network.api.list.dto.*
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
class ListDataSynchronizer @Inject constructor() : BaseDataSynchronizer() {

    private val apiService: ApiService

    private var disposable: CompositeDisposable? = null

    private var onSyncListDataListener: OnSyncListDataListener? = null
    private var isUpdateListRequestsFinished = false
    private var updateListRequestCount = 0
    private var currentUpdateListRequest = 0
    private var isInsertListRequestsFinished = false
    private var insertListRequestCount = 0
    private var currentInsertListRequest = 0

    private var listsToUpdate: ArrayList<List>? = null
    private var listsToInsert: ArrayList<List>? = null

    fun setOnSyncListDataListener(onSyncListDataListener: OnSyncListDataListener?) {
        this.onSyncListDataListener = onSyncListDataListener
    }

    fun syncListData(disposable: CompositeDisposable?) {
        this.disposable = disposable
        initListRequestsStates()

        apiService
                .getLists(dbLoader.lastListRowVersion)
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribeWith(createGetListsDisposableSingleObserver())?.let {
                    this.disposable!!.add(
                            it
                    )
                }
    }

    private fun createGetListsDisposableSingleObserver(): DisposableSingleObserver<GetListsResponse?> {
        return object : DisposableSingleObserver<GetListsResponse?>() {

            override fun onSuccess(getListsResponse: GetListsResponse) {
                Log.d(TAG, "Get Lists Response: $getListsResponse")

                if (getListsResponse != null && getListsResponse.error == "false") {
                    var lists: ArrayList<List?>? = null

                    lists = getListsResponse.lists

                    if (!lists!!.isEmpty())
                        updateListsInLocalDatabase(lists)

                    val shouldUpdateOrInsertLists = !listsToUpdate!!.isEmpty() || !listsToInsert!!.isEmpty()

                    if (shouldUpdateOrInsertLists)
                        updateOrInsertLists()
                    else
                        onSyncListDataListener!!.onFinishSyncListData()
                } else if (getListsResponse != null) {
                    // Handle error, if any
                    var message = getListsResponse.getMessage()

                    if (message == null) message = "Unknown error"
                    onSyncListDataListener!!.onSyncError(message)
                }
            }

            override fun onError(throwable: Throwable) {
                Log.d(TAG, "Get Next Row Version Response - onFailure: $throwable")
            }
        }
    }

    private fun updateOrInsertLists() {
        val call = apiService.getNextRowVersion(
                DbConstants.List.DATABASE_TABLE, dbLoader.apiKey
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

                    setRowVersionsForLists(listsToUpdate)
                    setRowVersionsForLists(listsToInsert)

                    updateLists()
                    insertLists()
                } else if (response.body() != null) {
                    var message = response.body()!!.getMessage()

                    if (message == null) message = "Unknown error"
                    onSyncListDataListener!!.onSyncError(message)
                }
            }

            override fun onFailure(call: Call<GetNextRowVersionResponse?>, t: Throwable) {
                Log.d(TAG, "Get Next Row Version Response - onFailure: $t")
            }
        })
    }

    private fun updateListsInLocalDatabase(lists: ArrayList<List?>?) {
        for (list in lists!!) {
            val exists = dbLoader.isListExists(list!!.listOnlineId)
            if (!exists) {
                dbLoader.createList(list)
            } else {
                dbLoader.updateList(list)
            }
        }
    }

    private fun initListRequestsStates() {
        isUpdateListRequestsFinished = false
        isInsertListRequestsFinished = false
        listsToUpdate = dbLoader.listsToUpdate
        listsToInsert = dbLoader.listsToInsert
        nextRowVersion = 0
    }

    private fun setRowVersionsForLists(lists: ArrayList<List>?) {
        for (list in lists!!) {
            list.rowVersion = nextRowVersion++
        }
    }

    private fun updateLists() {
        // Process list
        if (!listsToUpdate!!.isEmpty()) {
            updateListRequestCount = listsToUpdate!!.size
            currentUpdateListRequest = 1

            // Process list item
            for (listToUpdate in listsToUpdate!!) {
                val updateListRequest = UpdateListRequest()

                updateListRequest.listOnlineId = listToUpdate.listOnlineId
                updateListRequest.categoryOnlineId = listToUpdate.categoryOnlineId
                updateListRequest.title = listToUpdate.title
                updateListRequest.rowVersion = listToUpdate.rowVersion
                updateListRequest.deleted = listToUpdate.deleted
                updateListRequest.position = listToUpdate.position

                apiService
                        .updateList(updateListRequest)
                        ?.subscribeOn(Schedulers.io())
                        ?.observeOn(AndroidSchedulers.mainThread())
                        ?.subscribeWith(createUpdateListDisposableSingleObserver(listToUpdate))?.let {
                            disposable!!.add(
                                    it
                        )
                        }
            }
            // Sync finished - there are no list items
        } else {
            isUpdateListRequestsFinished = true
            if (isAllListRequestsFinished) {
                onSyncListDataListener!!.onFinishSyncListData()
            }
        }
    }

    private fun createUpdateListDisposableSingleObserver(listToUpdate: List):
            DisposableSingleObserver<UpdateListResponse?> {
        return object : DisposableSingleObserver<UpdateListResponse?>() {

            override fun onSuccess(updateListResponse: UpdateListResponse) {
                Log.d(TAG, "Update List Response: $updateListResponse")

                if (updateListResponse != null && updateListResponse.error == "false") {
                    makeListUpToDate(listToUpdate)
                    if (isLastUpdateListRequest) {
                        isUpdateListRequestsFinished = true
                        if (isAllListRequestsFinished) {
                            onSyncListDataListener!!.onFinishSyncListData()
                        }
                    }
                } else if (updateListResponse != null) {
                    var message = updateListResponse.getMessage()

                    if (message == null) message = "Unknown error"
                    onSyncListDataListener!!.onSyncError(message)

                    if (isLastUpdateListRequest) {
                        isUpdateListRequestsFinished = true
                        if (isAllListRequestsFinished) {
                            onSyncListDataListener!!.onFinishSyncListData()
                        }
                    }
                }
            }

            override fun onError(throwable: Throwable) {
                Log.d(TAG, "Update List Response - onFailure: $throwable")

                onSyncListDataListener!!.onSyncError(throwable.toString())

                if (isLastUpdateListRequest) {
                    isUpdateListRequestsFinished = true
                    if (isAllListRequestsFinished) {
                        onSyncListDataListener!!.onFinishSyncListData()
                    }
                }
            }
        }
    }

    private fun makeListUpToDate(listToUpdate: List) {
        listToUpdate.dirty = false
        dbLoader.updateList(listToUpdate)
    }

    private fun insertLists() {
        // Process list
        if (!listsToInsert!!.isEmpty()) {
            insertListRequestCount = listsToInsert!!.size
            currentInsertListRequest = 1

            // Process list item
            for (listToInsert in listsToInsert!!) {
                val insertListRequest = InsertListRequest()

                insertListRequest.listOnlineId = listToInsert.listOnlineId
                insertListRequest.categoryOnlineId = listToInsert.categoryOnlineId
                insertListRequest.title = listToInsert.title
                insertListRequest.rowVersion = listToInsert.rowVersion
                insertListRequest.deleted = listToInsert.deleted
                insertListRequest.position = listToInsert.position

                apiService
                        .insertList(insertListRequest)
                        ?.subscribeOn(Schedulers.io())
                        ?.observeOn(AndroidSchedulers.mainThread())
                        ?.subscribeWith(createInsertListDisposableSingleObserver(listToInsert))?.let {
                            disposable!!.add(
                                    it
                        )
                        }
            }
            // Sync finished - there are no list items
        } else {
            isInsertListRequestsFinished = true
            if (isAllListRequestsFinished) {
                onSyncListDataListener!!.onFinishSyncListData()
            }
        }
    }

    private fun createInsertListDisposableSingleObserver(listToInsert: List):
            DisposableSingleObserver<InsertListResponse?> {
        return object : DisposableSingleObserver<InsertListResponse?>() {
            override fun onSuccess(insertListResponse: InsertListResponse) {
                Log.d(TAG, "Insert List Response: $insertListResponse")

                if (insertListResponse != null && insertListResponse.error == "false") {
                    makeListUpToDate(listToInsert)
                    if (isLastInsertListRequest) {
                        isInsertListRequestsFinished = true
                        if (isAllListRequestsFinished) {
                            onSyncListDataListener!!.onFinishSyncListData()
                        }
                    }
                } else if (insertListResponse != null) {
                    var message = insertListResponse.getMessage()

                    if (message == null) message = "Unknown error"
                    onSyncListDataListener!!.onSyncError(message)

                    if (isLastInsertListRequest) {
                        isInsertListRequestsFinished = true
                        if (isAllListRequestsFinished) {
                            onSyncListDataListener!!.onFinishSyncListData()
                        }
                    }
                }
            }

            override fun onError(throwable: Throwable) {
                Log.d(TAG, "Insert List Response - onFailure: $throwable")

                onSyncListDataListener!!.onSyncError(throwable.toString())

                if (isLastInsertListRequest) {
                    isInsertListRequestsFinished = true
                    if (isAllListRequestsFinished) {
                        onSyncListDataListener!!.onFinishSyncListData()
                    }
                }
            }
        }
    }

    private val isLastInsertListRequest: Boolean
        private get() = if (currentInsertListRequest++ == insertListRequestCount) {
            true
        } else {
            false
        }

    private val isLastUpdateListRequest: Boolean
        private get() = if (currentUpdateListRequest++ == updateListRequestCount) {
            true
        } else {
            false
        }

    private val isAllListRequestsFinished: Boolean
        private get() = isUpdateListRequestsFinished && isInsertListRequestsFinished

    interface OnSyncListDataListener {
        fun onFinishSyncListData()
        fun onSyncError(errorMessage: String?)
    }

    companion object {
        private val TAG = ListDataSynchronizer::class.java.simpleName
    }

    init {
        Objects.requireNonNull(instance)?.appComponent?.inject(this)
        apiService = retrofit.create(ApiService::class.java)
    }
}
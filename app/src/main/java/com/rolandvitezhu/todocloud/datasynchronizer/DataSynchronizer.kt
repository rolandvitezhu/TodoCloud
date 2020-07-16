package com.rolandvitezhu.todocloud.datasynchronizer

import com.rolandvitezhu.todocloud.app.AppController.Companion.instance
import com.rolandvitezhu.todocloud.datasynchronizer.CategoryDataSynchronizer.OnSyncCategoryDataListener
import com.rolandvitezhu.todocloud.datasynchronizer.ListDataSynchronizer.OnSyncListDataListener
import com.rolandvitezhu.todocloud.datasynchronizer.TodoDataSynchronizer.OnSyncTodoDataListener
import io.reactivex.disposables.CompositeDisposable
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSynchronizer @Inject constructor() : OnSyncTodoDataListener, OnSyncListDataListener, OnSyncCategoryDataListener {

    @Inject
    lateinit var todoDataSynchronizer: TodoDataSynchronizer

    @Inject
    lateinit var listDataSynchronizer: ListDataSynchronizer

    @Inject
    lateinit var categoryDataSynchronizer: CategoryDataSynchronizer

    private var onSyncDataListener: OnSyncDataListener? = null

    private var isLastTodoRequestProcessed = false
    private var isLastListRequestProcessed = false
    private var isLastCategoryRequestProcessed = false

    fun setOnSyncDataListener(onSyncDataListener: OnSyncDataListener?) {
        this.onSyncDataListener = onSyncDataListener
    }

    /**
     * Call update and insert methods only, if get requests processed successfully. Otherwise the
     * client will have data in the local database with the biggest current row_version before it
     * get all of the data from the remote database, which is missing in the local database. Hence
     * it don't will get that data.
     * If an error occurs in the processing of the requests, they should be aborted and start the
     * whole processing from the beginning, with the call of get methods.
     */
    fun syncData(disposable: CompositeDisposable?) {
        initializeSyncStates()
        todoDataSynchronizer!!.syncTodoData(disposable)
        listDataSynchronizer!!.syncListData(disposable)
        categoryDataSynchronizer!!.syncCategoryData(disposable)
    }

    private fun initializeSyncStates() {
        isLastTodoRequestProcessed = false
        isLastListRequestProcessed = false
        isLastCategoryRequestProcessed = false
    }

    private val isSynchronizationCompleted: Boolean
        private get() = (isLastTodoRequestProcessed
                && isLastListRequestProcessed
                && isLastCategoryRequestProcessed)

    override fun onFinishSyncTodoData() {
        isLastTodoRequestProcessed = true
        if (isSynchronizationCompleted) onSyncDataListener!!.onFinishSyncData()
    }

    override fun onFinishSyncListData() {
        onSyncDataListener!!.onFinishSyncListData()
        isLastListRequestProcessed = true
        if (isSynchronizationCompleted) onSyncDataListener!!.onFinishSyncData()
    }

    override fun onFinishSyncCategoryData() {
        onSyncDataListener!!.onFinishSyncCategoryData()
        isLastCategoryRequestProcessed = true
        if (isSynchronizationCompleted) onSyncDataListener!!.onFinishSyncData()
    }

    override fun onSyncError(errorMessage: String?) {
        onSyncDataListener!!.onSyncError(errorMessage)
        onSyncDataListener!!.onFinishSyncData()
    }

    interface OnSyncDataListener {
        fun onFinishSyncListData()
        fun onFinishSyncCategoryData()
        fun onFinishSyncData()
        fun onSyncError(errorMessage: String?)
    }

    init {
        Objects.requireNonNull(instance)?.appComponent?.inject(this)
        todoDataSynchronizer!!.setOnSyncTodoDataListener(this)
        listDataSynchronizer!!.setOnSyncListDataListener(this)
        categoryDataSynchronizer!!.setOnSyncCategoryDataListener(this)
    }
}
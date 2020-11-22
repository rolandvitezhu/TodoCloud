package com.rolandvitezhu.todocloud.ui.activity.main

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController
import com.rolandvitezhu.todocloud.app.AppController.Companion.showWhiteTextSnackbar
import com.rolandvitezhu.todocloud.data.List
import com.rolandvitezhu.todocloud.data.PredefinedList
import com.rolandvitezhu.todocloud.data.Todo
import com.rolandvitezhu.todocloud.database.TodoCloudDatabase
import com.rolandvitezhu.todocloud.database.TodoCloudDatabaseDao
import com.rolandvitezhu.todocloud.databinding.ActivityMainBinding
import com.rolandvitezhu.todocloud.databinding.NavigationdrawerHeaderBinding
import com.rolandvitezhu.todocloud.helper.SessionManager
import com.rolandvitezhu.todocloud.helper.hideSoftInput
import com.rolandvitezhu.todocloud.receiver.ReminderSetter
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.LogoutUserDialogFragment
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.*
import com.rolandvitezhu.todocloud.ui.activity.main.preferencefragment.SettingsPreferenceFragment
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.ListsViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.PredefinedListsViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.TodosViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.UserViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.layout_appbar.*
import kotlinx.android.synthetic.main.layout_appbar.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainActivity : AppCompatActivity(), FragmentManager.OnBackStackChangedListener {

    @Inject
    lateinit var todoCloudDatabaseDao: TodoCloudDatabaseDao
    @Inject
    lateinit var todoCloudDatabase: TodoCloudDatabase
    @Inject
    lateinit var sessionManager: SessionManager

    private var actionBarDrawerToggle: ActionBarDrawerToggle? = null

    private val userViewModel by lazy {
        ViewModelProvider(this).get(UserViewModel::class.java)
    }
    private val todosViewModel by lazy {
        ViewModelProvider(this).get(TodosViewModel::class.java)
    }
    private val predefinedListsViewModel by lazy {
        ViewModelProvider(this).get(PredefinedListsViewModel::class.java)
    }
    private val listsViewModel by lazy {
        ViewModelProvider(this).get(ListsViewModel::class.java)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        (application as AppController).appComponent.inject(this)
        super.onCreate(savedInstanceState)

        val activityMainBinding: ActivityMainBinding =
                DataBindingUtil.setContentView(this, R.layout.activity_main)
        val navigationdrawerHeaderViewBinding: NavigationdrawerHeaderBinding =
                NavigationdrawerHeaderBinding.inflate(
                        layoutInflater,
                        activityMainBinding.mainlistNavigationview,
                        false)
        activityMainBinding.mainlistNavigationview.addHeaderView(navigationdrawerHeaderViewBinding.root)
        val view: View = activityMainBinding.root

        activityMainBinding.mainActivity = this
        activityMainBinding.executePendingBindings()

        setSupportActionBar(view.toolbar_main)
        prepareNavigationView(view)

        navigationdrawerHeaderViewBinding.lifecycleOwner = this  // Set it, so as we update
        // the data in the userViewModel, the UI will update automatically.
        navigationdrawerHeaderViewBinding.userViewModel = userViewModel
        navigationdrawerHeaderViewBinding.executePendingBindings()

        if (view.framelayout_main != null) {
            if (savedInstanceState != null) {
                // Prevent Fragment overlapping
                return
            }
            if (sessionManager.isLoggedIn) {
                val id = intent.getLongExtra("id", -1)
                val wasMainActivityStartedFromLauncherIcon = id == -1L
                if (wasMainActivityStartedFromLauncherIcon) {
                    openMainListFragment(view)
                } else if (wasMainActivityStartedFromNotification(id)) {
                    lifecycleScope.launch {
                        todosViewModel.todo = getNotificationRelatedTodo(id)
                    }
                    openMainListFragment(view)
                    val todoListFragment = TodoListFragment()
                    openAllPredefinedList(todoListFragment, view)
                    openModifyTodoFragment(todoListFragment, view)
                }
            } else {
                openLoginUserFragment(view)
            }
            prepareActionBarNavigationHandler()
            shouldDisplayHomeAsUp(view)
        }
        ReminderSetter.createReminderServices(applicationContext)
    }

    private fun wasMainActivityStartedFromNotification(id: Long): Boolean {
        return id != -1L
    }

    private fun openAllPredefinedList(todoListFragment: TodoListFragment, view: View) {
        lifecycleScope.launch {
            val allPredefinedListWhere = todoCloudDatabaseDao.prepareAllPredefinedListWhere()
            val predefinedList = PredefinedList(getString(R.string.all_all), allPredefinedListWhere)
            predefinedListsViewModel.predefinedList = predefinedList
            todosViewModel.updateTodosViewModelByWhereCondition(
                    predefinedList.title,
                    predefinedList.selectFromDB
            )
        }
        openTodoListFragment(todoListFragment, view)
    }

    private suspend fun getNotificationRelatedTodo(id: Long): Todo {
        return todoCloudDatabaseDao.getTodo(id)?: Todo()
    }

    private fun prepareActionBarNavigationHandler() {
        val fragmentManager = supportFragmentManager
        fragmentManager.addOnBackStackChangedListener(this)
    }

    private fun shouldDisplayHomeAsUp(view: View?) {
        val fragmentManager = supportFragmentManager
        val shouldDisplay = fragmentManager.backStackEntryCount > 0
        val actionBar = supportActionBar
        if (actionBar != null) {
            if (shouldDisplay) {
                setDrawerEnabled(false, view)
                actionBar.setDisplayHomeAsUpEnabled(true)
            } else {
                actionBar.setDisplayHomeAsUpEnabled(false)
                setDrawerEnabled(true, view)
            }
            val actionBarTitle = actionBar.title
            if (isMainListFragment(shouldDisplay, actionBarTitle)) {
                actionBar.setTitle(R.string.app_name)
                actionBar.setDisplayHomeAsUpEnabled(false)
                setDrawerEnabled(true, view)
            }
        }
    }

    private fun isMainListFragment(shouldDisplay: Boolean, actionBarTitle: CharSequence?): Boolean {
        return !shouldDisplay && actionBarTitle != null && actionBarTitle != getString(R.string.all_login)
    }

    private fun setDrawerEnabled(enabled: Boolean, view: View?) {
        if (!enabled) {
            disableDrawer(view)
            enableActionBarBackNavigation()
        } else {
            enableDrawer(view)
        }
    }

    private fun enableActionBarBackNavigation() {
        actionBarDrawerToggle?.toolbarNavigationClickListener = View.OnClickListener {
            onBackPressed()
            hideSoftInput()
        }
    }

    private fun enableDrawer(view: View?) {
        if (view != null)
            view.mainlist_drawerlayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        else
            this.mainlist_drawerlayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        actionBarDrawerToggle?.isDrawerIndicatorEnabled = true
        actionBarDrawerToggle?.syncState()
    }

    private fun disableDrawer(view: View?) {
        if (view != null)
            view.mainlist_drawerlayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        else
            this.mainlist_drawerlayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        actionBarDrawerToggle?.isDrawerIndicatorEnabled = false
        actionBarDrawerToggle?.syncState()
    }

    private fun prepareNavigationView(view: View) {
        view.mainlist_navigationview?.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menuitem_navigationdrawer_settings -> openSettingsPreferenceFragment(view)
                R.id.menuitem_navigationdrawer_logout -> openLogoutUserDialogFragment()
            }
            view.mainlist_drawerlayout?.closeDrawers()
            true
        }
        actionBarDrawerToggle = ActionBarDrawerToggle(
                this,
                view.mainlist_drawerlayout,
                view.toolbar_main,
                R.string.actionbardrawertoggle_opendrawer,
                R.string.actionbardrawertoggle_closedrawer
        )
        actionBarDrawerToggle?.let { view.mainlist_drawerlayout?.addDrawerListener(it) }
        actionBarDrawerToggle?.syncState()
    }

    private fun openLogoutUserDialogFragment() {
        val logoutUserDialogFragment = LogoutUserDialogFragment()
        logoutUserDialogFragment.show(supportFragmentManager, "LogoutUserDialogFragment")
    }

    fun onPrepareNavigationHeader() {
        lifecycleScope.launch {
            userViewModel.updateUserViewModel()
        }
    }

    fun onSearchActionItemClick() {
        openSearchFragment()
    }

    private fun openSearchFragment() {
        todosViewModel.todos.value?.clear()
        val searchFragment = SearchFragment()
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(this.framelayout_main!!.id, searchFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        when (itemId) {
            android.R.id.home -> {
                hideSoftInput()
                onBackPressed()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val fragmentManager = supportFragmentManager
        val modifyTodoFragment = fragmentManager.findFragmentByTag(
                "ModifyTodoFragment"
        ) as ModifyTodoFragment?
        if (modifyTodoFragment != null && todosViewModel.shouldNavigateBack) {
            super.onBackPressed()
        } else if (modifyTodoFragment != null) {
            modifyTodoFragment.handleModifyTodo()
        } else if (fragmentManager.findFragmentByTag("RegisterUserFragment") != null) {
            navigateBackToLoginUserFragment()
        } else if (fragmentManager.findFragmentByTag("ResetPasswordFragment") != null) {
            navigateBackToLoginUserFragment()
        } else if (this.mainlist_drawerlayout?.isDrawerOpen(GravityCompat.START) == true) {
            this.mainlist_drawerlayout?.closeDrawers()
        } else {
            super.onBackPressed()
        }
    }

    private fun navigateBackToLoginUserFragment() {
        super.onBackPressed()
        onSetActionBarTitle(getString(R.string.all_login))
    }

    fun onClickPredefinedList(predefinedList: PredefinedList) {
        lifecycleScope.launch {
            todosViewModel.updateTodosViewModelByWhereCondition(
                    predefinedList.title,
                    predefinedList.selectFromDB
            )
        }
        openTodoListFragment()
    }

    fun onClickList(list: List) {
        lifecycleScope.launch {
            list.listOnlineId?.let {
                todosViewModel.updateTodosViewModelByListOnlineId(
                        list.title,
                        it
                )
            }
        }
        openTodoListFragment()
    }

    private fun openTodoListFragment() {
        val todoListFragment = TodoListFragment()
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(this.framelayout_main!!.id, todoListFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    private fun openTodoListFragment(todoListFragment: TodoListFragment, view: View) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(view.framelayout_main!!.id, todoListFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    fun onLogout() {
        cancelReminders()
        sessionManager.setLogin(false)
        val fragmentManager = supportFragmentManager
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        openLoginUserFragment(null)
        GlobalScope.launch(Dispatchers.IO) { todoCloudDatabase.clearAllTables() }
    }

    private fun cancelReminders() {
        lifecycleScope.launch {
            val todosWithReminder = todoCloudDatabaseDao.getTodosWithReminder()
            for (todoWithReminder in todosWithReminder) {
                ReminderSetter.cancelReminderService(todoWithReminder)
            }
        }
    }

    fun onClickLinkToRegisterUser() {
        openRegisterUserFragment()
    }

    fun onClickLinkToResetPassword() {
        openResetPasswordFragment()
    }

    private fun openResetPasswordFragment() {
        val resetPasswordFragment = ResetPasswordFragment()
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(
                this.framelayout_main!!.id,
                resetPasswordFragment,
                "ResetPasswordFragment"
        )
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    private fun openRegisterUserFragment() {
        val registerUserFragment = RegisterUserFragment()
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(
                this.framelayout_main!!.id,
                registerUserFragment,
                "RegisterUserFragment"
        )
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    fun onFinishRegisterUser() {
        val fragmentManager = supportFragmentManager
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        openLoginUserFragment(null)
        onSetActionBarTitle(getString(R.string.all_login))
    }

    private fun openLoginUserFragment(view: View?) {
        val loginUserFragment = LoginUserFragment()
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        if (view != null)
            fragmentTransaction.replace(view.framelayout_main!!.id, loginUserFragment)
        else
            fragmentTransaction.replace(this.framelayout_main!!.id, loginUserFragment)
        fragmentTransaction.commit()
    }

    fun onFinishLoginUser() {
        val fragmentManager = supportFragmentManager
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        openMainListFragment(null)
        onSetActionBarTitle(getString(R.string.app_name))
    }

    private fun openMainListFragment(view: View?) {
        val mainListFragment = MainListFragment()
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        if (view != null)
            fragmentTransaction.replace(view.framelayout_main!!.id, mainListFragment)
        else
            fragmentTransaction.replace(this.framelayout_main!!.id, mainListFragment)
        fragmentTransaction.commit()
    }

    fun onClickChangePassword() {
        openModifyPasswordFragment()
    }

    private fun openModifyPasswordFragment() {
        val modifyPasswordFragment = ModifyPasswordFragment()
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(
                this.framelayout_main!!.id,
                modifyPasswordFragment,
                "ModifyPasswordFragment"
        )
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    override fun onBackStackChanged() {
        shouldDisplayHomeAsUp(null)
    }

    override fun onSupportNavigateUp(): Boolean {
        supportFragmentManager.popBackStack()
        return true
    }

    fun onFinishModifyPassword() {
        try {
            val snackbar = Snackbar.make(
                    this.main_coordinator_layout!!,
                    R.string.modifypassword_passwordchangedsuccessfully,
                    Snackbar.LENGTH_LONG
            )
            showWhiteTextSnackbar(snackbar)
        } catch (e: NullPointerException) {
            // Snackbar or coordinatorLayout doesn't exists already.
        }
    }

    fun onFinishResetPassword() {
        try {
            val snackbar = Snackbar.make(
                    this.main_coordinator_layout!!,
                    R.string.resetpassword_passwordresetsuccessful,
                    Snackbar.LENGTH_LONG
            )
            showWhiteTextSnackbar(snackbar)
        } catch (e: NullPointerException) {
            // Snackbar or coordinatorLayout doesn't exists already.
        }
    }

    fun onSetActionBarTitle(title: String?) {
        supportActionBar?.title = title
        setDrawerEnabled(title == "Todo Cloud", null)
    }

    fun onStartActionMode(callback: ActionMode.Callback?) {
        callback?.let { startSupportActionMode(it) }
    }

    fun openModifyTodoFragment(targetFragment: Fragment?, view: View?) {
        val modifyTodoFragment = ModifyTodoFragment()
        modifyTodoFragment.setTargetFragment(targetFragment, 0)

        val fragmentTransaction = supportFragmentManager.beginTransaction()
        if (view != null)
            fragmentTransaction.replace(
                    view.framelayout_main.id,
                    modifyTodoFragment,
                    "ModifyTodoFragment"
            )
        else
            fragmentTransaction.replace(
                    this.framelayout_main.id,
                    modifyTodoFragment,
                    "ModifyTodoFragment"
            )
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    fun onOpenCreateTodoFragment(targetFragment: TodoListFragment?) {
        val createTodoFragment = CreateTodoFragment()
        createTodoFragment.setTargetFragment(targetFragment, 0)

        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(this.framelayout_main.id, createTodoFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    fun openSettingsPreferenceFragment(view: View) {
        val settingsPreferenceFragment = SettingsPreferenceFragment()

        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(view.framelayout_main.id, settingsPreferenceFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    fun onModifyTodo() {
        lifecycleScope.launch {
            todoCloudDatabaseDao.updateTodo(todosViewModel.todo)
            todoCloudDatabaseDao.fixTodoPositions()
            todosViewModel.updateTodosViewModel()
        }

        if (isSetReminder(todosViewModel.todo)) {
            if (shouldCreateReminderService(todosViewModel.todo)) {
                ReminderSetter.createReminderService(todosViewModel.todo)
            }
        } else {
            ReminderSetter.cancelReminderService(todosViewModel.todo)
        }
    }

    private fun isSetReminder(todo: Todo): Boolean {
        return !todo.reminderDateTime?.equals("-1")!!
    }

    private fun shouldCreateReminderService(todoToModify: Todo): Boolean {
        return isNotCompleted(todoToModify) && isNotDeleted(todoToModify)
    }

    private fun isNotCompleted(todo: Todo): Boolean {
        return todo.completed?.not() ?: true
    }

    private fun isNotDeleted(todo: Todo): Boolean {
        return todo.deleted?.not() ?: true
    }
}
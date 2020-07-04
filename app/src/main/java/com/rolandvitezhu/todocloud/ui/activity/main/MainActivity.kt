package com.rolandvitezhu.todocloud.ui.activity.main

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProviders
import butterknife.BindView
import butterknife.ButterKnife
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController
import com.rolandvitezhu.todocloud.app.AppController.Companion.showWhiteTextSnackbar
import com.rolandvitezhu.todocloud.data.List
import com.rolandvitezhu.todocloud.data.PredefinedList
import com.rolandvitezhu.todocloud.data.Todo
import com.rolandvitezhu.todocloud.datastorage.DbConstants
import com.rolandvitezhu.todocloud.datastorage.DbLoader
import com.rolandvitezhu.todocloud.datastorage.asynctask.UpdateViewModelTask
import com.rolandvitezhu.todocloud.helper.OnlineIdGenerator
import com.rolandvitezhu.todocloud.helper.SessionManager
import com.rolandvitezhu.todocloud.receiver.ReminderSetter
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.LogoutUserDialogFragment
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.*
import com.rolandvitezhu.todocloud.ui.activity.main.preferencefragment.SettingsPreferenceFragment
import com.rolandvitezhu.todocloud.ui.activity.main.viewholder.NavigationHeaderViewHolder
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.ListsViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.PredefinedListsViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.TodosViewModel
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.UserViewModel
import javax.inject.Inject

class MainActivity : AppCompatActivity(), FragmentManager.OnBackStackChangedListener {

    @Inject
    lateinit var dbLoader: DbLoader

    @Inject
    lateinit var sessionManager: SessionManager

    @BindView(R.id.toolbar_main)
    lateinit var toolbar: Toolbar

    @BindView(R.id.mainlist_drawerlayout)
    lateinit var drawerLayout: DrawerLayout

    @BindView(R.id.mainlist_navigationview)
    lateinit var navigationView: NavigationView

    @BindView(R.id.framelayout_main)
    lateinit var container: FrameLayout

    @BindView(R.id.main_coordinator_layout)
    lateinit var coordinatorLayout: CoordinatorLayout

    private var navigationHeaderViewHolder: NavigationHeaderViewHolder? = null
    private var actionBarDrawerToggle: ActionBarDrawerToggle? = null
    private var userViewModel: UserViewModel? = null
    private var todosViewModel: TodosViewModel? = null
    private var predefinedListsViewModel: PredefinedListsViewModel? = null
    private var listsViewModel: ListsViewModel? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        (application as AppController).appComponent.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)

        userViewModel = ViewModelProviders.of(this).get(UserViewModel::class.java)
        todosViewModel = ViewModelProviders.of(this).get(TodosViewModel::class.java)
        predefinedListsViewModel = ViewModelProviders.of(this).get(PredefinedListsViewModel::class.java)
        listsViewModel = ViewModelProviders.of(this).get(ListsViewModel::class.java)
        setSupportActionBar(toolbar)
        prepareNavigationView(navigationView, toolbar)
        if (container != null) {
            if (savedInstanceState != null) {
                // Prevent Fragment overlapping
                return
            }
            if (sessionManager!!.isLoggedIn) {
                val id = intent.getLongExtra("id", -1)
                val wasMainActivityStartedFromLauncherIcon = id == -1L
                if (wasMainActivityStartedFromLauncherIcon) {
                    openMainListFragment()
                } else if (wasMainActivityStartedFromNotification(id)) {
                    todosViewModel!!.todo = getNotificationRelatedTodo(id)
                    openMainListFragment()
                    val todoListFragment = TodoListFragment()
                    openAllPredefinedList(todoListFragment)
                    openModifyTodoFragment(todoListFragment)
                }
            } else {
                openLoginUserFragment()
            }
            prepareActionBarNavigationHandler()
            shouldDisplayHomeAsUp()
        }
        ReminderSetter.createReminderServices(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        navigationHeaderViewHolder!!.unbind()
    }

    private fun wasMainActivityStartedFromNotification(id: Long): Boolean {
        return id != -1L
    }

    private fun openAllPredefinedList(todoListFragment: TodoListFragment) {
        val allPredefinedListWhere = dbLoader!!.prepareAllPredefinedListWhere()
        val predefinedList = PredefinedList(getString(R.string.all_all), allPredefinedListWhere)
        todosViewModel!!.setIsPredefinedList(true)
        predefinedListsViewModel!!.predefinedList = predefinedList
        openTodoListFragment(todoListFragment)
    }

    private fun getNotificationRelatedTodo(id: Long): Todo {
        return dbLoader!!.getTodo(id)
    }

    private fun prepareActionBarNavigationHandler() {
        val fragmentManager = supportFragmentManager
        fragmentManager.addOnBackStackChangedListener(this)
    }

    private fun shouldDisplayHomeAsUp() {
        val fragmentManager = supportFragmentManager
        val shouldDisplay = fragmentManager.backStackEntryCount > 0
        val actionBar = supportActionBar
        if (actionBar != null) {
            if (shouldDisplay) {
                setDrawerEnabled(false)
                actionBar.setDisplayHomeAsUpEnabled(true)
            } else {
                actionBar.setDisplayHomeAsUpEnabled(false)
                setDrawerEnabled(true)
            }
            val actionBarTitle = actionBar.title
            if (isMainListFragment(shouldDisplay, actionBarTitle)) {
                actionBar.setTitle(R.string.app_name)
                actionBar.setDisplayHomeAsUpEnabled(false)
                setDrawerEnabled(true)
            }
        }
    }

    private fun isMainListFragment(shouldDisplay: Boolean, actionBarTitle: CharSequence?): Boolean {
        return !shouldDisplay && actionBarTitle != null && actionBarTitle != getString(R.string.all_login)
    }

    private fun setDrawerEnabled(enabled: Boolean) {
        if (!enabled) {
            disableDrawer()
            enableActionBarBackNavigation()
        } else {
            enableDrawer()
        }
    }

    private fun enableActionBarBackNavigation() {
        actionBarDrawerToggle!!.toolbarNavigationClickListener = View.OnClickListener {
            onBackPressed()
            hideSoftInput()
        }
    }

    private fun enableDrawer() {
        drawerLayout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        actionBarDrawerToggle!!.isDrawerIndicatorEnabled = true
        actionBarDrawerToggle!!.syncState()
    }

    private fun disableDrawer() {
        drawerLayout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        actionBarDrawerToggle!!.isDrawerIndicatorEnabled = false
        actionBarDrawerToggle!!.syncState()
    }

    private fun prepareNavigationView(navigationView: NavigationView?, toolbar: Toolbar?) {
        navigationView!!.setNavigationItemSelectedListener { menuItem ->
            val menuItemId = menuItem.itemId
            when (menuItemId) {
                R.id.menuitem_navigationdrawer_settings -> openSettingsPreferenceFragment()
                R.id.menuitem_navigationdrawer_logout -> openLogoutUserDialogFragment()
            }
            drawerLayout!!.closeDrawers()
            true
        }
        actionBarDrawerToggle = ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.actionbardrawertoggle_opendrawer,
                R.string.actionbardrawertoggle_closedrawer
        )
        drawerLayout!!.addDrawerListener(actionBarDrawerToggle!!)
        actionBarDrawerToggle!!.syncState()
    }

    private fun openLogoutUserDialogFragment() {
        val logoutUserDialogFragment = LogoutUserDialogFragment()
        logoutUserDialogFragment.show(supportFragmentManager, "LogoutUserDialogFragment")
    }

    fun onPrepareNavigationHeader() {
        val navigationHeader = navigationView!!.getHeaderView(0)
        navigationHeaderViewHolder = NavigationHeaderViewHolder(navigationHeader)
        val user = dbLoader!!.user
        userViewModel!!.setUser(user)
        updateNavigationHeader()
    }

    fun updateNavigationHeader() {
        val user = userViewModel!!.user.value
        if (user != null && navigationHeaderViewHolder != null) {
            navigationHeaderViewHolder!!.name.text = user.name
            navigationHeaderViewHolder!!.email.text = user.email
        }
    }

    fun onSearchActionItemClick() {
        openSearchFragment()
    }

    private fun openSearchFragment() {
        todosViewModel!!.clearTodos()
        val searchFragment = SearchFragment()
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(container!!.id, searchFragment)
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

    private fun hideSoftInput() {
        val inputMethodManager = getSystemService(
                Context.INPUT_METHOD_SERVICE
        ) as InputMethodManager
        val currentlyFocusedView = currentFocus
        if (currentlyFocusedView != null) {
            val windowToken = currentlyFocusedView.windowToken
            inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    override fun onBackPressed() {
        val fragmentManager = supportFragmentManager
        val modifyTodoFragment = fragmentManager.findFragmentByTag(
                "ModifyTodoFragment"
        ) as ModifyTodoFragment?
        if (modifyTodoFragment != null && modifyTodoFragment.isShouldNavigateBack) {
            super.onBackPressed()
        } else if (modifyTodoFragment != null) {
            modifyTodoFragment.handleModifyTodo()
        } else if (fragmentManager.findFragmentByTag("RegisterUserFragment") != null) {
            navigateBackToLoginUserFragment()
        } else if (fragmentManager.findFragmentByTag("ResetPasswordFragment") != null) {
            navigateBackToLoginUserFragment()
        } else if (drawerLayout!!.isDrawerOpen(GravityCompat.START)) {
            drawerLayout!!.closeDrawers()
        } else {
            super.onBackPressed()
        }
    }

    private fun navigateBackToLoginUserFragment() {
        super.onBackPressed()
        onSetActionBarTitle(getString(R.string.all_login))
    }

    fun onClickPredefinedList(predefinedList: PredefinedList?) {
        todosViewModel!!.setIsPredefinedList(true)
        predefinedListsViewModel!!.predefinedList = predefinedList
        openTodoListFragment()
    }

    fun onClickList(list: List?) {
        todosViewModel!!.setIsPredefinedList(false)
        listsViewModel!!.list = list
        openTodoListFragment()
    }

    private fun openTodoListFragment() {
        todosViewModel!!.clearTodos()
        val todoListFragment = TodoListFragment()
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(container!!.id, todoListFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    private fun openTodoListFragment(todoListFragment: TodoListFragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(container!!.id, todoListFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    fun onLogout() {
        cancelReminders()
        sessionManager!!.setLogin(false)
        val fragmentManager = supportFragmentManager
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        openLoginUserFragment()
        dbLoader!!.reCreateDb()
    }

    private fun cancelReminders() {
        val todosWithReminder = dbLoader!!.todosWithReminder
        for (todoWithReminder in todosWithReminder) {
            ReminderSetter.cancelReminderService(todoWithReminder)
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
                container!!.id,
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
                container!!.id,
                registerUserFragment,
                "RegisterUserFragment"
        )
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    fun onFinishRegisterUser() {
        val fragmentManager = supportFragmentManager
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        openLoginUserFragment()
        onSetActionBarTitle(getString(R.string.all_login))
    }

    private fun openLoginUserFragment() {
        val loginUserFragment = LoginUserFragment()
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(container!!.id, loginUserFragment)
        fragmentTransaction.commit()
    }

    fun onFinishLoginUser() {
        val fragmentManager = supportFragmentManager
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        openMainListFragment()
        onSetActionBarTitle(getString(R.string.app_name))
    }

    private fun openMainListFragment() {
        val mainListFragment = MainListFragment()
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(container!!.id, mainListFragment)
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
                container!!.id,
                modifyPasswordFragment,
                "ModifyPasswordFragment"
        )
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    override fun onBackStackChanged() {
        shouldDisplayHomeAsUp()
    }

    override fun onSupportNavigateUp(): Boolean {
        supportFragmentManager.popBackStack()
        return true
    }

    fun onFinishModifyPassword() {
        try {
            val snackbar = Snackbar.make(
                    coordinatorLayout!!,
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
                    coordinatorLayout!!,
                    R.string.resetpassword_passwordresetsuccessful,
                    Snackbar.LENGTH_LONG
            )
            showWhiteTextSnackbar(snackbar)
        } catch (e: NullPointerException) {
            // Snackbar or coordinatorLayout doesn't exists already.
        }
    }

    fun onSetActionBarTitle(title: String?) {
        if (supportActionBar != null)
            supportActionBar!!.title = title
        setDrawerEnabled(title == "Todo Cloud")
    }

    fun onStartActionMode(callback: ActionMode.Callback?) {
        startSupportActionMode(callback!!)
    }

    fun openModifyTodoFragment(targetFragment: Fragment?) {
        val modifyTodoFragment = ModifyTodoFragment()
        modifyTodoFragment.setTargetFragment(targetFragment, 0)
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(
                container!!.id,
                modifyTodoFragment,
                "ModifyTodoFragment"
        )
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    fun onOpenCreateTodoFragment(targetFragment: TodoListFragment?) {
        val createTodoFragment = CreateTodoFragment()
        createTodoFragment.setTargetFragment(targetFragment, 0)
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(container!!.id, createTodoFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    fun openSettingsPreferenceFragment() {
        val settingsPreferenceFragment = SettingsPreferenceFragment()
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(container!!.id, settingsPreferenceFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    fun ModifyTodo() {
        val todo = todosViewModel!!.todo
        dbLoader!!.updateTodo(todo)
        dbLoader!!.fixTodoPositions(null)
        updateTodosViewModel()
        if (isSetReminder(todo)) {
            if (shouldCreateReminderService(todo)) {
                ReminderSetter.createReminderService(todo)
            }
        } else {
            ReminderSetter.cancelReminderService(todo)
        }
    }

    fun CreateTodo() {
        val todo = todosViewModel!!.todo
        createTodoInLocalDatabase(todo)
        updateTodosViewModel()
        if (isSetReminder(todo) && isNotCompleted(todo)) {
            ReminderSetter.createReminderService(todo)
        }
    }

    private fun updateTodosViewModel() {
        val updateViewModelTask = UpdateViewModelTask(todosViewModel, this)
        updateViewModelTask.execute()
    }

    private fun isSetReminder(todo: Todo): Boolean {
        return !todo.reminderDateTime?.equals("-1")!!
    }

    private fun shouldCreateReminderService(todoToModify: Todo): Boolean {
        return isNotCompleted(todoToModify) && isNotDeleted(todoToModify)
    }

    private fun isNotCompleted(todo: Todo): Boolean {
        return !todo.completed!!
    }

    private fun isNotDeleted(todo: Todo): Boolean {
        return !todo.deleted!!
    }

    private fun createTodoInLocalDatabase(todoToCreate: Todo) {
        val listOnlineId = listsViewModel!!.list.listOnlineId
        if (isPredefinedListCompleted) {
            todoToCreate.completed = true
        }
        if (!todosViewModel!!.isPredefinedList) {
            todoToCreate.listOnlineId = listOnlineId
        }
        todoToCreate.userOnlineId = dbLoader!!.userOnlineId
        todoToCreate._id = dbLoader!!.createTodo(todoToCreate)
        val todoOnlineId = OnlineIdGenerator.generateOnlineId(
                DbConstants.Todo.DATABASE_TABLE,
                todoToCreate._id!!,
                dbLoader!!.apiKey
        )
        todoToCreate.todoOnlineId = todoOnlineId
        dbLoader!!.updateTodo(todoToCreate)
        dbLoader!!.fixTodoPositions(null)
    }

    private val isPredefinedListCompleted: Boolean
        private get() {
            if (todosViewModel!!.isPredefinedList) {
                val selectPredefinedListCompleted = DbConstants.Todo.KEY_COMPLETED +
                        "=" +
                        1 +
                        " AND " +
                        DbConstants.Todo.KEY_USER_ONLINE_ID +
                        "='" +
                        dbLoader!!.userOnlineId +
                        "'" +
                        " AND " +
                        DbConstants.Todo.KEY_DELETED +
                        "=" +
                        0
                return predefinedListsViewModel!!.predefinedList
                        .selectFromDB == selectPredefinedListCompleted
            }
            return false
        }
}
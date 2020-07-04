package com.rolandvitezhu.todocloud.ui.activity.main

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController
import com.rolandvitezhu.todocloud.app.AppController.Companion.showWhiteTextSnackbar
import com.rolandvitezhu.todocloud.data.List
import com.rolandvitezhu.todocloud.data.PredefinedList
import com.rolandvitezhu.todocloud.data.Todo
import com.rolandvitezhu.todocloud.data.User
import com.rolandvitezhu.todocloud.databinding.ActivityMainBinding
import com.rolandvitezhu.todocloud.databinding.NavigationdrawerHeaderBinding
import com.rolandvitezhu.todocloud.datastorage.DbConstants
import com.rolandvitezhu.todocloud.datastorage.DbLoader
import com.rolandvitezhu.todocloud.datastorage.asynctask.UpdateViewModelTask
import com.rolandvitezhu.todocloud.helper.OnlineIdGenerator
import com.rolandvitezhu.todocloud.helper.SessionManager
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
import kotlinx.android.synthetic.main.navigationdrawer_header.view.*
import javax.inject.Inject

class MainActivity : AppCompatActivity(), FragmentManager.OnBackStackChangedListener {

    @Inject
    lateinit var dbLoader: DbLoader

    @Inject
    lateinit var sessionManager: SessionManager

    private var actionBarDrawerToggle: ActionBarDrawerToggle? = null
    private var userViewModel: UserViewModel? = null
    private var todosViewModel: TodosViewModel? = null
    private var predefinedListsViewModel: PredefinedListsViewModel? = null
    private var listsViewModel: ListsViewModel? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        (application as AppController).appComponent.inject(this)
        super.onCreate(savedInstanceState)

        val activityMainBinding: ActivityMainBinding =
                DataBindingUtil.setContentView(this, R.layout.activity_main)
        val navigationdrawerHeaderViewBinding: NavigationdrawerHeaderBinding =
                DataBindingUtil.inflate(
                        layoutInflater,
                        R.layout.navigationdrawer_header,
                        activityMainBinding.mainlistNavigationview,
                        false)
        activityMainBinding.mainlistNavigationview.addHeaderView(navigationdrawerHeaderViewBinding.root)
        val view: View = activityMainBinding.root
        activityMainBinding.mainActivity = this

        userViewModel = ViewModelProviders.of(this).get(UserViewModel::class.java)
        todosViewModel = ViewModelProviders.of(this).get(TodosViewModel::class.java)
        predefinedListsViewModel = ViewModelProviders.of(this).get(PredefinedListsViewModel::class.java)
        listsViewModel = ViewModelProviders.of(this).get(ListsViewModel::class.java)

        navigationdrawerHeaderViewBinding.userViewModel = userViewModel
        navigationdrawerHeaderViewBinding.lifecycleOwner = this  // Set it, so the as we update
        // the data in the userViewModel, the UI will update automatically.

        setSupportActionBar(view.toolbar_main)
        prepareNavigationView(view)
        if (view.framelayout_main != null) {
            if (savedInstanceState != null) {
                // Prevent Fragment overlapping
                return
            }
            if (sessionManager!!.isLoggedIn) {
                val id = intent.getLongExtra("id", -1)
                val wasMainActivityStartedFromLauncherIcon = id == -1L
                if (wasMainActivityStartedFromLauncherIcon) {
                    openMainListFragment(view)
                } else if (wasMainActivityStartedFromNotification(id)) {
                    todosViewModel!!.todo = getNotificationRelatedTodo(id)
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
        val allPredefinedListWhere = dbLoader!!.prepareAllPredefinedListWhere()
        val predefinedList = PredefinedList(getString(R.string.all_all), allPredefinedListWhere)
        todosViewModel!!.setIsPredefinedList(true)
        predefinedListsViewModel!!.predefinedList = predefinedList
        openTodoListFragment(todoListFragment, view)
    }

    private fun getNotificationRelatedTodo(id: Long): Todo {
        return dbLoader!!.getTodo(id)
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
        actionBarDrawerToggle!!.toolbarNavigationClickListener = View.OnClickListener {
            onBackPressed()
            hideSoftInput()
        }
    }

    private fun enableDrawer(view: View?) {
        if (view != null)
            view.mainlist_drawerlayout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        else
            this.mainlist_drawerlayout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        actionBarDrawerToggle!!.isDrawerIndicatorEnabled = true
        actionBarDrawerToggle!!.syncState()
    }

    private fun disableDrawer(view: View?) {
        if (view != null)
            view.mainlist_drawerlayout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        else
            this.mainlist_drawerlayout!!.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        actionBarDrawerToggle!!.isDrawerIndicatorEnabled = false
        actionBarDrawerToggle!!.syncState()
    }

    private fun prepareNavigationView(view: View) {
        view.mainlist_navigationview!!.setNavigationItemSelectedListener { menuItem ->
            val menuItemId = menuItem.itemId
            when (menuItemId) {
                R.id.menuitem_navigationdrawer_settings -> openSettingsPreferenceFragment(view)
                R.id.menuitem_navigationdrawer_logout -> openLogoutUserDialogFragment()
            }
            view.mainlist_drawerlayout!!.closeDrawers()
            true
        }
        actionBarDrawerToggle = ActionBarDrawerToggle(
                this,
                view.mainlist_drawerlayout,
                view.toolbar_main,
                R.string.actionbardrawertoggle_opendrawer,
                R.string.actionbardrawertoggle_closedrawer
        )
        view.mainlist_drawerlayout!!.addDrawerListener(actionBarDrawerToggle!!)
        actionBarDrawerToggle!!.syncState()
    }

    private fun openLogoutUserDialogFragment() {
        val logoutUserDialogFragment = LogoutUserDialogFragment()
        logoutUserDialogFragment.show(supportFragmentManager, "LogoutUserDialogFragment")
    }

    fun onPrepareNavigationHeader() {
        val user = dbLoader!!.user
        (userViewModel!!.user as MutableLiveData<User>).value = user
    }

    fun onSearchActionItemClick() {
        openSearchFragment()
    }

    private fun openSearchFragment() {
        todosViewModel!!.clearTodos()
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
        } else if (this.mainlist_drawerlayout!!.isDrawerOpen(GravityCompat.START)) {
            this.mainlist_drawerlayout!!.closeDrawers()
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
        sessionManager!!.setLogin(false)
        val fragmentManager = supportFragmentManager
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        openLoginUserFragment(null)
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
        if (supportActionBar != null)
            supportActionBar!!.title = title
        setDrawerEnabled(title == "Todo Cloud", null)
    }

    fun onStartActionMode(callback: ActionMode.Callback?) {
        startSupportActionMode(callback!!)
    }

    fun openModifyTodoFragment(targetFragment: Fragment?, view: View?) {
        val modifyTodoFragment = ModifyTodoFragment()
        modifyTodoFragment.setTargetFragment(targetFragment, 0)
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        if (view != null)
            fragmentTransaction.replace(
                    view.framelayout_main!!.id,
                    modifyTodoFragment,
                    "ModifyTodoFragment"
            )
        else
            fragmentTransaction.replace(
                    this.framelayout_main!!.id,
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
        fragmentTransaction.replace(this.framelayout_main!!.id, createTodoFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    fun openSettingsPreferenceFragment(view: View) {
        val settingsPreferenceFragment = SettingsPreferenceFragment()
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(view.framelayout_main!!.id, settingsPreferenceFragment)
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
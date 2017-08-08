package com.example.todocloud;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.example.todocloud.data.List;
import com.example.todocloud.data.PredefinedList;
import com.example.todocloud.data.Todo;
import com.example.todocloud.data.User;
import com.example.todocloud.datastorage.DbLoader;
import com.example.todocloud.fragment.CreateTodoFragment;
import com.example.todocloud.fragment.LoginUserFragment;
import com.example.todocloud.fragment.LogoutUserDialogFragment;
import com.example.todocloud.fragment.MainListFragment;
import com.example.todocloud.fragment.ModifyTodoFragment;
import com.example.todocloud.fragment.RegisterUserFragment;
import com.example.todocloud.fragment.SearchFragment;
import com.example.todocloud.fragment.SettingsPreferenceFragment;
import com.example.todocloud.fragment.TodoListFragment;
import com.example.todocloud.helper.SessionManager;
import com.example.todocloud.receiver.ReminderSetter;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements
    MainListFragment.IMainListFragment,
    LoginUserFragment.ILoginUserFragment,
    RegisterUserFragment.IRegisterUserFragment,
    FragmentManager.OnBackStackChangedListener,
    TodoListFragment.ITodoListFragment,
    ModifyTodoFragment.IModifyTodoFragmentActionBar,
    CreateTodoFragment.ICreateTodoFragmentActionBar,
    SettingsPreferenceFragment.ISettingsPreferenceFragment,
    LogoutUserDialogFragment.ILogoutUserDialogFragment,
    SearchFragment.ISearchFragment {

  private ActionBarDrawerToggle actionBarDrawerToggle;
  private SessionManager sessionManager;
  private DrawerLayout drawerLayout;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
    setSupportActionBar(toolbar);

    drawerLayout = (DrawerLayout) findViewById(R.id.mainlist_drawerlayout);
    NavigationView navigationView = (NavigationView) findViewById(R.id.mainlist_navigationview);

    prepareNavigationView(navigationView, toolbar);

    if (findViewById(R.id.framelayout_main) != null) {
      if (savedInstanceState != null) {
        // Prevent Fragment overlapping
        return;
      }

      sessionManager = SessionManager.getInstance();
      if (sessionManager.isLoggedIn()) {

        long id = getIntent().getLongExtra("id", -1);
        boolean wasMainActivityStartedFromLauncherIcon = id == -1;
        if (wasMainActivityStartedFromLauncherIcon) {
          openMainListFragment();
        } else if (wasMainActivityStartedFromNotification(id)){
          openMainListFragment();
          TodoListFragment todoListFragment = new TodoListFragment();
          openAllPredefinedList(todoListFragment);
          Todo notificationRelatedTodo = getNotificationRelatedTodo(id);
          openModifyTodoFragment(notificationRelatedTodo, todoListFragment);
        }
      } else {
        openLoginUserFragment();
      }

      prepareActionBarNavigationHandler();
      shouldDisplayHomeAsUp();
    }
  }

  private boolean wasMainActivityStartedFromNotification(long id) {
    return id != -1;
  }

  private void openAllPredefinedList(TodoListFragment todoListFragment) {
    DbLoader dbLoader = new DbLoader();
    String allPredefinedListWhere = dbLoader.prepareAllPredefinedListWhere();
    Bundle arguments = new Bundle();
    arguments.putString("selectFromDB", allPredefinedListWhere);
    arguments.putString("title", "2");
    arguments.putBoolean("isPredefinedList", true);
    todoListFragment.setArguments(arguments);
    openTodoListFragment(todoListFragment);
  }

  private Todo getNotificationRelatedTodo(long id) {
    DbLoader dbLoader = new DbLoader();
    return dbLoader.getTodo(id);
  }

  private void prepareActionBarNavigationHandler() {
    FragmentManager fragmentManager = getSupportFragmentManager();
    fragmentManager.addOnBackStackChangedListener(this);
  }

  private void shouldDisplayHomeAsUp() {
    FragmentManager fragmentManager = getSupportFragmentManager();
    boolean shouldDisplay = fragmentManager.getBackStackEntryCount()>0;
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(shouldDisplay);
      CharSequence actionBarTitle = actionBar.getTitle();
      if (isMainListFragment(shouldDisplay, actionBarTitle)) {
        actionBar.setTitle(R.string.app_name);
        setDrawerEnabled(true);
      }
    }
  }

  private boolean isMainListFragment(boolean shouldDisplay, CharSequence actionBarTitle) {
    return !shouldDisplay && actionBarTitle != null && !actionBarTitle.equals(getString(R.string.all_login));
  }

  private void setDrawerEnabled(boolean enabled) {
    if (!enabled) {
      disableDrawer();
      enableActionBarBackNavigation();
    } else {
      enableDrawer();
    }
  }

  private void enableActionBarBackNavigation() {
    actionBarDrawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View view) {
        onBackPressed();
        hideSoftInput();
      }

    });
  }

  private void enableDrawer() {
    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
    actionBarDrawerToggle.syncState();
  }

  private void disableDrawer() {
    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    actionBarDrawerToggle.setDrawerIndicatorEnabled(false);
    actionBarDrawerToggle.syncState();
  }

  private void prepareNavigationView(NavigationView navigationView, Toolbar toolbar) {
    navigationView.setNavigationItemSelectedListener(
        new NavigationView.OnNavigationItemSelectedListener() {

          @Override
          public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            int menuItemId = menuItem.getItemId();

            switch (menuItemId) {
              case R.id.menuitem_navigationdrawer_settings:
                openSettingsPreferenceFragment();
                break;
              case R.id.menuitem_navigationdrawer_logout:
                openLogoutUserDialogFragment();
                break;
            }

            drawerLayout.closeDrawers();

            return true;
          }

        }
    );

    actionBarDrawerToggle = new ActionBarDrawerToggle(
        this,
        drawerLayout,
        toolbar,
        R.string.actionbardrawertoggle_opendrawer,
        R.string.actionbardrawertoggle_closedrawer
    );
    drawerLayout.addDrawerListener(actionBarDrawerToggle);
    actionBarDrawerToggle.syncState();
  }

  private void openLogoutUserDialogFragment() {
    LogoutUserDialogFragment logoutUserDialogFragment = new LogoutUserDialogFragment();
    logoutUserDialogFragment.show(getSupportFragmentManager(), "LogoutUserDialogFragment");
  }

  @Override
  public void onPrepareNavigationHeader() {
    NavigationView navigationView = (NavigationView) findViewById(R.id.mainlist_navigationview);
    View navigationHeader = navigationView.getHeaderView(0);
    TextView tvName = (TextView) navigationHeader.findViewById(
        R.id.textview_navigationdrawerheader_name
    );
    TextView tvEmail = (TextView) navigationHeader.findViewById(
        R.id.textview_navigationdrawerheader_email
    );
    DbLoader dbLoader = new DbLoader();
    User user = dbLoader.getUser();
    if (user != null) {
      tvName.setText(user.getName());
      tvEmail.setText(user.getEmail());
    }
  }

  @Override
  public void onSearchActionItemClick() {
    openSearchFragment();
  }

  private void openSearchFragment() {
    Bundle arguments = new Bundle(); // Will store UpdateTodoAdapter arguments later
    SearchFragment searchFragment = new SearchFragment();
    searchFragment.setArguments(arguments);
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(R.id.framelayout_main, searchFragment);
    fragmentTransaction.addToBackStack(null);
    fragmentTransaction.commit();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int itemId = item.getItemId();

    switch (itemId) {
      case android.R.id.home:
        hideSoftInput();
        onBackPressed();
        break;
    }

    return super.onOptionsItemSelected(item);
  }

  private void hideSoftInput() {
    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(
        Context.INPUT_METHOD_SERVICE
    );
    View currentlyFocusedView = getCurrentFocus();
    if (currentlyFocusedView != null) {
      IBinder windowToken = currentlyFocusedView.getWindowToken();
      inputMethodManager.hideSoftInputFromWindow(windowToken, 0);
    }
  }

  @Override
  public void onBackPressed() {
    FragmentManager fragmentManager = getSupportFragmentManager();
    ModifyTodoFragment modifyTodoFragment = (ModifyTodoFragment) fragmentManager.findFragmentByTag(
        "ModifyTodoFragment"
    );
    if (modifyTodoFragment != null && modifyTodoFragment.isShouldNavigateBack()) {
      super.onBackPressed();
    } else if (modifyTodoFragment != null) {
      modifyTodoFragment.handleModifyTodo();
    } else if (fragmentManager.findFragmentByTag("RegisterUserFragment") != null) {
      navigateBackToLoginUserFragment();
    } else if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
      drawerLayout.closeDrawers();
    } else {
      super.onBackPressed();
    }
  }

  private void navigateBackToLoginUserFragment() {
    super.onBackPressed();
    onSetActionBarTitle(getString(R.string.all_login));
  }

  @Override
  public void onClickPredefinedList(PredefinedList predefinedList) {
    Bundle arguments = new Bundle();
    arguments.putString("selectFromDB", predefinedList.getSelectFromDB());
    arguments.putString("title", predefinedList.getTitle());
    arguments.putBoolean("isPredefinedList", true);
    openTodoListFragment(arguments);
  }

  @Override
  public void onClickList(List list) {
    Bundle arguments = new Bundle();
    arguments.putString("listOnlineId", list.getListOnlineId());
    arguments.putString("title", list.getTitle());
    openTodoListFragment(arguments);
  }

  private void openTodoListFragment(Bundle arguments) {
    TodoListFragment todoListFragment = new TodoListFragment();
    todoListFragment.setArguments(arguments);
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(R.id.framelayout_main, todoListFragment);
    fragmentTransaction.addToBackStack(null);
    fragmentTransaction.commit();
  }

  private void openTodoListFragment(TodoListFragment todoListFragment) {
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(R.id.framelayout_main, todoListFragment);
    fragmentTransaction.addToBackStack(null);
    fragmentTransaction.commit();
  }

  @Override
  public void onLogout() {
    cancelReminders();
    sessionManager.setLogin(false);
    FragmentManager fragmentManager = getSupportFragmentManager();
    fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    openLoginUserFragment();
    DbLoader dbLoader = new DbLoader();
    dbLoader.reCreateDb();
  }

  private void cancelReminders() {
    DbLoader dbLoader = new DbLoader();
    ArrayList<Todo> todosWithReminder = dbLoader.getTodosWithReminder();
    for (Todo todoWithReminder:todosWithReminder) {
      ReminderSetter.cancelReminderService(todoWithReminder);
    }
  }

  @Override
  public void onClickLinkToRegisterUser() {
    openRegisterUserFragment();
  }

  private void openRegisterUserFragment() {
    RegisterUserFragment registerUserFragment = new RegisterUserFragment();
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(
        R.id.framelayout_main,
        registerUserFragment,
        "RegisterUserFragment"
    );
    fragmentTransaction.addToBackStack(null);
    fragmentTransaction.commit();
  }

  @Override
  public void onFinishRegisterUser() {
    FragmentManager fragmentManager = getSupportFragmentManager();
    fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    openLoginUserFragment();
    onSetActionBarTitle(getString(R.string.all_login));
  }

  private void openLoginUserFragment() {
    LoginUserFragment loginUserFragment = new LoginUserFragment();
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(R.id.framelayout_main, loginUserFragment);
    fragmentTransaction.commit();
  }

  @Override
  public void onFinishLoginUser() {
    FragmentManager fragmentManager = getSupportFragmentManager();
    fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    openMainListFragment();
    onSetActionBarTitle(getString(R.string.app_name));
  }

  private void openMainListFragment() {
    MainListFragment mainListFragment = new MainListFragment();
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(R.id.framelayout_main, mainListFragment);
    fragmentTransaction.commit();
  }

  @Override
  public void onBackStackChanged() {
    shouldDisplayHomeAsUp();
  }

  @Override
  public boolean onSupportNavigateUp() {
    getSupportFragmentManager().popBackStack();
    return true;
  }

  @Override
  public void onSetActionBarTitle(String title) {
    if (getSupportActionBar() != null)
      getSupportActionBar().setTitle(title);
    setDrawerEnabled(title.equals("Todo Cloud"));
  }

  @Override
  public void onStartActionMode(ActionMode.Callback callback) {
    startSupportActionMode(callback);
  }

  @Override
  public void onClickTodo(Todo todo, Fragment targetFragment) {
    openModifyTodoFragment(todo, targetFragment);
  }

  private void openModifyTodoFragment(Todo todo, Fragment targetFragment) {
    Bundle arguments = new Bundle();
    arguments.putParcelable("todo", todo);
    ModifyTodoFragment modifyTodoFragment = new ModifyTodoFragment();
    modifyTodoFragment.setTargetFragment(targetFragment, 0);
    modifyTodoFragment.setArguments(arguments);
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(
        R.id.framelayout_main,
        modifyTodoFragment,
        "ModifyTodoFragment"
    );
    fragmentTransaction.addToBackStack(null);
    fragmentTransaction.commit();
  }

  @Override
  public void onOpenCreateTodoFragment(TodoListFragment targetFragment) {
    CreateTodoFragment createTodoFragment = new CreateTodoFragment();
    createTodoFragment.setTargetFragment(targetFragment, 0);
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(R.id.framelayout_main, createTodoFragment);
    fragmentTransaction.addToBackStack(null);
    fragmentTransaction.commit();
  }

  public void openSettingsPreferenceFragment() {
    SettingsPreferenceFragment settingsPreferenceFragment = new SettingsPreferenceFragment();
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(R.id.framelayout_main, settingsPreferenceFragment);
    fragmentTransaction.addToBackStack(null);
    fragmentTransaction.commit();
  }

}

package com.rolandvitezhu.todocloud;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
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
import android.widget.FrameLayout;
import android.widget.TextView;

import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.data.List;
import com.rolandvitezhu.todocloud.data.PredefinedList;
import com.rolandvitezhu.todocloud.data.Todo;
import com.rolandvitezhu.todocloud.data.User;
import com.rolandvitezhu.todocloud.datastorage.DbLoader;
import com.rolandvitezhu.todocloud.fragment.CreateTodoFragment;
import com.rolandvitezhu.todocloud.fragment.LoginUserFragment;
import com.rolandvitezhu.todocloud.fragment.LogoutUserDialogFragment;
import com.rolandvitezhu.todocloud.fragment.MainListFragment;
import com.rolandvitezhu.todocloud.fragment.ModifyPasswordFragment;
import com.rolandvitezhu.todocloud.fragment.ModifyTodoFragment;
import com.rolandvitezhu.todocloud.fragment.RegisterUserFragment;
import com.rolandvitezhu.todocloud.fragment.ResetPasswordFragment;
import com.rolandvitezhu.todocloud.fragment.SearchFragment;
import com.rolandvitezhu.todocloud.fragment.SettingsPreferenceFragment;
import com.rolandvitezhu.todocloud.fragment.TodoListFragment;
import com.rolandvitezhu.todocloud.helper.SessionManager;
import com.rolandvitezhu.todocloud.receiver.ReminderSetter;

import java.util.ArrayList;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements
    MainListFragment.IMainListFragment,
    LoginUserFragment.ILoginUserFragment,
    RegisterUserFragment.IRegisterUserFragment,
    ModifyPasswordFragment.IModifyPasswordFragment,
    ResetPasswordFragment.IResetPasswordFragment,
    FragmentManager.OnBackStackChangedListener,
    TodoListFragment.ITodoListFragment,
    ModifyTodoFragment.IModifyTodoFragmentActionBar,
    CreateTodoFragment.ICreateTodoFragmentActionBar,
    SettingsPreferenceFragment.ISettingsPreferenceFragment,
    LogoutUserDialogFragment.ILogoutUserDialogFragment,
    SearchFragment.ISearchFragment {

  @Inject
  DbLoader dbLoader;
  @Inject
  SessionManager sessionManager;

  @BindView(R.id.toolbar_main)
  Toolbar toolbar;
  @BindView(R.id.mainlist_drawerlayout)
  DrawerLayout drawerLayout;
  @BindView(R.id.mainlist_navigationview)
  NavigationView navigationView;
  @BindView(R.id.framelayout_main)
  FrameLayout container;
  @BindView(R.id.main_coordinator_layout)
  CoordinatorLayout coordinatorLayout;

  private ActionBarDrawerToggle actionBarDrawerToggle;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);

    ((AppController) getApplication()).getAppComponent().inject(this);

    setSupportActionBar(toolbar);

    prepareNavigationView(navigationView, toolbar);

    if (container != null) {
      if (savedInstanceState != null) {
        // Prevent Fragment overlapping
        return;
      }

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
    String allPredefinedListWhere = dbLoader.prepareAllPredefinedListWhere();
    Bundle arguments = new Bundle();
    arguments.putString("selectFromDB", allPredefinedListWhere);
    arguments.putString("title", "2");
    arguments.putBoolean("isPredefinedList", true);
    todoListFragment.setArguments(arguments);
    openTodoListFragment(todoListFragment);
  }

  private Todo getNotificationRelatedTodo(long id) {
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
      if (shouldDisplay) {
        setDrawerEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
      } else {
        actionBar.setDisplayHomeAsUpEnabled(false);
        setDrawerEnabled(true);
      }

      CharSequence actionBarTitle = actionBar.getTitle();
      if (isMainListFragment(shouldDisplay, actionBarTitle)) {
        actionBar.setTitle(R.string.app_name);
        actionBar.setDisplayHomeAsUpEnabled(false);
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
    View navigationHeader = navigationView.getHeaderView(0);
    TextView tvName = navigationHeader.findViewById(R.id.textview_navigationdrawerheader_name);
    TextView tvEmail = navigationHeader.findViewById(R.id.textview_navigationdrawerheader_email);
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
    fragmentTransaction.replace(container.getId(), searchFragment);
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
    } else if (fragmentManager.findFragmentByTag("ResetPasswordFragment") != null) {
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
    fragmentTransaction.replace(container.getId(), todoListFragment);
    fragmentTransaction.addToBackStack(null);
    fragmentTransaction.commit();
  }

  private void openTodoListFragment(TodoListFragment todoListFragment) {
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(container.getId(), todoListFragment);
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
    dbLoader.reCreateDb();
  }

  private void cancelReminders() {
    ArrayList<Todo> todosWithReminder = dbLoader.getTodosWithReminder();
    for (Todo todoWithReminder:todosWithReminder) {
      ReminderSetter.cancelReminderService(todoWithReminder);
    }
  }

  @Override
  public void onClickLinkToRegisterUser() {
    openRegisterUserFragment();
  }

  @Override
  public void onClickLinkToResetPassword() {
    openResetPasswordFragment();
  }

  private void openResetPasswordFragment() {
    ResetPasswordFragment resetPasswordFragment = new ResetPasswordFragment();
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(
        container.getId(),
        resetPasswordFragment,
        "ResetPasswordFragment"
    );
    fragmentTransaction.addToBackStack(null);
    fragmentTransaction.commit();
  }

  private void openRegisterUserFragment() {
    RegisterUserFragment registerUserFragment = new RegisterUserFragment();
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(
        container.getId(),
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
    fragmentTransaction.replace(container.getId(), loginUserFragment);
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
    fragmentTransaction.replace(container.getId(), mainListFragment);
    fragmentTransaction.commit();
  }

  @Override
  public void onClickChangePassword() {
    openModifyPasswordFragment();
  }

  private void openModifyPasswordFragment() {
    ModifyPasswordFragment modifyPasswordFragment = new ModifyPasswordFragment();
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(
        container.getId(),
        modifyPasswordFragment,
        "ModifyPasswordFragment"
    );
    fragmentTransaction.addToBackStack(null);
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
  public void onFinishModifyPassword() {
    if (coordinatorLayout != null) {
      Snackbar snackbar = Snackbar.make(
          coordinatorLayout,
          R.string.modifypassword_passwordchangedsuccessfully,
          Snackbar.LENGTH_LONG
      );
      AppController.showWhiteTextSnackbar(snackbar);
    }
  }

  @Override
  public void onFinishResetPassword() {
    if (coordinatorLayout != null) {
      Snackbar snackbar = Snackbar.make(
          coordinatorLayout,
          R.string.resetpassword_passwordresetsuccessful,
          Snackbar.LENGTH_LONG
      );
      AppController.showWhiteTextSnackbar(snackbar);
    }
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
        container.getId(),
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
    fragmentTransaction.replace(container.getId(), createTodoFragment);
    fragmentTransaction.addToBackStack(null);
    fragmentTransaction.commit();
  }

  public void openSettingsPreferenceFragment() {
    SettingsPreferenceFragment settingsPreferenceFragment = new SettingsPreferenceFragment();
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(container.getId(), settingsPreferenceFragment);
    fragmentTransaction.addToBackStack(null);
    fragmentTransaction.commit();
  }

}

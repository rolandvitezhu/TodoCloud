package com.rolandvitezhu.todocloud.ui.activity.main;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.data.List;
import com.rolandvitezhu.todocloud.data.PredefinedList;
import com.rolandvitezhu.todocloud.data.Todo;
import com.rolandvitezhu.todocloud.data.User;
import com.rolandvitezhu.todocloud.datastorage.DbConstants;
import com.rolandvitezhu.todocloud.datastorage.DbLoader;
import com.rolandvitezhu.todocloud.datastorage.asynctask.UpdateViewModelTask;
import com.rolandvitezhu.todocloud.helper.OnlineIdGenerator;
import com.rolandvitezhu.todocloud.helper.SessionManager;
import com.rolandvitezhu.todocloud.receiver.ReminderSetter;
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.LogoutUserDialogFragment;
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.CreateTodoFragment;
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.LoginUserFragment;
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.MainListFragment;
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.ModifyPasswordFragment;
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.ModifyTodoFragment;
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.RegisterUserFragment;
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.ResetPasswordFragment;
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.SearchFragment;
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.TodoListFragment;
import com.rolandvitezhu.todocloud.ui.activity.main.preferencefragment.SettingsPreferenceFragment;
import com.rolandvitezhu.todocloud.ui.activity.main.viewholder.NavigationHeaderViewHolder;
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.ListsViewModel;
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.PredefinedListsViewModel;
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.TodosViewModel;
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.UserViewModel;

import java.util.ArrayList;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {

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

  private NavigationHeaderViewHolder navigationHeaderViewHolder;

  private ActionBarDrawerToggle actionBarDrawerToggle;

  private UserViewModel userViewModel;
  private TodosViewModel todosViewModel;
  private PredefinedListsViewModel predefinedListsViewModel;
  private ListsViewModel listsViewModel;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    ((AppController) getApplication()).getAppComponent().inject(this);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);

    userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
    todosViewModel = ViewModelProviders.of(this).get(TodosViewModel.class);
    predefinedListsViewModel = ViewModelProviders.of(this).get(PredefinedListsViewModel.class);
    listsViewModel = ViewModelProviders.of(this).get(ListsViewModel.class);

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
          todosViewModel.setTodo(getNotificationRelatedTodo(id));

          openMainListFragment();
          TodoListFragment todoListFragment = new TodoListFragment();
          openAllPredefinedList(todoListFragment);
          openModifyTodoFragment(todoListFragment);
        }
      } else {
        openLoginUserFragment();
      }

      prepareActionBarNavigationHandler();
      shouldDisplayHomeAsUp();
    }

    ReminderSetter.createReminderServices(getApplicationContext());
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    navigationHeaderViewHolder.unbind();
  }

  private boolean wasMainActivityStartedFromNotification(long id) {
    return id != -1;
  }

  private void openAllPredefinedList(TodoListFragment todoListFragment) {
    String allPredefinedListWhere = dbLoader.prepareAllPredefinedListWhere();

    PredefinedList predefinedList =
        new PredefinedList(getString(R.string.all_all), allPredefinedListWhere);

    todosViewModel.setIsPredefinedList(true);
    predefinedListsViewModel.setPredefinedList(predefinedList);

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

  public void onPrepareNavigationHeader() {
    View navigationHeader = navigationView.getHeaderView(0);
    navigationHeaderViewHolder = new NavigationHeaderViewHolder(navigationHeader);

    User user = dbLoader.getUser();
    userViewModel.setUser(user);

    updateNavigationHeader();
  }

  public void updateNavigationHeader() {
    User user = userViewModel.getUser().getValue();

    if (user != null && navigationHeaderViewHolder != null) {
      navigationHeaderViewHolder.name.setText(user.getName());
      navigationHeaderViewHolder.email.setText(user.getEmail());
    }
  }

  public void onSearchActionItemClick() {
    openSearchFragment();
  }

  private void openSearchFragment() {
    todosViewModel.clearTodos();

    SearchFragment searchFragment = new SearchFragment();
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

  public void onClickPredefinedList(PredefinedList predefinedList) {
    todosViewModel.setIsPredefinedList(true);
    predefinedListsViewModel.setPredefinedList(predefinedList);

    openTodoListFragment();
  }

  public void onClickList(List list) {
    todosViewModel.setIsPredefinedList(false);
    listsViewModel.setList(list);

    openTodoListFragment();
  }

  private void openTodoListFragment() {
    todosViewModel.clearTodos();

    TodoListFragment todoListFragment = new TodoListFragment();
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

  public void onClickLinkToRegisterUser() {
    openRegisterUserFragment();
  }

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

  public void onFinishModifyPassword() {
    try {
      Snackbar snackbar = Snackbar.make(
          coordinatorLayout,
          R.string.modifypassword_passwordchangedsuccessfully,
          Snackbar.LENGTH_LONG
      );
      AppController.Companion.showWhiteTextSnackbar(snackbar);
    } catch (NullPointerException e) {
      // Snackbar or coordinatorLayout doesn't exists already.
    }
  }

  public void onFinishResetPassword() {
    try {
      Snackbar snackbar = Snackbar.make(
          coordinatorLayout,
          R.string.resetpassword_passwordresetsuccessful,
          Snackbar.LENGTH_LONG
      );
      AppController.Companion.showWhiteTextSnackbar(snackbar);
    } catch (NullPointerException e) {
      // Snackbar or coordinatorLayout doesn't exists already.
    }
  }

  public void onSetActionBarTitle(String title) {
    if (getSupportActionBar() != null)
      getSupportActionBar().setTitle(title);
    setDrawerEnabled(title.equals("Todo Cloud"));
  }

  public void onStartActionMode(ActionMode.Callback callback) {
    startSupportActionMode(callback);
  }

  public void openModifyTodoFragment(Fragment targetFragment) {
    ModifyTodoFragment modifyTodoFragment = new ModifyTodoFragment();
    modifyTodoFragment.setTargetFragment(targetFragment, 0);
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

  public void ModifyTodo() {
    Todo todo = todosViewModel.getTodo();

    dbLoader.updateTodo(todo);
    dbLoader.fixTodoPositions();
    updateTodosViewModel();

    if (isSetReminder(todo)) {
      if (shouldCreateReminderService(todo)) {
        ReminderSetter.createReminderService(todo);
      }
    } else {
      ReminderSetter.cancelReminderService(todo);
    }
  }

  public void CreateTodo() {
    Todo todo = todosViewModel.getTodo();

    createTodoInLocalDatabase(todo);
    updateTodosViewModel();

    if (isSetReminder(todo) && isNotCompleted(todo)) {
      ReminderSetter.createReminderService(todo);
    }
  }

  private void updateTodosViewModel() {
    UpdateViewModelTask updateViewModelTask = new UpdateViewModelTask(todosViewModel, this);
    updateViewModelTask.execute();
  }

  private boolean isSetReminder(Todo todo) {
    return !todo.getReminderDateTime().equals("-1");
  }

  private boolean shouldCreateReminderService(Todo todoToModify) {
    return isNotCompleted(todoToModify) && isNotDeleted(todoToModify);
  }

  private boolean isNotCompleted(Todo todo) {
    return !todo.getCompleted();
  }

  private boolean isNotDeleted(Todo todo) {
    return !todo.getDeleted();
  }

  private void createTodoInLocalDatabase(Todo todoToCreate) {
    String listOnlineId = listsViewModel.getList().getListOnlineId();

    if (isPredefinedListCompleted()) {
      todoToCreate.setCompleted(true);
    }

    if (!todosViewModel.isPredefinedList()) {
      todoToCreate.setListOnlineId(listOnlineId);
    }

    todoToCreate.setUserOnlineId(dbLoader.getUserOnlineId());
    todoToCreate.set_id(dbLoader.createTodo(todoToCreate));
    String todoOnlineId = OnlineIdGenerator.generateOnlineId(
        DbConstants.Todo.DATABASE_TABLE,
        todoToCreate.get_id(),
        dbLoader.getApiKey()
    );
    todoToCreate.setTodoOnlineId(todoOnlineId);

    dbLoader.updateTodo(todoToCreate);
    dbLoader.fixTodoPositions();
  }

  private boolean isPredefinedListCompleted() {
    if (todosViewModel.isPredefinedList())
    {
      String selectPredefinedListCompleted =
          DbConstants.Todo.KEY_COMPLETED +
              "=" +
              1 +
              " AND " +
              DbConstants.Todo.KEY_USER_ONLINE_ID +
              "='" +
              dbLoader.getUserOnlineId() +
              "'" +
              " AND " +
              DbConstants.Todo.KEY_DELETED +
              "=" +
              0;

      return predefinedListsViewModel.getPredefinedList()
          .getSelectFromDB().equals(selectPredefinedListCompleted);
    }

    return false;
  }

}

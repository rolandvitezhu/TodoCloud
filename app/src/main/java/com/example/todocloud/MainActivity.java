package com.example.todocloud;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.example.todocloud.data.List;
import com.example.todocloud.data.PredefinedListItem;
import com.example.todocloud.data.Todo;
import com.example.todocloud.data.User;
import com.example.todocloud.datastorage.DbLoader;
import com.example.todocloud.fragment.LoginUserFragment;
import com.example.todocloud.fragment.LogoutFragment;
import com.example.todocloud.fragment.MainListFragment;
import com.example.todocloud.fragment.RegisterUserFragment;
import com.example.todocloud.fragment.SettingsFragment;
import com.example.todocloud.fragment.TodoCreateFragment;
import com.example.todocloud.fragment.TodoListFragment;
import com.example.todocloud.fragment.TodoModifyFragment;
import com.example.todocloud.helper.SessionManager;
import com.example.todocloud.service.ReminderService;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MainListFragment.IMainListFragment,
    LoginUserFragment.ILoginUserFragment, RegisterUserFragment.IRegisterUserFragment,
    FragmentManager.OnBackStackChangedListener,
    TodoListFragment.ITodoListFragment,
    TodoModifyFragment.ITodoModifyFragmentActionBar,
    TodoCreateFragment.ITodoCreateFragmentActionBar, SettingsFragment.ISettingsFragment,
    LogoutFragment.ILogoutFragment {

  private ActionBarDrawerToggle actionBarDrawerToggle;
  private SessionManager sessionManager;
  private DrawerLayout drawerLayout;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
    NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);

    prepareNavigationView(navigationView, toolbar);

    if (findViewById(R.id.FragmentContainer) != null) {
      if (savedInstanceState != null) {
        // Első indításnál null az érték.
        // A vizsgálat célja, hogy csak akkor hozzunk létre fragment-et, ha még nincs.
        return;
      }

      sessionManager = SessionManager.getInstance();
      if (sessionManager.isLoggedIn()) {
        // Bejelentkezett.

        onSetNavigationHeader();

        long id = getIntent().getLongExtra("id", -1);
        if (id == -1) {
          // Az appot Launcher-rel indítottuk. Megnyitjuk a MainListFragment-et.
          MainListFragment mainListFragment = new MainListFragment();
          FragmentManager fragmentManager = getSupportFragmentManager();
          FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
          fragmentTransaction.replace(R.id.FragmentContainer, mainListFragment);
          fragmentTransaction.commit();
        } else {
          // Az appot Notification-ön történő kattintással indítottuk.

          FragmentManager fragmentManager = getSupportFragmentManager();
          FragmentTransaction fragmentTransaction;

          // Megnyitjuk a MainListFragment-et.
          MainListFragment mainListFragment = new MainListFragment();
          fragmentTransaction = fragmentManager.beginTransaction();
          fragmentTransaction.replace(R.id.FragmentContainer, mainListFragment);
          fragmentTransaction.commit();

          // Az "Összes" listát nyitjuk meg.
          TodoListFragment todoListFragment = new TodoListFragment();
          Bundle args = new Bundle();
          args.putString("selectFromDB", null);
          todoListFragment.setArguments(args);
          fragmentTransaction = fragmentManager.beginTransaction();
          fragmentTransaction.replace(R.id.FragmentContainer, todoListFragment);
          fragmentTransaction.addToBackStack(null);
          fragmentTransaction.commit();

          // Megnyitjuk a TodoModifyFragment-et a megfelelő tennivalóval (a Notification-höz
          // tartozóval).
          DbLoader dbLoader = new DbLoader();
          Todo todo = dbLoader.getTodo(id);

          TodoModifyFragment todoModifyFragment = new TodoModifyFragment();
          todoModifyFragment.setTargetFragment(todoListFragment, 0);
          Bundle bundle = new Bundle();
          bundle.putParcelable("todo", todo);
          todoModifyFragment.setArguments(bundle);
          fragmentTransaction = fragmentManager.beginTransaction();
          fragmentTransaction.replace(R.id.FragmentContainer,
              todoModifyFragment, "TodoModifyFragment");
          fragmentTransaction.addToBackStack(null);
          fragmentTransaction.commit();
        }
      } else {
        // Nem jelentkezett be.
        LoginUserFragment loginUserFragment = new LoginUserFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.FragmentContainer,
            loginUserFragment).commit();
      }

      getSupportFragmentManager().addOnBackStackChangedListener(this);
      shouldDisplayHomeUp();
    }
  }

  /**
   * Megjeleníti az Action Bar-on vissza navigáló gombot, feltéve hogy szükséges (van hová vissza
   * navigálni).
   */
  private void shouldDisplayHomeUp() {
    boolean display = getSupportFragmentManager().getBackStackEntryCount()>0;
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(display);
      if (!display && getSupportActionBar().getTitle() != null &&
          !getSupportActionBar().getTitle().equals(getString(R.string.login))) {
        // A MainListFragment-en megjelenítjük a NavigationDrawer-t.
        getSupportActionBar().setTitle(R.string.app_name);
        setDrawerEnabled(true);
      }
    }
  }

  /**
   * A megadott logikai értéktől függően engedélyezi vagy letiltja a NavigationDrawer-t.
   * @param enabled A megadott logikai érték.
   */
  private void setDrawerEnabled(boolean enabled) {
    // ActionBarDrawerToggle eltüntetése, ha nem "Todo Cloud" az AppBar title-je.
    if (!enabled) {
      // ActionBarDrawerToggle elrejtése.
      drawerLayout.setDrawerLockMode(
          DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
      actionBarDrawerToggle.setDrawerIndicatorEnabled(false);
      actionBarDrawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {

        @Override
        public void onClick(View view) {
          onBackPressed();
        }

      });
      actionBarDrawerToggle.syncState();
    } else {
      // ActionBarDrawerToggle megjelenítése.
      drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
      actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
      actionBarDrawerToggle.syncState();
    }
  }

  private void prepareNavigationView(NavigationView navigationView, Toolbar toolbar) {
    navigationView.setNavigationItemSelectedListener(
        new NavigationView.OnNavigationItemSelectedListener() {

          @Override
          public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            int itemId = menuItem.getItemId();

            switch (itemId) {
              case R.id.itemSettings:
                openSettingsDialogFragment();
                break;
              case R.id.itemLogout:
                openLogoutDialogFragment();
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
        R.string.open_drawer,
        R.string.close_drawer
    );
    drawerLayout.addDrawerListener(actionBarDrawerToggle);
    actionBarDrawerToggle.syncState();
  }

  private void openLogoutDialogFragment() {
    LogoutFragment logoutFragment = new LogoutFragment();
    logoutFragment.show(getSupportFragmentManager(), "LogoutFragment");
  }

  /**
   * Beállítja a NavigationView HeaderView-ját.
   */
  @Override
  public void onSetNavigationHeader() {
    NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
    View navigationHeader = navigationView.getHeaderView(0);
    TextView tvName = (TextView) navigationHeader.findViewById(R.id.tvName);
    TextView tvEmail = (TextView) navigationHeader.findViewById(R.id.tvEmail);
    DbLoader dbLoader = new DbLoader();
    User user = dbLoader.getUser();
    tvName.setText(user.getName());
    tvEmail.setText(user.getEmail());
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {

      // Elrejti a virtális billentyűzetet (TodoCreateFragment Title mezőjén állva, majd
      // a vissza navigációs gombot megnyomva a virtuális billentyűzet nem tűnt el 5.1.1 API 22-n
      // tesztelve; API 23-as emulátoron nem jött elő ez a probléma).
      InputMethodManager inputMethodManager = (InputMethodManager)
          getSystemService(Context.INPUT_METHOD_SERVICE);
      if (getCurrentFocus() != null)
        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

      onBackPressed();

      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onBackPressed() {
    TodoModifyFragment todoModifyFragment =
        (TodoModifyFragment) getSupportFragmentManager().
            findFragmentByTag("TodoModifyFragment");
    if (todoModifyFragment != null && todoModifyFragment.isShouldNavigateBack()) {
      super.onBackPressed();
    } else if (todoModifyFragment != null)
      todoModifyFragment.updateTodo();
    else if (getSupportFragmentManager().findFragmentByTag("RegisterUserFragment") != null) {
      // A RegisterUserFragment-ről visszanavigálva a LoginUserFragment-re.
      super.onBackPressed();
      onSetActionBarTitle(getString(R.string.login));
    } else if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
      drawerLayout.closeDrawers();
    } else {
      super.onBackPressed();
    }
  }

  @Override
  public void onOpenTodoListFragment(PredefinedListItem predefinedListItem) {
    Bundle arguments = new Bundle();
    arguments.putString("selectFromDB", predefinedListItem.getSelectFromDB());
    arguments.putString("title", predefinedListItem.getTitle());
    arguments.putBoolean("isPredefinedList", true);

    TodoListFragment todoListFragment = new TodoListFragment();
    todoListFragment.setArguments(arguments);
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(R.id.FragmentContainer, todoListFragment);
    fragmentTransaction.addToBackStack(null);
    fragmentTransaction.commit();
  }

  @Override
  public void onOpenTodoListFragment(List listToOpen) {
    Bundle arguments = new Bundle();
    arguments.putString("listOnlineId", listToOpen.getListOnlineId());
    arguments.putString("title", listToOpen.getTitle());

    TodoListFragment todoListFragment = new TodoListFragment();
    todoListFragment.setArguments(arguments);
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(R.id.FragmentContainer, todoListFragment);
    fragmentTransaction.addToBackStack(null);
    fragmentTransaction.commit();
  }

  @Override
  public void onLogout() {

    // Emlékeztetők törlése.
    DbLoader dbLoader = new DbLoader();
    ArrayList<Todo> todos = dbLoader.getTodosWithReminder();
    for (Todo todo:todos) {
      Intent service = new Intent(getApplicationContext(), ReminderService.class);
      service.putExtra("todo", todo);
      service.setAction(ReminderService.CANCEL);
      getApplicationContext().startService(service);
    }

    sessionManager.setLogin(false);
    openLoginUserFragment();
    dbLoader.reCreateDb();
  }

  @Override
  public void onClickLinkToRegisterUser() {
    openRegisterUserFragment();
  }

  private void openRegisterUserFragment() {
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    RegisterUserFragment registerUserFragment = new RegisterUserFragment();
    fragmentTransaction.replace(
        R.id.FragmentContainer,
        registerUserFragment,
        "RegisterUserFragment"
    );
    fragmentTransaction.addToBackStack(null);
    fragmentTransaction.commit();
  }

  @Override
  public void onFinishRegisterUser() {
    openLoginUserFragment();
    onSetActionBarTitle(getString(R.string.login));
  }

  private void openLoginUserFragment() {
    FragmentManager fragmentManager = getSupportFragmentManager();
    fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    LoginUserFragment loginUserFragment = new LoginUserFragment();
    fragmentTransaction.replace(R.id.FragmentContainer, loginUserFragment);
    fragmentTransaction.commit();
  }

  @Override
  public void onFinishLoginUser() {
    FragmentManager fragmentManager = getSupportFragmentManager();
    fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    openMainListFragment(fragmentManager);
    onSetActionBarTitle(getString(R.string.app_name));
  }

  private void openMainListFragment(FragmentManager fragmentManager) {
    MainListFragment mainListFragment = new MainListFragment();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(R.id.FragmentContainer, mainListFragment);
    fragmentTransaction.commit();
  }

  @Override
  public void onBackStackChanged() {
    shouldDisplayHomeUp();
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

  /**
   * Elindítja az ActionMode-ot.
   * @param callback Az ActionMode indításához megadott Callback.
   */
  @Override
  public void onStartActionMode(ActionMode.Callback callback) {
    startSupportActionMode(callback);
  }

  @Override
  public void onClickTodo(Todo clickedTodo, TodoListFragment targetFragment) {
    openTodoModifyFragment(clickedTodo, targetFragment);
  }

  private void openTodoModifyFragment(Todo clickedTodo, TodoListFragment targetFragment) {
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

    TodoModifyFragment todoModifyFragment = new TodoModifyFragment();
    todoModifyFragment.setTargetFragment(targetFragment, 0);

    Bundle arguments = new Bundle();
    arguments.putParcelable("todo", clickedTodo);
    todoModifyFragment.setArguments(arguments);

    fragmentTransaction.replace(R.id.FragmentContainer, todoModifyFragment,
        "TodoModifyFragment");
    fragmentTransaction.addToBackStack(null);
    fragmentTransaction.commit();
  }

  @Override
  public void onOpenTodoCreateFragment(TodoListFragment targetFragment) {
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    TodoCreateFragment todoCreateFragment = new TodoCreateFragment();
    todoCreateFragment.setTargetFragment(targetFragment, 0);
    fragmentTransaction.replace(R.id.FragmentContainer, todoCreateFragment);
    fragmentTransaction.addToBackStack(null);
    fragmentTransaction.commit();
  }

  public void openSettingsDialogFragment() {
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    SettingsFragment settingsFragment = new SettingsFragment();
    fragmentTransaction.replace(R.id.FragmentContainer, settingsFragment);
    fragmentTransaction.addToBackStack(null);
    fragmentTransaction.commit();
  }

}

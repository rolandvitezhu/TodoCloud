package com.rolandvitezhu.todocloud.fragment;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.app.Constant;
import com.rolandvitezhu.todocloud.data.Todo;
import com.rolandvitezhu.todocloud.fragment.DatePickerDialogFragment.IDatePickerDialogFragment;
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity;
import com.rolandvitezhu.todocloud.viewmodel.TodosViewModel;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class CreateTodoFragment extends Fragment implements
    IDatePickerDialogFragment,
    ReminderDatePickerDialogFragment.IReminderDatePickerDialogFragment,
    ReminderTimePickerDialogFragment.IReminderTimePickerDialogFragment {

  @BindView(R.id.textinputlayout_createtodo_title)
  TextInputLayout tilTitle;
  @BindView(R.id.textinputedittext_createtodo_title)
  TextInputEditText tietTitle;
  @BindView(R.id.switch_createtodo_priority)
  SwitchCompat switchPriority;
  @BindView(R.id.textview_createtodo_duedate)
  TextView tvDueDate;
  @BindView(R.id.textview_createtodo_reminderdatetime)
  TextView tvReminderDateTime;
  @BindView(R.id.button_createtodo_clearduedate)
  TextView btnClearDueDate;
  @BindView(R.id.button_createtodo_clearreminder)
  TextView btnClearReminder;
  @BindView(R.id.textinputedittext_createtodo_description)
  TextInputEditText tietDescription;

  private LocalDate dueDate;
  private ZonedDateTime zdtDueDate;
  private long dueDateLong;
  private String dueDateDisp;

  private LocalDateTime reminderDateTime;
  private ZonedDateTime zdtReminderDateTime;
  private long reminderDateTimeLong;
  private String reminderDateTimeDisp;

  Unbinder unbinder;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    initDueDate();
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
		View view = inflater.inflate(R.layout.fragment_createtodo, container, false);
		unbinder = ButterKnife.bind(this, view);

    tietTitle.addTextChangedListener(new TextWatcher() {

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {

      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {

      }

      @Override
      public void afterTextChanged(Editable s) {
        validateTitle();
      }

    });

    setTvDueDateText(dueDate);
    setTvReminderDateTimeText(null);

	  return view;
  }

  @Override
  public void onResume() {
    super.onResume();

    ((MainActivity)this.getActivity()).onSetActionBarTitle(getString(R.string.all_createtodo));
    setClearDueDateVisibility();
    setClearReminderDateTimeVisibility();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
  }

  private void initDueDate() {
    if (dueDate == null) {
      dueDate = LocalDate.now();
      zdtDueDate = dueDate.atStartOfDay(ZoneId.systemDefault());
      dueDateLong = zdtDueDate.toInstant().toEpochMilli();
      dueDateDisp = DateUtils.formatDateTime(
          AppController.getAppContext(),
          dueDateLong,
          DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR
      );
    }
  }

  private void initReminderDateTime() {
    if (reminderDateTime == null) {
      reminderDateTime = LocalDateTime.now();
      zdtReminderDateTime = reminderDateTime.atZone(ZoneId.systemDefault());
      reminderDateTimeLong = zdtReminderDateTime.toInstant().toEpochMilli();
      reminderDateTimeDisp = DateUtils.formatDateTime(
          AppController.getAppContext(),
          reminderDateTimeLong,
          DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_TIME
      );
    }
  }

  private void setClearDueDateVisibility() {
    if (dueDate != null)
      btnClearDueDate.setVisibility(View.VISIBLE);
    else
      btnClearDueDate.setVisibility(View.GONE);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    String givenTitle = tietTitle.getText().toString().trim();
    if (!givenTitle.isEmpty()) {
      menu.clear();
      inflater.inflate(R.menu.fragment_createtodo, menu);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int menuItemId = item.getItemId();
    if (menuItemId == R.id.menuitem_createtodo) {
      hideSoftInput();
      Todo todoToCreate = prepareTodoToCreate();

      TodosViewModel todosViewModel =
          ViewModelProviders.of(this.getActivity()).get(TodosViewModel.class);

      todosViewModel.setTodo(todoToCreate);

      ((MainActivity)this.getActivity()).CreateTodo();
      ((MainActivity)this.getActivity()).onBackPressed();
    }
    return super.onOptionsItemSelected(item);
  }

  @NonNull
  private Todo prepareTodoToCreate() {
    Todo todo = new Todo();
    todo.setTitle(tietTitle.getText().toString().trim());
    todo.setPriority(switchPriority.isChecked());
    todo.setDueDate(dueDateLong);
    todo.setReminderDateTime(reminderDateTimeLong);

    // Old code
//    todo.setDueDate(simpleDateFormat.format(dueDate));
//    if (tvReminderDateTime.getText().equals(getString(R.string.all_noreminder))) {
//      todo.setReminderDateTime("-1");
//    } else {
//      String reminderDateTime = reminderDateTimeFormat.format(this.reminderDateTime);
//      todo.setReminderDateTime(reminderDateTime);
//    }

    String giveDescription = tietDescription.getText().toString().trim();
    if (!giveDescription.equals("")) {
      todo.setDescription(tietDescription.getText().toString());
    } else {
      todo.setDescription(null);
    }
    todo.setCompleted(false);
    todo.setRowVersion(0);
    todo.setDeleted(false);
    todo.setDirty(true);
    // TODO: Set position - get last position and increment by 100
    // todo.setPosition();

    return todo;
  }

  private void hideSoftInput() {
    FragmentActivity activity = getActivity();
    InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(
        Context.INPUT_METHOD_SERVICE
    );
    View currentlyFocusedView = activity.getCurrentFocus();
    if (currentlyFocusedView != null) {
      IBinder windowToken = currentlyFocusedView.getWindowToken();
      inputMethodManager.hideSoftInputFromWindow(windowToken, 0);
    }
  }

  private void validateTitle() {
    getActivity().invalidateOptionsMenu();
    String givenTitle = tietTitle.getText().toString().trim();
    if (givenTitle.isEmpty()) tilTitle.setError(getString(R.string.all_entertitle));
    else tilTitle.setErrorEnabled(false);
  }

  private void openDatePickerDialogFragment() {
	  Bundle arguments = new Bundle();
    arguments.putSerializable(Constant.DUE_DATE, dueDate);
    DatePickerDialogFragment datePickerDialogFragment = new DatePickerDialogFragment();
    datePickerDialogFragment.setTargetFragment(this, 0);
    datePickerDialogFragment.setArguments(arguments);
	  datePickerDialogFragment.show(getFragmentManager(), "DatePickerDialogFragment");
  }

  private void openReminderDatePickerDialogFragment() {
    Bundle arguments = new Bundle();
    arguments.putSerializable(Constant.REMINDER_DATE_TIME, reminderDateTime);
    ReminderDatePickerDialogFragment reminderDatePickerDialogFragment =
        new ReminderDatePickerDialogFragment();
    reminderDatePickerDialogFragment.setTargetFragment(CreateTodoFragment.this, 0);
    reminderDatePickerDialogFragment.setArguments(arguments);
    reminderDatePickerDialogFragment.show(
        getFragmentManager(),
        "ReminderDatePickerDialogFragment"
    );
  }

  @Override
  public void onSelectDate(LocalDate date) {
    setTvDueDateText(date);
    setClearDueDateVisibility();
  }

  private void setTvDueDateText(@Nullable LocalDate date) {
    if (date != null) {
      dueDate = date;
      zdtDueDate = dueDate.atStartOfDay(ZoneId.systemDefault());
      dueDateLong = zdtDueDate.toInstant().toEpochMilli();
      dueDateDisp = DateUtils.formatDateTime(
          AppController.getAppContext(),
          dueDateLong,
          DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR
      );
      tvDueDate.setText(dueDateDisp);
    } else {
      tvDueDate.setText(R.string.all_noduedate);
    }
  }

  @Override
  public void onSelectReminderDate(LocalDateTime date) {
    openReminderTimePickerDialogFragment(date);
  }

  private void openReminderTimePickerDialogFragment(LocalDateTime date) {
    Bundle arguments = new Bundle();
    arguments.putSerializable(Constant.REMINDER_DATE_TIME, date);
    ReminderTimePickerDialogFragment reminderTimePickerDialogFragment =
        new ReminderTimePickerDialogFragment();
    reminderTimePickerDialogFragment.setTargetFragment(this, 0);
    reminderTimePickerDialogFragment.setArguments(arguments);
    reminderTimePickerDialogFragment.show(
        getFragmentManager(),
        "ReminderTimePickerDialogFragment"
    );
  }

  @Override
  public void onDeleteReminder() {
    tvReminderDateTime.setText(getString(R.string.all_noreminder));
  }

  @Override
  public void onSelectReminderDateTime(LocalDateTime date) {
    setTvReminderDateTimeText(date);
    setClearReminderDateTimeVisibility();
  }

  private void setClearReminderDateTimeVisibility() {
    if (reminderDateTime != null)
      btnClearReminder.setVisibility(View.VISIBLE);
    else
      btnClearReminder.setVisibility(View.GONE);
  }

  private void setTvReminderDateTimeText(@Nullable LocalDateTime date) {
    if (date != null) {
      reminderDateTime = date;
      zdtReminderDateTime = reminderDateTime.atZone(ZoneId.systemDefault());
      reminderDateTimeLong = zdtReminderDateTime.toInstant().toEpochMilli();
      reminderDateTimeDisp = DateUtils.formatDateTime(
          AppController.getAppContext(),
          reminderDateTimeLong,
          DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_TIME
      );
      tvReminderDateTime.setText(reminderDateTimeDisp);
    } else {
      tvReminderDateTime.setText(R.string.all_noreminder);
    }
  }

  private void clearDueDate() {
    tvDueDate.setText(R.string.all_noduedate);
    dueDate = null;
    zdtDueDate = null;
    dueDateLong = 0;
    dueDateDisp = null;
  }

  private void clearReminder() {
    tvReminderDateTime.setText(R.string.all_noreminder);
    reminderDateTime = null;
    zdtReminderDateTime = null;
    reminderDateTimeLong = 0;
    reminderDateTimeDisp = null;
  }

  @OnClick(R.id.textview_createtodo_duedate)
  public void onTvDueDateClick(View view) {
    openDatePickerDialogFragment();
  }

  @OnClick(R.id.button_createtodo_clearduedate)
  public void onBtnClearDueDateClick(View view) {
    clearDueDate();
    setClearDueDateVisibility();
  }

  @OnClick(R.id.button_createtodo_clearreminder)
  public void onBtnClearReminderClick(View view) {
    clearReminder();
    setClearReminderDateTimeVisibility();
  }

  @OnClick(R.id.textview_createtodo_reminderdatetime)
  public void onTvReminderDateTimeClick(View view) {
    openReminderDatePickerDialogFragment();
  }
	
}

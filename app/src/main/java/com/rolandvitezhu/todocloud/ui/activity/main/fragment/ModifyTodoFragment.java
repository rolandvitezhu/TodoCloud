package com.rolandvitezhu.todocloud.ui.activity.main.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.app.Constant;
import com.rolandvitezhu.todocloud.data.Todo;
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity;
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.DatePickerDialogFragment;
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.ReminderDatePickerDialogFragment;
import com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment.ReminderTimePickerDialogFragment;
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.TodosViewModel;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class ModifyTodoFragment extends Fragment {

  @BindView(R.id.textinputlayout_modifytodo_title)
  TextInputLayout tilTitle;
  @BindView(R.id.textinputedittext_modifytodo_title)
  TextInputEditText tietTitle;

  @BindView(R.id.switch_modifytodo_priority)
	SwitchCompat switchPriority;

	@BindView(R.id.textview_modifytodo_duedate)
  TextView tvDueDate;
  @BindView(R.id.textview_modifytodo_reminderdatetime)
	TextView tvReminderDateTime;

  @BindView(R.id.button_modifytodo_clearduedate)
  Button btnClearDueDate;
  @BindView(R.id.button_modifytodo_clearreminder)
  Button btnClearReminder;

  @BindView(R.id.textinputlayout_modifytodo_description)
  TextInputLayout tilDescription;
  @BindView(R.id.textinputedittext_modifytodo_description)
  TextInputEditText tietDescription;

  private Todo todo;

  private LocalDate dueDate;
  private ZonedDateTime zdtDueDate;
  private long dueDateLong;
  private String dueDateDisp;

  private LocalDateTime reminderDateTime;
  private ZonedDateTime zdtReminderDateTime;
  private long reminderDateTimeLong;
  private String reminderDateTimeDisp;

  private boolean shouldNavigateBack;

  private TodosViewModel todosViewModel;

  Unbinder unbinder;

  public boolean isShouldNavigateBack() {
    return shouldNavigateBack;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    todosViewModel = ViewModelProviders.of(getActivity()).get(TodosViewModel.class);
    todo = todosViewModel.getTodo();

    initDueDate(todo);
    initReminderDateTime(todo);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
		View view = inflater.inflate(R.layout.fragment_modifytodo, container, false);
		unbinder = ButterKnife.bind(this, view);

    setTvDueDateText(dueDate);
    setTvReminderDateTimeText(reminderDateTime);
    AppController.setText(todo.getTitle(), tietTitle, tilTitle);
    switchPriority.setChecked(todo.isPriority());
    AppController.setText(todo.getDescription(), tietDescription, tilDescription);

	  return view;
  }

  @Override
  public void onResume() {
    super.onResume();

    ((MainActivity)getActivity()).onSetActionBarTitle(getString(R.string.modifytodo_title));
    setClearDueDateVisibility();
    setClearReminderDateTimeVisibility();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
  }

  private void setClearDueDateVisibility() {
    if (dueDate != null)
      btnClearDueDate.setVisibility(View.VISIBLE);
    else
      btnClearDueDate.setVisibility(View.GONE);
  }

  public void handleModifyTodo() {
    View rootView = getView();
    if (rootView != null) {
      hideSoftInput(rootView);

      Todo todo = todosViewModel.getTodo();
      String titleOnUi = tietTitle.getText().toString().trim();

      if (titleOnUi.isEmpty()) {
        String originalTitle = todo.getTitle();
        AppController.setText(originalTitle, tietTitle, tilTitle);
      } else {
        todo = prepareTodo(todo);
        todosViewModel.setTodo(todo);
        ((MainActivity)getActivity()).ModifyTodo();
        shouldNavigateBack = true;
        ((MainActivity)getActivity()).onBackPressed();
      }
    }
  }

  private Todo prepareTodo(Todo todo) {
    String titleOnUi = tietTitle.getText().toString().trim();
    String description = tietDescription.getText().toString().trim();

    boolean priority = switchPriority.isChecked();
    todo.setDueDate(dueDateLong);
    todo.setTitle(titleOnUi);
    todo.setPriority(priority);
    todo.setReminderDateTime(reminderDateTimeLong);
    if (!description.equals("")) {
      todo.setDescription(description);
    }
    todo.setDirty(true);

    return todo;
  }

  private void hideSoftInput(View rootView) {
    switchPriority.setFocusableInTouchMode(true);
    switchPriority.requestFocus();
    FragmentActivity activity = getActivity();
    InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(
        Context.INPUT_METHOD_SERVICE
    );
    IBinder windowToken = rootView.getWindowToken();
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0);
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
    reminderDatePickerDialogFragment.setTargetFragment(ModifyTodoFragment.this, 0);
    reminderDatePickerDialogFragment.setArguments(arguments);
    reminderDatePickerDialogFragment.show(
        getFragmentManager(),
        "ReminderDatePickerDialogFragment"
    );
  }

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

  public void onDeleteReminder() {
    tvReminderDateTime.setText(getString(R.string.all_noreminder));
  }

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

  private void initDueDate(Todo todo) {
    if (todo.getDueDate() != null && todo.getDueDate() != 0) {
      dueDate = Instant.ofEpochMilli(todo.getDueDate()).atZone(ZoneId.systemDefault()).toLocalDate();
      zdtDueDate = dueDate.atStartOfDay(ZoneId.systemDefault());
      dueDateLong = todo.getDueDate();
      dueDateDisp = DateUtils.formatDateTime(
          AppController.getAppContext(),
          dueDateLong,
          DateUtils.FORMAT_SHOW_DATE
              | DateUtils.FORMAT_NUMERIC_DATE
              | DateUtils.FORMAT_SHOW_YEAR
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
          DateUtils.FORMAT_SHOW_DATE
              | DateUtils.FORMAT_NUMERIC_DATE
              | DateUtils.FORMAT_SHOW_YEAR
              | DateUtils.FORMAT_SHOW_TIME
      );
    }
  }

  private void initReminderDateTime(Todo todo) {
    if (todo.getReminderDateTime() != null && todo.getReminderDateTime() != 0) {
      reminderDateTime = Instant.ofEpochMilli(todo.getReminderDateTime())
          .atZone(ZoneId.systemDefault()).toLocalDateTime();
      zdtReminderDateTime = reminderDateTime.atZone(ZoneId.systemDefault());
      reminderDateTimeLong = todo.getReminderDateTime();
      reminderDateTimeDisp = DateUtils.formatDateTime(
          AppController.getAppContext(),
          reminderDateTimeLong,
          DateUtils.FORMAT_SHOW_DATE
              | DateUtils.FORMAT_NUMERIC_DATE
              | DateUtils.FORMAT_SHOW_YEAR
              | DateUtils.FORMAT_SHOW_TIME
      );
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

  @OnClick(R.id.textview_modifytodo_duedate)
  public void onDueDateClick(View view) {
    openDatePickerDialogFragment();
  }

  @OnClick(R.id.textview_modifytodo_reminderdatetime)
  public void onReminderDateTimeClick(View view) {
    openReminderDatePickerDialogFragment();
  }

  @OnClick(R.id.button_modifytodo_clearduedate)
  public void onBtnClearDueDateClick(View view) {
    clearDueDate();
    setClearDueDateVisibility();
  }

  @OnClick(R.id.button_modifytodo_clearreminder)
  public void onBtnClearReminderClick(View view) {
    clearReminder();
    setClearReminderDateTimeVisibility();
  }

}

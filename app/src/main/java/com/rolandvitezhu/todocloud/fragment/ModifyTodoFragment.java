package com.rolandvitezhu.todocloud.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.app.Constant;
import com.rolandvitezhu.todocloud.data.Todo;
import com.rolandvitezhu.todocloud.fragment.DatePickerDialogFragment.IDatePickerDialogFragment;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

public class ModifyTodoFragment extends Fragment implements
    IDatePickerDialogFragment,
    ReminderDatePickerDialogFragment.IReminderDatePickerDialogFragment,
    ReminderTimePickerDialogFragment.IReminderTimePickerDialogFragment {

  private TextInputLayout tilTitle;
  private TextInputEditText tietTitle;
	private SwitchCompat switchPriority;
	private TextView tvDueDate;
  private TextView tvReminderDateTime;
  private Button btnClearDueDate;
  private Button btnClearReminder;
  private TextInputLayout tilDescription;
  private TextInputEditText tietDescription;

  private Todo todo;

  private LocalDate dueDate;
  private ZonedDateTime zdtDueDate;
  private long dueDateLong;
  private String dueDateDisp;

  private LocalDateTime reminderDateTime;
  private ZonedDateTime zdtReminderDateTime;
  private long reminderDateTimeLong;
  private String reminderDateTimeDisp;

  private IModifyTodoFragment listener;
  private IModifyTodoFragmentActionBar actionBarListener;

  private boolean shouldNavigateBack;

  public boolean isShouldNavigateBack() {
    return shouldNavigateBack;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (IModifyTodoFragment) getTargetFragment();
    actionBarListener = (IModifyTodoFragmentActionBar) context;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      todo = (Todo) getArguments().get("todo");
    }
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

    tilTitle = view.findViewById(R.id.textinputlayout_modifytodo_title);
    tietTitle = view.findViewById(R.id.textinputedittext_modifytodo_title);
    switchPriority = view.findViewById(R.id.switch_modifytodo_priority);
    tvDueDate = view.findViewById(R.id.textview_modifytodo_duedate);
    btnClearDueDate = view.findViewById(R.id.button_modifytodo_clearduedate);
    btnClearReminder = view.findViewById(R.id.button_modifytodo_clearreminder);
    tvReminderDateTime = view.findViewById(R.id.textview_modifytodo_reminderdatetime);
    tilDescription = view.findViewById(R.id.textinputlayout_modifytodo_description);
    tietDescription = view.findViewById(
        R.id.textinputedittext_modifytodo_description
    );

    setTvDueDateText(dueDate);
    setTvReminderDateTimeText(reminderDateTime);

    AppController.setText(todo.getTitle(), tietTitle, tilTitle);
    switchPriority.setChecked(todo.isPriority());
//    tvDueDate.setText(simpleDateFormat.format(dueDate));
//    if (!isReminderDateTimeSet) {
//      tvReminderDateTime.setText(R.string.all_noreminder);
//    } else {
//      tvReminderDateTime.setText(reminderDateTimeFormat.format(reminderDateTime));
//    }
    AppController.setText(todo.getDescription(), tietDescription, tilDescription);
    tvDueDate.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        openDatePickerDialogFragment();
      }

    });
    tvReminderDateTime.setOnClickListener(onReminderDateTimeClick);

    btnClearDueDate.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        clearDueDate();
        setClearDueDateVisibility();
      }
    });
    btnClearReminder.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        clearReminder();
        setClearReminderDateTimeVisibility();
      }
    });

	  return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    actionBarListener.onSetActionBarTitle(getString(R.string.modifytodo_title));
    setClearDueDateVisibility();
    setClearReminderDateTimeVisibility();
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
      Bundle arguments = getArguments();
      Todo todo = (Todo) arguments.get("todo");
      String givenTitle = tietTitle.getText().toString().trim();

      if (givenTitle.isEmpty()) {
        String originalTitle = todo.getTitle();
        AppController.setText(originalTitle, tietTitle, tilTitle);
      } else {
        todo = prepareTodo(todo);
        listener.onModifyTodo(todo);
        shouldNavigateBack = true;
        actionBarListener.onBackPressed();
      }
    }
  }

  private Todo prepareTodo(Todo todo) {
    String givenTitle = tietTitle.getText().toString().trim();
    boolean priority = switchPriority.isChecked();
    todo.setDueDate(dueDateLong);
    todo.setTitle(givenTitle);
    todo.setPriority(priority);
    String description = tietDescription.getText().toString().trim();
    todo.setReminderDateTime(reminderDateTimeLong);
    if (!description.equals("")) {
      todo.setDescription(description);
    } else {
      todo.setDescription(null);
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

  private OnClickListener onReminderDateTimeClick = new OnClickListener() {

    @Override
    public void onClick(View v) {
      openReminderDatePickerDialogFragment();
    }

  };

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
//    reminderDateTime = new Date();
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

  public interface IModifyTodoFragment {
		void onModifyTodo(Todo todo);
	}

  public interface IModifyTodoFragmentActionBar {
    void onSetActionBarTitle(String title);
    void onBackPressed();
  }

}

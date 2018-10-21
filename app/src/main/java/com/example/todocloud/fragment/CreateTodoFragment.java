package com.example.todocloud.fragment;

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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import com.example.todocloud.R;
import com.example.todocloud.app.AppController;
import com.example.todocloud.app.Constant;
import com.example.todocloud.data.Todo;
import com.example.todocloud.fragment.DatePickerDialogFragment.IDatePickerDialogFragment;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;

public class CreateTodoFragment extends Fragment implements
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
  private TextInputEditText tietDescription;

  private LocalDate dueDate;
  private ZonedDateTime zdtDueDate;
  private long dueDateLong;
  private String dueDateDisp;

  private LocalDateTime reminderDateTime;
  private ZonedDateTime zdtReminderDateTime;
  private long reminderDateTimeLong;
  private String reminderDateTimeDisp;

	private ICreateTodoFragment listener;
  private ICreateTodoFragmentActionBar actionBarListener;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (ICreateTodoFragment) getTargetFragment();
    actionBarListener = (ICreateTodoFragmentActionBar) context;
  }

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

    tilTitle = view.findViewById(R.id.textinputlayout_createtodo_title);
    tietTitle = view.findViewById(R.id.textinputedittext_createtodo_title);
    switchPriority = view.findViewById(R.id.switch_createtodo_priority);
    tvDueDate = view.findViewById(R.id.textview_createtodo_duedate);
    btnClearDueDate = view.findViewById(R.id.button_createtodo_clearduedate);
    btnClearReminder = view.findViewById(R.id.button_createtodo_clearreminder);
    tvReminderDateTime = view.findViewById(R.id.textview_createtodo_reminderdatetime);
    tietDescription = view.findViewById(
        R.id.textinputedittext_createtodo_description
    );

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

  @Override
  public void onResume() {
    super.onResume();
    actionBarListener.onSetActionBarTitle(getString(R.string.all_createtodo));
    setClearDueDateVisibility();
    setClearReminderDateTimeVisibility();
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
      listener.onCreateTodo(todoToCreate);
      actionBarListener.onBackPressed();
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

  public interface ICreateTodoFragment {
		void onCreateTodo(Todo todo);
	}

  public interface ICreateTodoFragmentActionBar {
    void onSetActionBarTitle(String title);
    void onBackPressed();
  }
	
}

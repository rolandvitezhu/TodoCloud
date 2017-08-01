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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.example.todocloud.R;
import com.example.todocloud.data.Todo;
import com.example.todocloud.fragment.DatePickerDialogFragment.IDatePickerDialogFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreateTodoFragment extends Fragment implements
    IDatePickerDialogFragment,
    ReminderDatePickerDialogFragment.IReminderDatePickerDialogFragment,
    ReminderTimePickerDialogFragment.IReminderTimePickerDialogFragment {

  private TextInputLayout tilTitle;
	private TextInputEditText tietTitle;
	private SwitchCompat switchPriority;
	private TextView tvDueDate;
  private TextView tvReminderDateTime;
  private Date dueDate = new Date();
  private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
      "yyyy.MM.dd.",
      Locale.getDefault()
  );
  private Date reminderDateTime = new Date();
  private SimpleDateFormat reminderDateTimeFormat = new SimpleDateFormat(
      "yyyy.MM.dd HH:mm",
      Locale.getDefault()
  );
	private TextInputEditText tietDescription;

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
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
		View view = inflater.inflate(R.layout.fragment_createtodo, container, false);

    tilTitle = (TextInputLayout) view.findViewById(R.id.tilTitle);
    tietTitle = (TextInputEditText) view.findViewById(R.id.tietTitle);
    switchPriority = (SwitchCompat) view.findViewById(R.id.switchPriority);
    tvDueDate = (TextView) view.findViewById(R.id.tvDueDate);
    tvReminderDateTime = (TextView) view.findViewById(R.id.tvReminderDateTime);
    tietDescription = (TextInputEditText) view.findViewById(R.id.tietDescription);

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
    tvDueDate.setText(simpleDateFormat.format(dueDate));
    tvDueDate.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				openDatePickerDialogFragment();
			}

		});
    tvReminderDateTime.setText(R.string.txtNoReminders);
    tvReminderDateTime.setOnClickListener(onReminderDateTimeClick);

	  return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    actionBarListener.onSetActionBarTitle(getString(R.string.new_todo));
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    String givenTitle = tietTitle.getText().toString().trim();
    if (!givenTitle.isEmpty()) {
      menu.clear();
      inflater.inflate(R.menu.create_todo, menu);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int itemId = item.getItemId();
    if (itemId == R.id.itemCreateTodo) {
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
    todo.setDueDate(simpleDateFormat.format(dueDate));
    if (tvReminderDateTime.getText().equals(getString(R.string.txtNoReminders))) {
      todo.setReminderDateTime("-1");
    } else {
      String reminderDateTime = reminderDateTimeFormat.format(this.reminderDateTime);
      todo.setReminderDateTime(reminderDateTime);
    }
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
    if (givenTitle.isEmpty()) tilTitle.setError(getString(R.string.enter_title));
    else tilTitle.setErrorEnabled(false);
  }

  private void openDatePickerDialogFragment() {
	  Bundle arguments = new Bundle();
    arguments.putSerializable("date", dueDate);
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
    arguments.putSerializable("reminderDate", reminderDateTime);
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
  public void onSelectDate(Date date) {
    dueDate = date;
    String selectedDate = simpleDateFormat.format(date);
    tvDueDate.setText(selectedDate);
  }

  @Override
  public void onSelectReminderDate(Date date) {
    openReminderTimePickerDialogFragment(date);
  }

  private void openReminderTimePickerDialogFragment(Date date) {
    Bundle arguments = new Bundle();
    arguments.putSerializable("reminderDate", date);
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
    reminderDateTime = new Date();
    tvReminderDateTime.setText(getString(R.string.txtNoReminders));
  }

  @Override
  public void onSelectReminderDateTime(Date date) {
    reminderDateTime = date;
    String reminderDateTime = reminderDateTimeFormat.format(date);
    tvReminderDateTime.setText(reminderDateTime);
  }

  public interface ICreateTodoFragment {
		void onCreateTodo(Todo todo);
	}

  public interface ICreateTodoFragmentActionBar {
    void onSetActionBarTitle(String title);
    void onBackPressed();
  }
	
}

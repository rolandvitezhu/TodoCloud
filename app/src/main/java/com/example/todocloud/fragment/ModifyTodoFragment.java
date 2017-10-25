package com.example.todocloud.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.example.todocloud.R;
import com.example.todocloud.app.AppController;
import com.example.todocloud.data.Todo;
import com.example.todocloud.fragment.DatePickerDialogFragment.IDatePickerDialogFragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ModifyTodoFragment extends Fragment implements
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
  private TextInputLayout tilDescription;
  private TextInputEditText tietDescription;

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
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
		View view = inflater.inflate(R.layout.fragment_modifytodo, container, false);

    Todo todo = (Todo) getArguments().get("todo");
    tilTitle = view.findViewById(R.id.textinputlayout_modifytodo_title);
    tietTitle = view.findViewById(R.id.textinputedittext_modifytodo_title);
    switchPriority = view.findViewById(R.id.switch_modifytodo_priority);
    tvDueDate = view.findViewById(R.id.textview_modifytodo_duedate);
    tvReminderDateTime = view.findViewById(R.id.textview_modifytodo_reminderdatetime);
    tilDescription = view.findViewById(R.id.textinputlayout_modifytodo_description);
    tietDescription = view.findViewById(
        R.id.textinputedittext_modifytodo_description
    );

    try {
      dueDate = simpleDateFormat.parse(todo.getDueDate());
    } catch (ParseException e) {
      e.printStackTrace();
    }
    boolean isReminderDateTimeSet = !todo.getReminderDateTime().equals("-1");
    if (isReminderDateTimeSet) {
      try {
        reminderDateTime = reminderDateTimeFormat.parse(todo.getReminderDateTime());
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }

    AppController.setText(todo.getTitle(), tietTitle, tilTitle);
    switchPriority.setChecked(todo.isPriority());
    tvDueDate.setText(simpleDateFormat.format(dueDate));
    if (!isReminderDateTimeSet) {
      tvReminderDateTime.setText(R.string.all_noreminder);
    } else {
      tvReminderDateTime.setText(reminderDateTimeFormat.format(reminderDateTime));
    }
    AppController.setText(todo.getDescription(), tietDescription, tilDescription);
    tvDueDate.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        openDatePickerDialogFragment();
      }

    });
    tvReminderDateTime.setOnClickListener(onReminderDateTimeClick);

	  return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    actionBarListener.onSetActionBarTitle(getString(R.string.modifytodo_title));
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
    String dueDate = simpleDateFormat.format(this.dueDate);
    todo.setTitle(givenTitle);
    todo.setPriority(priority);
    todo.setDueDate(dueDate);
    String description = tietDescription.getText().toString().trim();
    boolean isNoReminderSet = tvReminderDateTime.getText().equals(getString(R.string.all_noreminder));
    if (isNoReminderSet) {
      todo.setReminderDateTime("-1");
    } else {
      todo.setReminderDateTime(reminderDateTimeFormat.format(reminderDateTime));
    }
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
    reminderDatePickerDialogFragment.setTargetFragment(ModifyTodoFragment.this, 0);
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
    tvReminderDateTime.setText(getString(R.string.all_noreminder));
  }

  @Override
  public void onSelectReminderDateTime(Date date) {
    reminderDateTime = date;
    String selectedReminderDateTime = reminderDateTimeFormat.format(date);
    tvReminderDateTime.setText(selectedReminderDateTime);
  }

  public interface IModifyTodoFragment {
		void onModifyTodo(Todo todo);
	}

  public interface IModifyTodoFragmentActionBar {
    void onSetActionBarTitle(String title);
    void onBackPressed();
  }

}

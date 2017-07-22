package com.example.todocloud.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.example.todocloud.R;
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

  private TextInputEditText tietTitle;
	private SwitchCompat switchPriority;
	private TextView tvDueDate;
  private TextView tvReminderDateTime;
  private Date date = new Date();
  private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
      "yyyy.MM.dd.", Locale.getDefault());
  private Date reminderDate = new Date();
  private SimpleDateFormat reminderDateFormat = new SimpleDateFormat(
      "yyyy.MM.dd HH:mm", Locale.getDefault());
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
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.create_todo, container, false);

    Todo todo = (Todo) getArguments().get("todo");
	  tietTitle = (TextInputEditText) view.findViewById(R.id.tietTitle);
    switchPriority = (SwitchCompat) view.findViewById(R.id.switchPriority);
    tvDueDate = (TextView) view.findViewById(R.id.tvDueDate);
    tvReminderDateTime = (TextView) view.findViewById(R.id.tvReminderDateTime);
    tietDescription = (TextInputEditText) view.findViewById(R.id.tietDescription);

    tietTitle.setText(todo.getTitle());
	  switchPriority.setChecked(todo.isPriority());

    // Az esedékesség dátuma beállításra kerül az adatbázisban tárolt String alapján.
    try {
      date = simpleDateFormat.parse(todo.getDueDate());
    } catch (ParseException e) {
      e.printStackTrace();
    }
	  tvDueDate.setText(simpleDateFormat.format(date));

    // Az emlékeztető időpontja beállításra kerül az adatbázisban tárolt String alapján, feltéve
    // hogy van beállítva emlékeztető.
    if (!todo.getReminderDateTime().equals("-1")) {
      try {
        reminderDate = reminderDateFormat.parse(todo.getReminderDateTime());
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }

    // Ha nincs beállítva emlékeztető, akkor ezt jelezzük, egyébként pedig az emlékeztető dátumát
    // állítjuk be.
    if (todo.getReminderDateTime().equals("-1")) {
      tvReminderDateTime.setText(R.string.txtNoReminders);
    } else {
      tvReminderDateTime.setText(reminderDateFormat.format(reminderDate));
    }

    tietDescription.setText(todo.getDescription());

    tvDueDate.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        openDatePickerDialogFragment();
      }

    });
    tvReminderDateTime.setOnClickListener(onRDTClick);

	  return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    actionBarListener.onSetActionBarTitle(getString(R.string.todo));
  }

  /**
   * Megvizsgálja, hogy a title mezőt kitöltötték-e. Ha nem, akkor az eredeti title tulajdonságot
   * állítja be. Ha a title mező ki van töltve, akkor a Todo frissítésre kerül az aktuálisan mega-
   * dott adatokkal.
   */
  public void updateTodo() {
    if (getView() != null) {

      switchPriority.setFocusableInTouchMode(true);
      switchPriority.requestFocus();
      InputMethodManager inputMethodManager = (InputMethodManager) getActivity().
          getSystemService(Context.INPUT_METHOD_SERVICE);
      inputMethodManager.hideSoftInputFromWindow(getView().getWindowToken(), 0);

      Todo todo = (Todo) getArguments().get("todo");
      if (todo != null) {
        if (tietTitle.getText().toString().trim().isEmpty())
          tietTitle.setText(todo.getTitle());
        else {

          // Todo objektum beállítása az aktuális állapotra.
          todo.setTitle(tietTitle.getText().toString().trim());
          todo.setPriority(switchPriority.isChecked());
          todo.setDueDate(simpleDateFormat.format(date));
          // Ha nincs beállítva értesítés, akkor "-1"-et tárolunk.
          if (tvReminderDateTime.getText().equals(getString(R.string.txtNoReminders))) {
            todo.setReminderDateTime("-1");
          } else {
            todo.setReminderDateTime(reminderDateFormat.format(reminderDate));
          }

          if (!tietDescription.getText().toString().trim().equals("")) {
            todo.setDescription(tietDescription.getText().toString());
          } else {
            todo.setDescription(null);
          }

          todo.setDirty(true);

          // A Todo frissítése az sqlitedb-ben és a TodoListFragment frissítése.
          listener.onModifyTodo(todo);

          shouldNavigateBack = true;
          actionBarListener.onBackPressed();
        }
      }
    }
  }

  private void openDatePickerDialogFragment() {
    Bundle arguments = new Bundle();
    arguments.putSerializable("date", date);
    DatePickerDialogFragment datePickerDialogFragment = new DatePickerDialogFragment();
    datePickerDialogFragment.setTargetFragment(this, 0);
	  datePickerDialogFragment.setArguments(arguments);
	  datePickerDialogFragment.show(getFragmentManager(), "DatePickerDialogFragment");
  }

  /**
   * A ReminderDatePickerDialogFragment-et jeleníti meg.
   */
  private OnClickListener onRDTClick = new OnClickListener() {

    @Override
    public void onClick(View v) {
      ReminderDatePickerDialogFragment reminderDatePickerDialogFragment =
          new ReminderDatePickerDialogFragment();
      reminderDatePickerDialogFragment.setTargetFragment(ModifyTodoFragment.this, 0);
      Bundle bundle = new Bundle();
      bundle.putSerializable("reminderDate", reminderDate);
      reminderDatePickerDialogFragment.setArguments(bundle);
      reminderDatePickerDialogFragment.show(
          getFragmentManager(), "ReminderDatePickerDialogFragment");
    }

  };

  /**
   * Beállítja a View-n a megadott Date-et.
   * @param date A megadott Date.
   */
	@Override
  public void onSelectDate(Date date) {
    this.date = date;
		tvDueDate.setText(simpleDateFormat.format(date));
  }

  /**
   * A ReminderTimePickerDialogFragment-et jeleníti meg és átadja neki a megadott Date-et.
   * @param date A megadott Date.
   */
  @Override
  public void onSelectReminderDate(Date date) {
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

  /**
   * Törli az emlékeztetőt.
   */
  @Override
  public void onDeleteReminder() {
    reminderDate = new Date();
    tvReminderDateTime.setText(getString(R.string.txtNoReminders));
  }

  /**
   * Beállítja az emlékeztetőt a megadott Date alapján.
   * @param date A megadott Date.
   */
  @Override
  public void onSelectReminderDateTime(Date date) {
    reminderDate = date;
    tvReminderDateTime.setText(reminderDateFormat.format(date));
  }

  public interface IModifyTodoFragment {
		void onModifyTodo(Todo todoToModify);
	}

  public interface IModifyTodoFragmentActionBar {
    void onSetActionBarTitle(String title);
    void onBackPressed();
  }

}

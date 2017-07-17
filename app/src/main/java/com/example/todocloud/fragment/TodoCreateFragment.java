package com.example.todocloud.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
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

public class TodoCreateFragment extends Fragment implements IDatePickerDialogFragment,
    ReminderDatePickerDialogFragment.IReminderDatePickerDialogFragment,
    ReminderTimePickerDialogFragment.IReminderTimePickerDialogFragment {

  private TextInputLayout tilTitle;
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

	private ITodoCreateFragment listener;
  private ITodoCreateFragmentActionBar actionBarListener;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (ITodoCreateFragment) getTargetFragment();
    actionBarListener = (ITodoCreateFragmentActionBar) context;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.create_todo, container, false);

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
    tvDueDate.setText(simpleDateFormat.format(date));
    tvDueDate.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showDatePickerDialog();
			}

		});
    tvReminderDateTime.setText(R.string.txtNoReminders);
    tvReminderDateTime.setOnClickListener(onRDTClick);

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
    if (!tietTitle.getText().toString().trim().isEmpty()) {
      menu.clear();
      inflater.inflate(R.menu.todo_create, menu);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.itemCreateTodo) {
      // Szoftveres billentyűzet elrejtése.
      InputMethodManager inputMethodManager = (InputMethodManager) getActivity().
          getSystemService(Context.INPUT_METHOD_SERVICE);
      if (getActivity().getCurrentFocus() != null)
        inputMethodManager.hideSoftInputFromWindow(
            getActivity().getCurrentFocus().getWindowToken(), 0);

      // Létrehozandó tennivaló összeállítása a megadott adatok alapján.
      Todo todo = new Todo();
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

      todo.setCompleted(false);
      todo.setRowVersion(0);
      todo.setDeleted(false);
      todo.setDirty(true);

      // Tennivaló létrehozása.
      listener.onCreateTodo(todo);
      actionBarListener.onBackPressed();
    }
    return super.onOptionsItemSelected(item);
  }

  /**
   * Validálja a Title mezőt.
   */
  private void validateTitle() {
    getActivity().invalidateOptionsMenu();
    if (tietTitle.getText().toString().trim().isEmpty())
      tilTitle.setError(getString(R.string.enter_title));
    else
      tilTitle.setErrorEnabled(false);
  }

  /**
   * DatePickerDialogFragment-et jelenít meg.
   */
	private void showDatePickerDialog() {
	  DatePickerDialogFragment datePickerDialogFragment = new DatePickerDialogFragment();
	  datePickerDialogFragment.setTargetFragment(this, 0);
	  Bundle bundle = new Bundle();
	  bundle.putSerializable("date", date);
	  datePickerDialogFragment.setArguments(bundle);
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
      reminderDatePickerDialogFragment.setTargetFragment(TodoCreateFragment.this, 0);
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
    ReminderTimePickerDialogFragment timePickerDialogFragment =
        new ReminderTimePickerDialogFragment();
    timePickerDialogFragment.setTargetFragment(this, 0);
    Bundle bundle = new Bundle();
    bundle.putSerializable("reminderDate", date);
    timePickerDialogFragment.setArguments(bundle);
    timePickerDialogFragment.show(getFragmentManager(), "ReminderTimePickerDialogFragment");
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

  public interface ITodoCreateFragment {
		void onCreateTodo(Todo todoToCreate);
	}

  public interface ITodoCreateFragmentActionBar {
    void onSetActionBarTitle(String title);
    void onBackPressed();
  }
	
}

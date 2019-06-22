package com.rolandvitezhu.todocloud.fragment;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialogFragment;
import android.widget.DatePicker;

import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.app.Constant;

import org.threeten.bp.LocalDate;

public class DatePickerDialogFragment extends AppCompatDialogFragment implements
    DatePickerDialog.OnDateSetListener {
	
	private int year;
	private int month;
	private int day;
  private LocalDate date;

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    boolean thereIsNoDate = getArguments() == null || getArguments().get(Constant.DUE_DATE) == null;

    if (thereIsNoDate)
      date = LocalDate.now();
    else
      date = (LocalDate) getArguments().get(Constant.DUE_DATE);

    year = date.getYear();
    month = date.getMonthValue();
    day = date.getDayOfMonth();

	  return new DatePickerDialog(
        getActivity(), R.style.MyPickerDialogTheme, this, year, month - 1, day
    );
  }

  @Override
  public void onDateSet(DatePicker view, int year, int month, int day) {
    date = LocalDate.of(year, month + 1, day);

    if (getTargetFragment() instanceof CreateTodoFragment)
      ((CreateTodoFragment)getTargetFragment()).onSelectDate(date);
    else if (getTargetFragment() instanceof ModifyTodoFragment)
      ((ModifyTodoFragment)getTargetFragment()).onSelectDate(date);

    dismiss();
  }

}

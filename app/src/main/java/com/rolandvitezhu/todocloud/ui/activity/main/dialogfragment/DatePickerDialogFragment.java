package com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.DatePicker;

import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.app.Constant;
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.CreateTodoFragment;
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.ModifyTodoFragment;

import org.threeten.bp.LocalDate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;

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

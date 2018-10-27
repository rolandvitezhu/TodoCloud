package com.rolandvitezhu.todocloud.fragment;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
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
	private IDatePickerDialogFragment listener;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (IDatePickerDialogFragment) getTargetFragment();
  }

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
    listener.onSelectDate(date);
	  dismiss();
  }

  public interface IDatePickerDialogFragment {
		void onSelectDate(LocalDate date);
	}

}

package com.example.todocloud.fragment;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialogFragment;
import android.widget.DatePicker;

import com.example.todocloud.R;

import java.util.Calendar;
import java.util.Date;

public class DatePickerDialogFragment extends AppCompatDialogFragment implements
    DatePickerDialog.OnDateSetListener {
	
	private int year;
	private int month;
	private int day;
  private Date date;
  private Calendar calendar = Calendar.getInstance();
	private IDatePickerDialogFragment listener;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (IDatePickerDialogFragment) getTargetFragment();
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
		date = (Date) getArguments().get("date");
    calendar.setTime(date);
    year = calendar.get(Calendar.YEAR);
    month = calendar.get(Calendar.MONTH);
    day = calendar.get(Calendar.DAY_OF_MONTH);
	  return new DatePickerDialog(
        getActivity(), R.style.MyPickerDialogTheme, this, year, month, day
    );
  }

  @Override
  public void onDateSet(DatePicker view, int year, int month, int day) {
    calendar.set(year, month, day);
    date.setTime(calendar.getTimeInMillis());
    listener.onSelectDate(date);
	  dismiss();
  }

  public interface IDatePickerDialogFragment {
		void onSelectDate(Date date);
	}

}

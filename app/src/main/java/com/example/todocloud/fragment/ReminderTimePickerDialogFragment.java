package com.example.todocloud.fragment;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;
import android.widget.TimePicker;

import com.example.todocloud.R;

import java.util.Calendar;
import java.util.Date;

public class ReminderTimePickerDialogFragment extends AppCompatDialogFragment implements TimePickerDialog.OnTimeSetListener {

  private int hour;
  private int minute;
  private Date date;
  private Calendar calendar = Calendar.getInstance();
  private IReminderTimePickerDialogFragment listener;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (IReminderTimePickerDialogFragment) getTargetFragment();
  }

  /**
   * Létrehoz egy időpontválasztó dialógust az átadott dátum alapján.
   */
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    date = (Date) getArguments().get("reminderDate");
    calendar.setTime(date);
    hour = calendar.get(Calendar.HOUR_OF_DAY);
    minute = calendar.get(Calendar.MINUTE);
    return new TimePickerDialog(
        getActivity(), R.style.MyPickerDialogTheme, this, hour, minute, true);
  }

  /**
   * Átadja a kiválasztott időt.
   */
  @Override
  public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
    calendar.set(Calendar.MINUTE, minute);
    date.setTime(calendar.getTimeInMillis());
    listener.onSelectReminderDateTime(date);
    dismiss();
  }

  public interface IReminderTimePickerDialogFragment {
    void onSelectReminderDateTime(Date date);
  }

}

package com.example.todocloud.fragment;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialogFragment;
import android.widget.DatePicker;

import com.example.todocloud.R;

import java.util.Calendar;
import java.util.Date;

public class ReminderDatePickerDialogFragment extends AppCompatDialogFragment implements
    DatePickerDialog.OnDateSetListener {

  private int year;
  private int month;
  private int day;
  private Date date;
  private Calendar calendar = Calendar.getInstance();
  private IReminderDatePickerDialogFragment listener;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (IReminderDatePickerDialogFragment) getTargetFragment();
  }

  /**
   * Létrehoz egy dátumválasztó dialógust az átadott dátum alapján.
   */
  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    date = (Date) getArguments().get("reminderDate");
    calendar.setTime(date);
    year = calendar.get(Calendar.YEAR);
    month = calendar.get(Calendar.MONTH);
    day = calendar.get(Calendar.DAY_OF_MONTH);
    DatePickerDialog datePickerDialog = new DatePickerDialog(
        getActivity(), R.style.MyPickerDialogTheme, this, year, month, day);
    datePickerDialog.setButton(DialogInterface.BUTTON_POSITIVE,
        getString(R.string.btnTime), datePickerDialog);
    datePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
        getString(R.string.btnDeleteReminder), new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        listener.onDeleteReminder();
      }
    });
    return datePickerDialog;
  }

  /**
   * Átadja a kiválasztott dátumot.
   */
  @Override
  public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
    calendar.set(year, monthOfYear, dayOfMonth);
    date.setTime(calendar.getTimeInMillis());
    listener.onReminderDateSelected(date);
    dismiss();
  }

  /**
   * Interfész, a ReminderDatePickerDialogFragment-et meghívó Fragment-ekkel való kommunikációra.
   */
  public interface IReminderDatePickerDialogFragment {
    void onReminderDateSelected(Date date);
    void onDeleteReminder();
  }

}

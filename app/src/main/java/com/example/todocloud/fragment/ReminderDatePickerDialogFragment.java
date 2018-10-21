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
import com.example.todocloud.app.Constant;

import org.threeten.bp.LocalDateTime;

public class ReminderDatePickerDialogFragment extends AppCompatDialogFragment implements
    DatePickerDialog.OnDateSetListener {

  private int year;
  private int month;
  private int day;
  private LocalDateTime date;
  private IReminderDatePickerDialogFragment listener;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (IReminderDatePickerDialogFragment) getTargetFragment();
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    boolean thereIsNoDateTime = getArguments() == null || getArguments().get(Constant.REMINDER_DATE_TIME) == null;

    if (thereIsNoDateTime)
      date = LocalDateTime.now();
    else
      date = (LocalDateTime) getArguments().get(Constant.REMINDER_DATE_TIME);

    year = date.getYear();
    month = date.getMonthValue();
    day = date.getDayOfMonth();

    DatePickerDialog datePickerDialog = new DatePickerDialog(
        getActivity(), R.style.MyPickerDialogTheme, this, year, month - 1, day
    );
    prepareDatePickerDialogButtons(datePickerDialog);
    return datePickerDialog;
  }

  private void prepareDatePickerDialogButtons(DatePickerDialog datePickerDialog) {
    datePickerDialog.setButton(
        DialogInterface.BUTTON_POSITIVE,
        getString(R.string.reminderdatepicker_positivebuttontext),
        datePickerDialog
    );
//    datePickerDialog.setButton(
//        DialogInterface.BUTTON_NEGATIVE,
//        getString(R.string.all_delete),
//        new DialogInterface.OnClickListener() {
//
//          @Override
//          public void onClick(DialogInterface dialog, int which) {
//            listener.onDeleteReminder();
//          }
//
//        }
//    );
  }

  @Override
  public void onDateSet(DatePicker view, int year, int month, int day) {
    date = LocalDateTime.of(year, month + 1, day, date.getHour(), date.getMinute());
    listener.onSelectReminderDate(date);
    dismiss();
  }

  public interface IReminderDatePickerDialogFragment {
    void onSelectReminderDate(LocalDateTime date);
    void onDeleteReminder();
  }

}

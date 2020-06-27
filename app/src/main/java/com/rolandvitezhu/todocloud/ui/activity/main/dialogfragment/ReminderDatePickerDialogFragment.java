package com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.DatePicker;

import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.app.Constant;
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.CreateTodoFragment;
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.ModifyTodoFragment;

import org.threeten.bp.LocalDateTime;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;

public class ReminderDatePickerDialogFragment extends AppCompatDialogFragment implements
    DatePickerDialog.OnDateSetListener {

  private int year;
  private int month;
  private int day;
  private LocalDateTime date;

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
  }

  @Override
  public void onDateSet(DatePicker view, int year, int month, int day) {
    date = LocalDateTime.of(year, month + 1, day, date.getHour(), date.getMinute());

    if (getTargetFragment() instanceof CreateTodoFragment)
      ((CreateTodoFragment)getTargetFragment()).onSelectReminderDate(date);
    else if (getTargetFragment() instanceof ModifyTodoFragment)
      ((ModifyTodoFragment)getTargetFragment()).onSelectReminderDate(date);

    dismiss();
  }

}

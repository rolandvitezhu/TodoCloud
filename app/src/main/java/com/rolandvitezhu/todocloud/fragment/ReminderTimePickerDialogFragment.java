package com.rolandvitezhu.todocloud.fragment;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;
import android.widget.TimePicker;

import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.app.Constant;

import org.threeten.bp.LocalDateTime;

public class ReminderTimePickerDialogFragment extends AppCompatDialogFragment implements TimePickerDialog.OnTimeSetListener {

  private int hour;
  private int minute;
  private LocalDateTime date;

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    if (getArguments() != null) {
      date = (LocalDateTime) getArguments().get(Constant.REMINDER_DATE_TIME);

      if (date != null) {
        hour = date.getHour();
        minute = date.getMinute();
      }
    }
    return new TimePickerDialog(
        getActivity(), R.style.MyPickerDialogTheme, this, hour, minute, true
    );
  }

  @Override
  public void onTimeSet(TimePicker view, int hour, int minute) {
    date = LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(), hour, minute);

    if (getTargetFragment() instanceof CreateTodoFragment)
      ((CreateTodoFragment)getTargetFragment()).onSelectReminderDateTime(date);
    else if (getTargetFragment() instanceof ModifyTodoFragment)
      ((ModifyTodoFragment)getTargetFragment()).onSelectReminderDateTime(date);

    dismiss();
  }

}

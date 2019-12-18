package com.rolandvitezhu.todocloud.customcomponent;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

public class CustomisablePreference extends Preference {
  public CustomisablePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  public CustomisablePreference(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public CustomisablePreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public CustomisablePreference(Context context) {
    super(context);
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder holder) {
    super.onBindViewHolder(holder);

    TextView titleView = holder.itemView.findViewById(android.R.id.title);
    titleView.setTextColor(Color.BLACK);
  }
}

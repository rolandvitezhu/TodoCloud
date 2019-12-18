package com.rolandvitezhu.todocloud.customcomponent;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rolandvitezhu.todocloud.R;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

public class CustomisableSwitchPreferenceCompat extends SwitchPreferenceCompat {
  public CustomisableSwitchPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  public CustomisableSwitchPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public CustomisableSwitchPreferenceCompat(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public CustomisableSwitchPreferenceCompat(Context context) {
    super(context);
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder holder) {
    super.onBindViewHolder(holder);

    setTitleTextColor(holder);
    setSwitchTrackColor(holder);
  }

  private void setSwitchTrackColor(PreferenceViewHolder holder) {
    try {
      SwitchCompat switchCompat = ((SwitchCompat)((LinearLayout)((LinearLayout) holder.itemView)
          .getChildAt(2))
          .getChildAt(0));

      if (switchCompat != null)
      {
        if (switchCompat.isChecked())
          switchCompat.getTrackDrawable().setColorFilter(ContextCompat.getColor(getContext(), R.color.colorAccent), PorterDuff.Mode.SRC_IN);
        else
          switchCompat.getTrackDrawable().setColorFilter(ContextCompat.getColor(getContext(), R.color.colorControlNormal), PorterDuff.Mode.SRC_IN);
        switchCompat.getTrackDrawable().setAlpha(76);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void setTitleTextColor(PreferenceViewHolder holder) {
    TextView titleView = holder.itemView.findViewById(android.R.id.title);
    if (titleView != null)
      titleView.setTextColor(Color.BLACK);
  }
}

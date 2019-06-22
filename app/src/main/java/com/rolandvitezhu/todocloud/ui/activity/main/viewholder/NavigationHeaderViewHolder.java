package com.rolandvitezhu.todocloud.ui.activity.main.viewholder;

import android.view.View;
import android.widget.TextView;

import com.rolandvitezhu.todocloud.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class NavigationHeaderViewHolder {

  private Unbinder unbinder;

  @BindView(R.id.textview_navigationdrawerheader_name)
  public TextView name;

  @BindView(R.id.textview_navigationdrawerheader_email)
  public TextView email;

  public NavigationHeaderViewHolder(View view) {
    unbinder = ButterKnife.bind(this, view);
  }

  public void unbind() {
    unbinder.unbind();
  }
}

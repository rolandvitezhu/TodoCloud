package com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.ui.activity.main.MainActivity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class LogoutUserDialogFragment extends AppCompatDialogFragment {

  Unbinder unbinder;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setStyle(STYLE_NORMAL, R.style.MyDialogTheme);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.dialog_logoutuser, container);
    unbinder = ButterKnife.bind(this, view);

    getDialog().setTitle(R.string.logoutuser_title);

    return view;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
  }

  @OnClick(R.id.button_logoutuser_ok)
  public void onBtnOkClick(View view) {
    ((MainActivity)getActivity()).onLogout();
    dismiss();
  }

  @OnClick(R.id.button_logoutuser_cancel)
  public void onBtnCancelClick(View view) {
    dismiss();
  }

}

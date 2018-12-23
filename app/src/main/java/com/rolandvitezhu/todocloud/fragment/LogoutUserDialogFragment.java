package com.rolandvitezhu.todocloud.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.rolandvitezhu.todocloud.R;

import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class LogoutUserDialogFragment extends AppCompatDialogFragment {

  public ILogoutUserDialogFragment listener;

  Unbinder unbinder;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if (getTargetFragment() != null)
      listener = (ILogoutUserDialogFragment) getTargetFragment();
    else
      listener = (ILogoutUserDialogFragment) context;
  }

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
    listener.onLogout();
    dismiss();
  }

  @OnClick(R.id.button_logoutuser_cancel)
  public void onBtnCancelClick(View view) {
    dismiss();
  }

  public interface ILogoutUserDialogFragment {
    void onLogout();
  }

}

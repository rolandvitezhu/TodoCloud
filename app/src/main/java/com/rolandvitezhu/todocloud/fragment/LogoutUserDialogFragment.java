package com.rolandvitezhu.todocloud.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.rolandvitezhu.todocloud.R;

public class LogoutUserDialogFragment extends AppCompatDialogFragment {

  public ILogoutUserDialogFragment listener;

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
    // Beállítja az erőforrásban definiált stílust.
    setStyle(STYLE_NORMAL, R.style.MyDialogTheme);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.dialog_logoutuser, container);
    getDialog().setTitle(R.string.logoutuser_title);

    Button btnOK = (Button) view.findViewById(R.id.button_logoutuser_ok);
    Button btnCancel = (Button) view.findViewById(R.id.button_logoutuser_cancel);

    btnOK.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        listener.onLogout();
        dismiss();
      }

    });
    btnCancel.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        dismiss();
      }

    });
    return view;
  }

  public interface ILogoutUserDialogFragment {
    void onLogout();
  }

}

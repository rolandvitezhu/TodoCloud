package com.example.todocloud.fragment;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import com.example.todocloud.R;
import com.example.todocloud.data.List;

public class CreateListDialogFragment extends AppCompatDialogFragment {

  private TextInputLayout tilTitle;
  private TextInputEditText tietTitle;
  private Button btnOK;
  private Button btnCancel;

  private ICreateListDialogFragment listener;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (ICreateListDialogFragment) getTargetFragment();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setStyle(STYLE_NORMAL, R.style.MyDialogTheme);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    View view = inflater.inflate(R.layout.dialog_createlist, container);
    Dialog dialog = getDialog();
    dialog.setTitle(R.string.all_createlist);
    setSoftInputMode();

    tilTitle = (TextInputLayout) view.findViewById(R.id.textinputlayout_createlist_title);
    tietTitle = (TextInputEditText) view.findViewById(R.id.textinputedittext_createlist_title);
    btnOK = (Button) view.findViewById(R.id.button_createlist_ok);
    btnCancel = (Button) view.findViewById(R.id.button_createlist_cancel);

    applyTextChangedEvents();
    applyEditorEvents();
    applyClickEvents();

    return view;
  }

  private void setSoftInputMode() {
    Dialog dialog = getDialog();
    Window window = dialog.getWindow();
    if (window != null) {
      int hiddenSoftInputAtOpenDialog = WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN;
      int softInputNotCoverFooterButtons = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
      window.setSoftInputMode(softInputNotCoverFooterButtons | hiddenSoftInputAtOpenDialog);
    }
  }

  private void applyTextChangedEvents() {
    tietTitle.addTextChangedListener(new TextWatcher() {

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {

      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {

      }

      @Override
      public void afterTextChanged(Editable s) {
        validateTitle();
      }

    });
  }

  private void applyEditorEvents() {
    tietTitle.setOnEditorActionListener(new TextView.OnEditorActionListener() {

      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        boolean pressDone = actionId == EditorInfo.IME_ACTION_DONE;
        boolean pressEnter = false;
        if (event != null) {
          int keyCode = event.getKeyCode();
          pressEnter = keyCode == KeyEvent.KEYCODE_ENTER;
        }

        if (pressEnter || pressDone) {
          btnOK.performClick();
          return true;
        }
        return false;
      }

    });
  }

  private void applyClickEvents() {
    btnOK.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        String givenTitle = tietTitle.getText().toString().trim();

        if (validateTitle()) {
          List listToCreate = prepareListToCreate(givenTitle);
          listener.onCreateList(listToCreate);
          dismiss();
        }
      }

      @NonNull
      private List prepareListToCreate(String givenTitle) {
        List listToCreate = new List();
        listToCreate.setTitle(givenTitle);
        listToCreate.setRowVersion(0);
        listToCreate.setDeleted(false);
        listToCreate.setDirty(true);
        return listToCreate;
      }

    });
    btnCancel.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        dismiss();
      }
    });
  }

  private boolean validateTitle() {
    String givenTitle = tietTitle.getText().toString().trim();
    if (givenTitle.isEmpty()) {
      tilTitle.setError(getString(R.string.all_entertitle));
      return false;
    } else {
      tilTitle.setErrorEnabled(false);
      return true;
    }
  }

  public interface ICreateListDialogFragment {
    void onCreateList(List list);
  }

}

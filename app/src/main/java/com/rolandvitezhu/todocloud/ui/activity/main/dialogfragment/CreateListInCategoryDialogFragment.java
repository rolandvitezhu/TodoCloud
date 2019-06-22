package com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment;

import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
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

import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.data.List;
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.MainListFragment;
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.ListsViewModel;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class CreateListInCategoryDialogFragment extends AppCompatDialogFragment {

  @BindView(R.id.textinputlayout_createlistincategory_title)
  TextInputLayout tilTitle;
  @BindView(R.id.textinputedittext_createlistincategory_title)
  TextInputEditText tietTitle;
  @BindView(R.id.button_createlistincategory_ok)
  Button btnOK;

  private ListsViewModel listsViewModel;

  Unbinder unbinder;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setStyle(STYLE_NORMAL, R.style.MyDialogTheme);
    listsViewModel = ViewModelProviders.of(getActivity()).get(ListsViewModel.class);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    View view = inflater.inflate(R.layout.dialog_createlistincategory, container);
    unbinder = ButterKnife.bind(this, view);

    Dialog dialog = getDialog();
    dialog.setTitle(R.string.all_createlist);
    setSoftInputMode();

    applyTextChangedEvents();
    applyEditorActionEvents();

    return view;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
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

  private void applyEditorActionEvents() {
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

  @NonNull
  private List prepareListToCreate(String givenTitle) {
    List listToCreate = new List();
    listToCreate.setTitle(givenTitle);
    listToCreate.setRowVersion(0);
    listToCreate.setDeleted(false);
    listToCreate.setDirty(true);
    return listToCreate;
  }

  @OnClick(R.id.button_createlistincategory_ok)
  public void onBtnOkClick(View view) {
    String givenTitle = tietTitle.getText().toString().trim();

    if (validateTitle()) {
      List listToCreate = prepareListToCreate(givenTitle);
      listsViewModel.setList(listToCreate);
      ((MainListFragment)getTargetFragment()).onCreateListInCategory();
      dismiss();
    }
  }

  @OnClick(R.id.button_createlistincategory_cancel)
  public void onBtnCancelClick(View view) {
    dismiss();
  }

}

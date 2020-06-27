package com.rolandvitezhu.todocloud.ui.activity.main.dialogfragment;

import android.app.Dialog;
import android.os.Bundle;
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

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.data.Category;
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.MainListFragment;
import com.rolandvitezhu.todocloud.ui.activity.main.viewmodel.CategoriesViewModel;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class CreateCategoryDialogFragment extends AppCompatDialogFragment {

  @BindView(R.id.textinputlayout_createcategory_title)
  TextInputLayout tilTitle;
  @BindView(R.id.textinputedittext_createcategory_title)
  TextInputEditText tietTitle;
  @BindView(R.id.button_createcategory_ok)
  Button btnOK;

  private CategoriesViewModel categoriesViewModel;

  Unbinder unbinder;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setStyle(STYLE_NORMAL, R.style.MyDialogTheme);
    categoriesViewModel = ViewModelProviders.of(getActivity()).get(CategoriesViewModel.class);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    View view = inflater.inflate(R.layout.dialog_createcategory, container);
    unbinder = ButterKnife.bind(this, view);

    Dialog dialog = getDialog();
    dialog.setTitle(R.string.all_createcategory);
    setSoftInputMode();

    applyTextChangeEvent();
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

  private void applyTextChangeEvent() {
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
  private Category prepareCategoryToCreate(String givenTitle) {
    Category categoryToCreate = new Category();

    categoryToCreate.setTitle(givenTitle);
    categoryToCreate.setRowVersion(0);
    categoryToCreate.setDeleted(false);
    categoryToCreate.setDirty(true);
    categoryToCreate.setPosition(5.0);

    return categoryToCreate;
  }

  @OnClick(R.id.button_createcategory_ok)
  public void onBtnOkClick(View view) {
    String givenTitle = tietTitle.getText().toString().trim();

    if (validateTitle()) {
      Category categoryToCreate = prepareCategoryToCreate(givenTitle);
      categoriesViewModel.setCategory(categoryToCreate);
      ((MainListFragment)getTargetFragment()).onCreateCategory();
      dismiss();
    }
  }

  @OnClick(R.id.button_createcategory_cancel)
  public void onBtnCancelClick(View view) {
    dismiss();
  }

}

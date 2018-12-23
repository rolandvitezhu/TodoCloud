package com.rolandvitezhu.todocloud.fragment;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.app.AppController;
import com.rolandvitezhu.todocloud.data.Category;
import com.rolandvitezhu.todocloud.data.List;
import com.rolandvitezhu.todocloud.datastorage.DbLoader;

import java.util.ArrayList;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class MoveListDialogFragment extends AppCompatDialogFragment {

  @Inject
  DbLoader dbLoader;

  @BindView(R.id.spinner_movelist_category)
  Spinner spinnerCategory;

  private IMoveListDialogFragment listener;
  private ArrayList<Category> categoriesFor;

  Unbinder unbinder;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (IMoveListDialogFragment) getTargetFragment();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ((AppController) getActivity().getApplication()).getAppComponent().inject(this);

    setStyle(STYLE_NORMAL, R.style.MyDialogTheme);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    View view = inflater.inflate(R.layout.dialog_movelist, container);
    unbinder = ButterKnife.bind(this, view);

    Dialog dialog = getDialog();
    dialog.setTitle(R.string.movelist_title);
    setSoftInputMode();

    prepareSpinner();

    return view;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
  }

  private void prepareSpinner() {
    Category categoryForListWithoutCategory = new Category(
        getString(R.string.movelist_spinneritemlistnotincategory)
    );
    Category categoryOriginallyRelatedToList = (Category) getArguments().get("category");
    ArrayList<Category> realCategoriesFromDatabase = dbLoader.getCategories();

    ArrayList<Category> categoriesForSpinner = new ArrayList<>();
    categoriesForSpinner.add(categoryForListWithoutCategory);
    categoriesForSpinner.addAll(realCategoriesFromDatabase);

    spinnerCategory.setAdapter(new ArrayAdapter<>(
        getActivity(),
        android.R.layout.simple_spinner_item,
        categoriesForSpinner
    ));
    int categoryOriginallyRelatedToListPosition = categoriesForSpinner.indexOf(categoryOriginallyRelatedToList);
    spinnerCategory.setSelection(categoryOriginallyRelatedToListPosition);
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

  @OnClick(R.id.button_movelist_ok)
  public void onBtnOkClick(View view) {
    Category category = (Category) getArguments().get("category");
    boolean isListNotInCategoryBeforeMove = category.getCategoryOnlineId() == null;
    List list = getArguments().getParcelable("list");
    Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
    String categoryOnlineId = selectedCategory.getCategoryOnlineId();
    listener.onMoveList(list, categoryOnlineId, isListNotInCategoryBeforeMove);
    dismiss();
  }

  @OnClick(R.id.button_movelist_cancel)
  public void onBtnCancelClick(View view) {
    dismiss();
  }

  public interface IMoveListDialogFragment {
    void onMoveList(List list, String categoryOnlineId, boolean isListNotInCategoryBeforeMove);
  }

}

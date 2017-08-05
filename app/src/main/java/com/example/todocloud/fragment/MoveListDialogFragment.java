package com.example.todocloud.fragment;

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
import android.widget.Button;
import android.widget.Spinner;

import com.example.todocloud.R;
import com.example.todocloud.data.Category;
import com.example.todocloud.data.List;
import com.example.todocloud.datastorage.DbLoader;

import java.util.ArrayList;

public class MoveListDialogFragment extends AppCompatDialogFragment {

  private Spinner spnrCategory;
  private Button btnOK;
  private Button btnCancel;

  private IMoveListDialogFragment listener;
  private ArrayList<Category> categoriesFor;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (IMoveListDialogFragment) getTargetFragment();
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
    View view = inflater.inflate(R.layout.dialog_movelist, container);
    Dialog dialog = getDialog();
    dialog.setTitle(R.string.movelist_title);
    setSoftInputMode();

    spnrCategory = (Spinner) view.findViewById(R.id.listCategory);
    btnOK = (Button) view.findViewById(R.id.btnOKMoveList);
    btnCancel = (Button) view.findViewById(R.id.btnCancelMoveList);

    prepareSpinner();
    applyClickEvents();

    return view;
  }

  private void prepareSpinner() {
    DbLoader dbLoader = new DbLoader();
    Category categoryForListWithoutCategory = new Category(
        getString(R.string.movelist_spinneritemlistnotincategory)
    );
    Category categoryOriginallyRelatedToList = (Category) getArguments().get("category");
    ArrayList<Category> realCategoriesFromDatabase = dbLoader.getCategories();

    ArrayList<Category> categoriesForSpinner = new ArrayList<>();
    categoriesForSpinner.add(categoryForListWithoutCategory);
    categoriesForSpinner.addAll(realCategoriesFromDatabase);

    spnrCategory.setAdapter(new ArrayAdapter<>(
        getActivity(),
        android.R.layout.simple_spinner_item,
        categoriesForSpinner
    ));
    int categoryOriginallyRelatedToListPosition = categoriesForSpinner.indexOf(categoryOriginallyRelatedToList);
    spnrCategory.setSelection(categoryOriginallyRelatedToListPosition);
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

  private void applyClickEvents() {
    btnOK.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        Category category = (Category) getArguments().get("category");
        boolean isListNotInCategoryBeforeMove = category.getCategoryOnlineId() == null;
        List list = getArguments().getParcelable("list");
        Category selectedCategory = (Category) spnrCategory.getSelectedItem();
        String categoryOnlineId = selectedCategory.getCategoryOnlineId();
        listener.onMoveList(list, categoryOnlineId, isListNotInCategoryBeforeMove);
        dismiss();
      }

    });
    btnCancel.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        dismiss();
      }

    });
  }

  public interface IMoveListDialogFragment {
    void onMoveList(List list, String categoryOnlineId, boolean isListNotInCategoryBeforeMove);
  }

}

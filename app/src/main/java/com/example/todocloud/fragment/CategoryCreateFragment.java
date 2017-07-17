package com.example.todocloud.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import com.example.todocloud.R;
import com.example.todocloud.data.Category;

public class CategoryCreateFragment extends AppCompatDialogFragment {

  private TextInputLayout tilTitle;
  private TextInputEditText tietTitle;
  private ICategoryCreateFragment listener;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (ICategoryCreateFragment) getTargetFragment();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Beállítja az erőforrásban definiált stílust.
    setStyle(STYLE_NORMAL, R.style.MyDialogTheme);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.create_category, container);
    getDialog().setTitle(R.string.itemCreateCategory);

    // A footer gombok a szoftveres billentyűzet használata alatt is láthatók.
    // A szoftveres billentyűzet nem jelenik meg alapértelmezetten.
    if (getDialog().getWindow() != null)
      getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
          WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

    tilTitle = (TextInputLayout) view.findViewById(R.id.tilTitle);
    tietTitle = (TextInputEditText) view.findViewById(R.id.tietTitle);
    final Button btnOK = (Button) view.findViewById(R.id.btnOK);
    Button btnCancel = (Button) view.findViewById(R.id.btnCancel);

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
    tietTitle.setOnEditorActionListener(new TextView.OnEditorActionListener() {

      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))
            || (actionId == EditorInfo.IME_ACTION_DONE)) {
          btnOK.performClick();
          return true;
        }
        return false;
      }

    });
    btnOK.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        String title = tietTitle.getText().toString().trim();

        if (validateTitle()) {
          Category category = new Category();
          category.setTitle(title);

          category.setRowVersion(0);
          category.setDeleted(false);
          category.setDirty(true);

          listener.onCreateCategory(category);
          dismiss();
        }
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

  /**
   * Validálja a Title mezőt.
   * @return Kitöltött mező esetén true, egyébként false.
   */
  private boolean validateTitle() {
    if (tietTitle.getText().toString().trim().isEmpty()) {
      tilTitle.setError(getString(R.string.enter_title));
      return false;
    } else {
      tilTitle.setErrorEnabled(false);
      return true;
    }
  }

  public interface ICategoryCreateFragment {
    void onCreateCategory(Category category);
  }

}

package com.example.todocloud.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
  private IMoveListDialogFragment listener;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (IMoveListDialogFragment) getTargetFragment();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Beállítja az erőforrásban definiált stílust.
    setStyle(STYLE_NORMAL, R.style.MyDialogTheme);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.move_list, container);
    getDialog().setTitle(R.string.itemMoveList);

    // A footer gombok a szoftveres billentyűzet használata alatt is láthatók.
    // A szoftveres billentyűzet nem jelenik meg alapértelmezetten.
    getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
        WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

    spnrCategory = (Spinner) view.findViewById(R.id.listCategory);
    final DbLoader dbLoader = new DbLoader();
    ArrayList<Category> categories = new ArrayList<Category>();
    categories.add(new Category(getString(R.string.itemListWithoutCategory)));
    categories.addAll(dbLoader.getCategories());
    spnrCategory.setAdapter(new ArrayAdapter<>(getActivity(),
        android.R.layout.simple_spinner_item, categories));
    spnrCategory.setSelection(categories.indexOf(getArguments().get("category")));
    Button btnOK = (Button) view.findViewById(R.id.btnOKMoveList);
    btnOK.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        boolean listIsNotInCategory = false;
        // Ha a kapott category categoryOnlineId-ja null, akkor kategória nélküli listáról van szó.
        // Ez esetben pedig a ListInCategory táblában nincs hozzá tartozó bejegyzés, létre kell azt
        // hozni. Tehát listát helyezünk kategóriába.
        if (((Category) getArguments().get("category")).getCategoryOnlineId() == null) {
          listIsNotInCategory = true;
        }
        List list = getArguments().getParcelable("list");
        String categoryOnlineId = ((Category) spnrCategory.getSelectedItem()).
            getCategoryOnlineId();
        listener.onMoveList(list, categoryOnlineId, listIsNotInCategory);
        dismiss();
      }

    });
    Button btnCancel = (Button) view.findViewById(R.id.btnCancelMoveList);
    btnCancel.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        dismiss();
      }
    });
    return view;
  }

  public interface IMoveListDialogFragment {
    void onMoveList(List list, String categoryOnlineId, boolean listIsNotInCategory);
  }

}

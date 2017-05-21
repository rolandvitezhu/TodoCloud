package com.example.todocloud.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.todocloud.R;
import com.example.todocloud.data.Todo;

import java.util.ArrayList;

public class ConfirmDeleteDialogFragment extends AppCompatDialogFragment {

  private String type;
  private boolean many;
  private ArrayList items;
  private IDeleteFragment listener;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    listener = (IDeleteFragment) getTargetFragment();
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
    View view = inflater.inflate(R.layout.delete, container);

    TextView tvTitle = (TextView) view.findViewById(R.id.tvTitle);
    Button btnOK = (Button) view.findViewById(R.id.btnOK);
    Button btnCancel = (Button) view.findViewById(R.id.btnCancel);

    // A many flag, és a DialogFragment szövegezésének beállítása az elkért argumentumok alapján.
    type = getArguments().getString("type");
    items = getArguments().getParcelableArrayList("items");
    if (items != null) many = true;
    String title;
    String itemTitle = getArguments().getString("title");
    if (type != null) {
      switch (type) {
        case "todo":
          if (isMany()) {
            getDialog().setTitle(R.string.delete_todos);
            tvTitle.setText(getString(R.string.title_delete_todos));
          } else {
            getDialog().setTitle(R.string.delete_todo);
            title = getString(R.string.title_delete_todo);
            ArrayList<Todo> todos = items;
            itemTitle = todos.get(0).getTitle();
            title = title + "\"" + itemTitle + "\"?";
            tvTitle.setText(title);
          }
          break;
        case "list":
          if (many) {
            getDialog().setTitle(R.string.delete_lists);
            tvTitle.setText(R.string.title_delete_lists);
          } else {
            getDialog().setTitle(R.string.delete_list);
            title = getString(R.string.title_delete_list);
            title = title + "\"" + itemTitle + "\"?";
            tvTitle.setText(title);
          }
          break;
        case "listInCategory":
          if (many) {
            getDialog().setTitle(R.string.delete_lists);
            tvTitle.setText(R.string.title_delete_lists);
          } else {
            getDialog().setTitle(R.string.delete_list);
            title = getString(R.string.title_delete_list);
            title = title + "\"" + itemTitle + "\"?";
            tvTitle.setText(title);
          }
          break;
        case "category":
          if (many) {
            getDialog().setTitle(R.string.delete_categories);
            tvTitle.setText(R.string.title_delete_categories);
          } else {
            getDialog().setTitle(R.string.delete_category);
            title = getString(R.string.title_delete_category);
            title = title + "\"" + itemTitle + "\"?";
            tvTitle.setText(title);
          }
          break;
      }
    }

    btnOK.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        if (!many) {
          String onlineId = getArguments().getString("onlineId");
          listener.onDelete(onlineId, type);
        } else {
          listener.onDelete(items, type);
        }
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

  private boolean isMany() {
    return items.size() > 1;
  }

  /**
   * Interfész, a ConfirmDeleteDialogFragment-et meghívó Fragment-ekkel való kommunikációra.
   */
  public interface IDeleteFragment {
    void onDelete(String onlineId, String type);
    void onDelete(ArrayList items, String type);
  }

}

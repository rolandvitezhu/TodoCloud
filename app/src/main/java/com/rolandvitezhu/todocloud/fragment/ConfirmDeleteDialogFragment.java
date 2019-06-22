package com.rolandvitezhu.todocloud.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.data.Todo;
import com.rolandvitezhu.todocloud.ui.activity.main.fragment.MainListFragment;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class ConfirmDeleteDialogFragment extends AppCompatDialogFragment {

  private String itemType;
  private ArrayList itemsToDelete;
  private boolean isManyItems = false;

  @BindView(R.id.textview_confirmdelete_actiontext)
  TextView tvActionText;
  @BindView(R.id.button_confirmdelete_ok)
  Button btnOK;

  Unbinder unbinder;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setStyle(STYLE_NORMAL, R.style.MyDialogTheme);
    prepareItemVariables();
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState
  ) {
    View view = inflater.inflate(R.layout.dialog_confirmdelete, container);
    unbinder = ButterKnife.bind(this, view);

    prepareDialogTexts();

    return view;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
  }

  private void prepareItemVariables() {
    Bundle arguments = getArguments();
    itemType = arguments.getString("itemType");
    itemsToDelete = arguments.getParcelableArrayList("itemsToDelete");
    prepareIsManyItems();
  }

  private void prepareIsManyItems() {
    if (itemsToDelete != null && itemsToDelete.size() > 1) {
      isManyItems = true;
    }
  }

  private void prepareDialogTexts() {
    String itemTitle = getArguments().getString("itemTitle");
    switch (itemType) {
      case "todo":
        if (isManyItems) {
          prepareConfirmDeleteTodosDialogTexts();
        } else {
          prepareConfirmDeleteTodoDialogTexts();
        }
        break;
      case "list":
        if (isManyItems) {
          prepareConfirmDeleteListsDialogTexts();
        } else {
          prepareConfirmDeleteListDialogTexts(itemTitle);
        }
        break;
      case "listInCategory":
        if (isManyItems) {
          prepareConfirmDeleteListsDialogTexts();
        } else {
          prepareConfirmDeleteListDialogTexts(itemTitle);
        }
        break;
      case "category":
        if (isManyItems) {
          prepareConfirmDeleteCategoriesDialogTexts();
        } else {
          prepareConfirmDeleteCategoryDialogTexts(itemTitle);
        }
        break;
    }
  }

  private void prepareConfirmDeleteCategoryDialogTexts(String itemTitle) {
    String dialogTitle = getString(R.string.confirmdelete_deletecategorytitle);
    String actionTextPrefix = getString(R.string.confirmdelete_deletecategoryactiontext);
    String actionText = prepareActionText(actionTextPrefix, itemTitle);
    setDialogTitle(dialogTitle);
    setActionText(actionText);
  }

  private void prepareConfirmDeleteCategoriesDialogTexts() {
    String dialogTitle = getString(R.string.confirmdelete_categoriestitle);
    String actionText = getString(R.string.confirmdelete_categoriesactiontext);
    setDialogTitle(dialogTitle);
    setActionText(actionText);
  }

  private void prepareConfirmDeleteListDialogTexts(String itemTitle) {
    String dialogTitle = getString(R.string.confirmdelete_deletelisttitle);
    String actionTextPrefix = getString(R.string.confirmdelete_deletelistactiontext);
    String actionText = prepareActionText(actionTextPrefix, itemTitle);
    setDialogTitle(dialogTitle);
    setActionText(actionText);
  }

  private void prepareConfirmDeleteListsDialogTexts() {
    String dialogTitle = getString(R.string.confirmdelete_liststitle);
    String actionText = getString(R.string.confirmdelete_listsactiontext);
    setDialogTitle(dialogTitle);
    setActionText(actionText);
  }

  private void prepareConfirmDeleteTodoDialogTexts() {
    String dialogTitle = getString(R.string.confirmdelete_deletetodotitle);
    String itemTitle = prepareTodoItemTitle();
    String actionTextPrefix = getString(R.string.confirmdelete_deletetodoactiontext);
    String actionText = prepareActionText(actionTextPrefix, itemTitle);
    setDialogTitle(dialogTitle);
    setActionText(actionText);
  }

  private void prepareConfirmDeleteTodosDialogTexts() {
    String dialogTitle = getString(R.string.confirmdelete_todostitle);
    String actionText = getString(R.string.confirmdelete_todosactiontext);
    setDialogTitle(dialogTitle);
    setActionText(actionText);
  }

  private String prepareTodoItemTitle() {
    ArrayList<Todo> todos = itemsToDelete;
    return todos.get(0).getTitle();
  }

  @NonNull
  private String prepareActionText(String actionTextPrefix, String itemTitle) {
    return actionTextPrefix + "\"" + itemTitle + "\"?";
  }

  private void setDialogTitle(String dialogTitle) {
    Dialog dialog = getDialog();
    dialog.setTitle(dialogTitle);
  }

  private void setActionText(String actionText) {
    tvActionText.setText(actionText);
  }

  @OnClick(R.id.button_confirmdelete_ok)
  public void onBtnOkClick(View view) {
    if (getTargetFragment() instanceof MainListFragment) {
      MainListFragment mainListFragment = (MainListFragment) getTargetFragment();

      if (itemType.equals("todo")) {
        mainListFragment.onSoftDelete(itemsToDelete, itemType);
      } else if (!isManyItems) {
        String onlineId = getArguments().getString("onlineId");
        mainListFragment.onSoftDelete(onlineId, itemType);
      } else {
        mainListFragment.onSoftDelete(itemsToDelete, itemType);
      }
    } else if (getTargetFragment() instanceof TodoListFragment) {
      TodoListFragment todoListFragment = (TodoListFragment) getTargetFragment();

      if (itemType.equals("todo")) {
        todoListFragment.onSoftDelete(itemsToDelete, itemType);
      } else if (!isManyItems) {
        String onlineId = getArguments().getString("onlineId");
        todoListFragment.onSoftDelete(onlineId, itemType);
      } else {
        todoListFragment.onSoftDelete(itemsToDelete, itemType);
      }
    } else if (getTargetFragment() instanceof SearchFragment) {
      SearchFragment searchFragment = (SearchFragment) getTargetFragment();

      if (itemType.equals("todo")) {
        searchFragment.onSoftDelete(itemsToDelete, itemType);
      } else if (!isManyItems) {
        String onlineId = getArguments().getString("onlineId");
        searchFragment.onSoftDelete(onlineId, itemType);
      } else {
        searchFragment.onSoftDelete(itemsToDelete, itemType);
      }
    }

    dismiss();
  }

  @OnClick(R.id.button_confirmdelete_cancel)
  public void onBtnCancelClick(View view) {
    dismiss();
  }

}

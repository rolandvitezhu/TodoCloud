package com.rolandvitezhu.todocloud.dialog;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.Window;

import com.rolandvitezhu.todocloud.R;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class SortTodoListDialog extends Dialog {

  public interface Presenter {
    void onSortByDueDatePushed();

    void onSortByPriorityPushed();
  }

  private Presenter presenter;

  public SortTodoListDialog(@NonNull Context context,
                           Presenter presenter) {
    super(context);
//    BaseInjector.getComponent().inject(this);
    this.presenter = presenter;

    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.dialog_sorttodolist);
    ButterKnife.bind(this);
    setCancelable(true);

    show();
  }

  @OnClick(R.id.dialog_sort_by_due_date)
  public void onSortByDueDateClicked() {
    presenter.onSortByDueDatePushed();
    dismiss();
  }

  @OnClick(R.id.dialog_sort_by_priority)
  public void onSortByPriorityClicked() {
    dismiss();
    presenter.onSortByPriorityPushed();
  }

}

package com.rolandvitezhu.todocloud.ui.activity.main.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.data.PredefinedList;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PredefinedListAdapter extends BaseAdapter {

  private List<PredefinedList> predefinedLists;

  @BindView(R.id.textview_predefinedlist_actiontext)
  TextView tvTitle;
  @BindView(R.id.imageview_predefinedlist)
  ImageView ivPredefinedList;

  public PredefinedListAdapter() {
    predefinedLists = new ArrayList<>();
  }

  @Override
  public int getCount() {
    return predefinedLists.size();
  }

  @Override
  public Object getItem(int position) {
    return predefinedLists.get(position);
  }

  @Override
  public long getItemId(int position) {
    return 0;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    PredefinedList predefinedList = predefinedLists.get(position);
    LayoutInflater layoutInflater = (LayoutInflater) parent.getContext().getSystemService(
        Context.LAYOUT_INFLATER_SERVICE
    );
    View itemView = layoutInflater.inflate(R.layout.item_predefinedlist, null);
    ButterKnife.bind(this, itemView);

    switch (position) {
      case 0:
        ivPredefinedList.setImageResource(R.drawable.baseline_today_24);
        break;
      case 1:
        ivPredefinedList.setImageResource(R.drawable.baseline_view_week_24);
        break;
      case 2:
        ivPredefinedList.setImageResource(R.drawable.baseline_all_inclusive_24);
        break;
      case 3:
        ivPredefinedList.setImageResource(R.drawable.baseline_done_24);
        break;
    }

    tvTitle.setText(predefinedList.getTitle());

    return itemView;
  }

  public void update(List<PredefinedList> predefinedLists) {
    this.predefinedLists.clear();
    this.predefinedLists.addAll(predefinedLists);
  }

  public void clear() {
    predefinedLists.clear();
    notifyDataSetChanged();
  }

}

package com.rolandvitezhu.todocloud.adapter;

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

    String title = predefinedList.getTitle();
    switch (title) {
      case "0":
        ivPredefinedList.setImageResource(R.drawable.baseline_today_24);
        tvTitle.setText(R.string.all_today);
        break;
      case "1":
        ivPredefinedList.setImageResource(R.drawable.baseline_view_week_24);
        tvTitle.setText(R.string.all_next7days);
        break;
      case "2":
        ivPredefinedList.setImageResource(R.drawable.baseline_all_inclusive_24);
        tvTitle.setText(R.string.all_all);
        break;
      case "3":
        ivPredefinedList.setImageResource(R.drawable.baseline_done_24);
        tvTitle.setText(R.string.all_completed);
        break;
    }

    return itemView;
  }

  public void addItem(PredefinedList predefinedList) {
    predefinedLists.add(predefinedList);
  }

  public void clear() {
    predefinedLists.clear();
    notifyDataSetChanged();
  }

}

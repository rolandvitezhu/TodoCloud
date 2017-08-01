package com.example.todocloud.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.todocloud.R;
import com.example.todocloud.data.PredefinedList;

import java.util.ArrayList;
import java.util.List;

public class PredefinedListAdapter extends BaseAdapter {

  private final List<PredefinedList> predefinedLists;

  public PredefinedListAdapter(final ArrayList<PredefinedList> predefinedLists) {
    this.predefinedLists = predefinedLists;
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
    View itemView = layoutInflater.inflate(R.layout.predefined_list_item, null);
    TextView tvTitle = (TextView) itemView.findViewById(R.id.tvActionText);
    ImageView ivPredefinedList = (ImageView) itemView.findViewById(R.id.ivPredefinedList);

    String title = predefinedList.getTitle();
    switch (title) {
      case "0":
        ivPredefinedList.setImageResource(R.drawable.ic_calendar_1_24dp);
        tvTitle.setText(R.string.itemMainListToday);
        break;
      case "1":
        ivPredefinedList.setImageResource(R.drawable.ic_week_view_24dp);
        tvTitle.setText(R.string.itemMainListNext7Days);
        break;
      case "2":
        ivPredefinedList.setImageResource(R.drawable.ic_infinity_24dp);
        tvTitle.setText(R.string.itemMainListAll);
        break;
      case "3":
        ivPredefinedList.setImageResource(R.drawable.ic_today_24dp);
        tvTitle.setText(R.string.itemMainListCompleted);
        break;
    }

    return itemView;
  }

  public void addItem(PredefinedList predefinedList) {
    predefinedLists.add(predefinedList);
  }

}

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

    final PredefinedList predefinedList = predefinedLists.get(position);

    LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(
        Context.LAYOUT_INFLATER_SERVICE);
    View itemView = inflater.inflate(R.layout.predefined_list_item, null);

    TextView textViewTitle = (TextView) itemView.findViewById(R.id.tvTitle);
    ImageView ivPredefinedList = (ImageView) itemView.findViewById(R.id.ivPredefinedList);

    String title = predefinedList.getTitle();
    switch (title) {
      case "0":
        ivPredefinedList.setImageResource(R.drawable.calendar_1_24);
        textViewTitle.setText(R.string.itemMainListToday);
        break;
      case "1":
        ivPredefinedList.setImageResource(R.drawable.week_view_24);
        textViewTitle.setText(R.string.itemMainListNext7Days);
        break;
      case "2":
        ivPredefinedList.setImageResource(R.drawable.infinity_24);
        textViewTitle.setText(R.string.itemMainListAll);
        break;
      case "3":
        ivPredefinedList.setImageResource(R.drawable.today_24);
        textViewTitle.setText(R.string.itemMainListCompleted);
        break;
    }

    return itemView;
  }

  /**
   * Hozzáadja az adapterhez a megadott elemet.
   * @param predefinedList Az adapterhez adandó elem.
   */
  public void addItem(PredefinedList predefinedList) {
    predefinedLists.add(predefinedList);
  }

}

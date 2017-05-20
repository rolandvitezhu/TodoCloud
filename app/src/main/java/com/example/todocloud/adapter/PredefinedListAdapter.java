package com.example.todocloud.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.todocloud.R;
import com.example.todocloud.data.PredefinedListItem;

import java.util.ArrayList;
import java.util.List;

public class PredefinedListAdapter extends BaseAdapter {

  private final List<PredefinedListItem> predefinedListItems;

  public PredefinedListAdapter(final ArrayList<PredefinedListItem> predefinedListItems) {
    this.predefinedListItems = predefinedListItems;
  }

  @Override
  public int getCount() {
    return predefinedListItems.size();
  }

  @Override
  public Object getItem(int position) {
    return predefinedListItems.get(position);
  }

  @Override
  public long getItemId(int position) {
    return 0;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {

    final PredefinedListItem predefinedListItem = predefinedListItems.get(position);

    LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(
        Context.LAYOUT_INFLATER_SERVICE);
    View itemView = inflater.inflate(R.layout.predefined_list_item, null);

    TextView textViewTitle = (TextView) itemView.findViewById(R.id.tvTitle);
    ImageView ivPredefinedList = (ImageView) itemView.findViewById(R.id.ivPredefinedList);

    String title = predefinedListItem.getTitle();
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
   * @param predefinedListItem Az adapterhez adandó elem.
   */
  public void addItem(PredefinedListItem predefinedListItem) {
    predefinedListItems.add(predefinedListItem);
  }

}

package com.example.todocloud.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.todocloud.R;

import java.util.List;

public class ListAdapter extends BaseAdapter {

  private List<com.example.todocloud.data.List> listItems;

  public ListAdapter(List<com.example.todocloud.data.List> listItems) {
    this.listItems = listItems;
  }

  @Override
  public int getCount() {
    return listItems.size();
  }

  @Override
  public Object getItem(int position) {
    return listItems.get(position);
  }

  @Override
  public long getItemId(int position) {
    return listItems.get(position).get_id();
  }

  public String getItemListOnlineId(int position) {
    return listItems.get(position).getListOnlineId();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    final com.example.todocloud.data.List listItem = listItems.get(position);
    LayoutInflater inflater = (LayoutInflater) parent.getContext().
        getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    convertView = inflater.inflate(R.layout.list_item, null);
    TextView tvTitle = (TextView) convertView.findViewById(R.id.tvTitle);
    tvTitle.setText(listItem.getTitle());
    return convertView;
  }

  /**
   * Törli az adapterből a megadott List-et .
   * @param list A törlendő List.
   */
  public void deleteList(com.example.todocloud.data.List list) {
    if (listItems.contains(list)) {
      listItems.remove(list);
    }
  }

  /**
   * Frissíti az adapter tartalmát a megadott adatokkal.
   * @param listItems A megadott List-ek.
   */
  public void update(List<com.example.todocloud.data.List> listItems) {
    this.listItems.clear();
    this.listItems.addAll(listItems);
  }

}

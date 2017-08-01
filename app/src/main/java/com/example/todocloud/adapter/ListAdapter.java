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

  private List<com.example.todocloud.data.List> lists;

  public ListAdapter(List<com.example.todocloud.data.List> lists) {
    this.lists = lists;
  }

  @Override
  public int getCount() {
    return lists.size();
  }

  @Override
  public Object getItem(int position) {
    return lists.get(position);
  }

  @Override
  public long getItemId(int position) {
    com.example.todocloud.data.List list = lists.get(position);
    return list.get_id();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    com.example.todocloud.data.List list = lists.get(position);
    LayoutInflater layoutInflater = (LayoutInflater) parent.getContext().getSystemService(
        Context.LAYOUT_INFLATER_SERVICE
    );
    convertView = layoutInflater.inflate(R.layout.item_list, null);
    TextView tvTitle = (TextView) convertView.findViewById(R.id.tvActionText);
    tvTitle.setText(list.getTitle());

    return convertView;
  }

  public void update(List<com.example.todocloud.data.List> lists) {
    this.lists.clear();
    this.lists.addAll(lists);
  }

}

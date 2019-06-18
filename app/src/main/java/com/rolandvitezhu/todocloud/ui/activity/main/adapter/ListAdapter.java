package com.rolandvitezhu.todocloud.ui.activity.main.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.rolandvitezhu.todocloud.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ListAdapter extends BaseAdapter {

  private List<com.rolandvitezhu.todocloud.data.List> lists;

  @BindView(R.id.textview_itemlist_actiontext)
  TextView tvTitle;

  public ListAdapter() {
    lists = new ArrayList<>();
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
    com.rolandvitezhu.todocloud.data.List list = lists.get(position);
    return list.get_id();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    com.rolandvitezhu.todocloud.data.List list = lists.get(position);
    LayoutInflater layoutInflater = (LayoutInflater) parent.getContext().getSystemService(
        Context.LAYOUT_INFLATER_SERVICE
    );
    convertView = layoutInflater.inflate(R.layout.item_list, null);
    ButterKnife.bind(this, convertView);

    tvTitle.setText(list.getTitle());

    return convertView;
  }

  public void update(List<com.rolandvitezhu.todocloud.data.List> lists) {
    this.lists.clear();
    this.lists.addAll(lists);
  }

  public void clear() {
    lists.clear();
    notifyDataSetChanged();
  }

}

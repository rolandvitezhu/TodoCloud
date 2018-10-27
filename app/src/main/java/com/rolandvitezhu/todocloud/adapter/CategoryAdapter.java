package com.rolandvitezhu.todocloud.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.data.Category;

import java.util.HashMap;
import java.util.List;

public class CategoryAdapter extends BaseExpandableListAdapter {

  private final List<Category> categories;
  private final HashMap<Category, List<com.rolandvitezhu.todocloud.data.List>> hmCategories;

  public CategoryAdapter(
      final List<Category> categories,
      final HashMap<Category, List<com.rolandvitezhu.todocloud.data.List>> hmCategories
  ) {
    this.categories = categories;
    this.hmCategories = hmCategories;
  }

  @Override
  public int getGroupCount() {
    return categories.size();
  }

  @Override
  public int getChildrenCount(int groupPosition) {
    Category category = categories.get(groupPosition);
    List<com.rolandvitezhu.todocloud.data.List> lists = hmCategories.get(category);
    return lists.size();
  }

  @Override
  public Object getGroup(int groupPosition) {
    return categories.get(groupPosition);
  }

  @Override
  public Object getChild(int groupPosition, int childPosition) {
    Category category = categories.get(groupPosition);
    List<com.rolandvitezhu.todocloud.data.List> lists = hmCategories.get(category);
    return lists.get(childPosition);
  }

  @Override
  public long getGroupId(int groupPosition) {
    Category category = categories.get(groupPosition);
    return category.get_id();
  }

  @Override
  public long getChildId(int groupPosition, int childPosition) {
    Category category = categories.get(groupPosition);
    List<com.rolandvitezhu.todocloud.data.List> lists = hmCategories.get(category);
    com.rolandvitezhu.todocloud.data.List list = lists.get(childPosition);
    return list.get_id();
  }

  @Override
  public boolean hasStableIds() {
    return false;
  }

  @Override
  public View getGroupView(
      int groupPosition,
      boolean isExpanded,
      View convertView,
      ViewGroup parent
  ) {
    Category category = (Category) getGroup(groupPosition);
    LayoutInflater layoutInflater = (LayoutInflater) parent.getContext().getSystemService(
        Context.LAYOUT_INFLATER_SERVICE
    );
    convertView = layoutInflater.inflate(R.layout.item_category, null);
    TextView tvTitle = (TextView) convertView.findViewById(R.id.textview_itemcategory_actiontext);
    tvTitle.setText(category.getTitle());
    handleCategoryIndicator(groupPosition, isExpanded, convertView);

    return convertView;
  }

  private void handleCategoryIndicator(int groupPosition, boolean isExpanded, View convertView) {
    if (shouldNotShowGroupIndicator(groupPosition)) {

    } else if (isExpanded) {
      showExpandedGroupIndicator(convertView);
    } else {
      showCollapsedGroupIndicator(convertView);
    }
  }

  private void showCollapsedGroupIndicator(View convertView) {
    ImageView ivGroupIndicator = (ImageView) convertView.findViewById(
        R.id.imageview_itemcategory_groupindicator
    );
    ivGroupIndicator.setImageResource(R.drawable.ic_previous_18dp);
  }

  private void showExpandedGroupIndicator(View convertView) {
    ImageView ivGroupIndicator = (ImageView) convertView.findViewById(
        R.id.imageview_itemcategory_groupindicator
    );
    ivGroupIndicator.setImageResource(R.drawable.ic_expand_arrow_18dp);
  }

  private boolean shouldNotShowGroupIndicator(int groupPosition) {
    return getChildrenCount(groupPosition) == 0;
  }

  @Override
  public View getChildView(
      int groupPosition,
      int childPosition,
      boolean isLastChild,
      View convertView,
      ViewGroup parent
  ) {
    com.rolandvitezhu.todocloud.data.List list = (com.rolandvitezhu.todocloud.data.List) getChild(
        groupPosition,
        childPosition
    );
    LayoutInflater layoutInflater = (LayoutInflater) parent.getContext().getSystemService(
        Context.LAYOUT_INFLATER_SERVICE
    );
    convertView = layoutInflater.inflate(R.layout.item_listincategory, null);
    TextView tvTitle = (TextView) convertView.findViewById(
        R.id.textview_itemlistincategory_actiontext
    );
    tvTitle.setText(list.getTitle());

    return convertView;
  }

  @Override
  public boolean isChildSelectable(int groupPosition, int childPosition) {
    return true;
  }

  public void update(final List<Category> categories,
                     final HashMap<Category, List<com.rolandvitezhu.todocloud.data.List>> hmCategories) {
    this.categories.clear();
    this.hmCategories.clear();
    this.categories.addAll(categories);
    this.hmCategories.putAll(hmCategories);
  }

}

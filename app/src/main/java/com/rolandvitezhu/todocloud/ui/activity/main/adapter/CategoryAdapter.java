package com.rolandvitezhu.todocloud.ui.activity.main.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.rolandvitezhu.todocloud.R;
import com.rolandvitezhu.todocloud.data.Category;
import com.rolandvitezhu.todocloud.di.FragmentScope;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

@FragmentScope
public class CategoryAdapter extends BaseExpandableListAdapter {

  private final List<Category> categories;
  private final LinkedHashMap<Category, List<com.rolandvitezhu.todocloud.data.List>> lhmCategories;

  @BindView(R.id.textview_itemcategory_actiontext)
  TextView tvTitle;

  @Inject
  public CategoryAdapter() {
    categories = new ArrayList<>();
    lhmCategories = new LinkedHashMap<>();
  }

  @Override
  public int getGroupCount() {
    return categories.size();
  }

  @Override
  public int getChildrenCount(int groupPosition) {
    Category category = categories.get(groupPosition);
    List<com.rolandvitezhu.todocloud.data.List> lists = lhmCategories.get(category);
    return (lists != null) ? lists.size() : 0;
  }

  @Override
  public Object getGroup(int groupPosition) {
    return categories.get(groupPosition);
  }

  @Override
  public Object getChild(int groupPosition, int childPosition) {
    Category category = categories.get(groupPosition);
    List<com.rolandvitezhu.todocloud.data.List> lists = lhmCategories.get(category);
    return (lists != null) ? lists.get(childPosition) : null;
  }

  @Override
  public long getGroupId(int groupPosition) {
    Category category = categories.get(groupPosition);
    return category.get_id();
  }

  @Override
  public long getChildId(int groupPosition, int childPosition) {
    Category category = categories.get(groupPosition);
    List<com.rolandvitezhu.todocloud.data.List> lists = lhmCategories.get(category);
    com.rolandvitezhu.todocloud.data.List list = (lists != null) ? lists.get(childPosition) : null;
    return (list != null) ? list.get_id() : -1;
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
    ButterKnife.bind(this, convertView);

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
    ImageView ivGroupIndicator = convertView.findViewById(R.id.imageview_itemcategory_groupindicator);
    ivGroupIndicator.setImageResource(R.drawable.baseline_expand_less_24);
  }

  private void showExpandedGroupIndicator(View convertView) {
    ImageView ivGroupIndicator = convertView.findViewById(R.id.imageview_itemcategory_groupindicator);
    ivGroupIndicator.setImageResource(R.drawable.baseline_expand_more_24);
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
    TextView tvTitle = convertView.findViewById(R.id.textview_itemlistincategory_actiontext);
    tvTitle.setText(list.getTitle());

    return convertView;
  }

  @Override
  public boolean isChildSelectable(int groupPosition, int childPosition) {
    return true;
  }

  public void update(final LinkedHashMap<Category, List<com.rolandvitezhu.todocloud.data.List>> lhmCategories) {
    this.categories.clear();
    this.lhmCategories.clear();
    this.categories.addAll(lhmCategories.keySet());
    this.lhmCategories.putAll(lhmCategories);
  }

  public void clear() {
    categories.clear();
    lhmCategories.clear();
    notifyDataSetChanged();
  }

}

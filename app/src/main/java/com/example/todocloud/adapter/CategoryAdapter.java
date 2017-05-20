package com.example.todocloud.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.todocloud.R;
import com.example.todocloud.data.Category;

import java.util.HashMap;
import java.util.List;

public class CategoryAdapter extends BaseExpandableListAdapter {

  private final List<Category> categories;
  private final HashMap<Category, List<com.example.todocloud.data.List>> hmCategories;

  public CategoryAdapter(final List<Category> categories,
                         final HashMap<Category, List<com.example.todocloud.data.List>> hmCategories) {
    this.categories = categories;
    this.hmCategories = hmCategories;
  }

  @Override
  public int getGroupCount() {
    return categories.size();
  }

  @Override
  public int getChildrenCount(int groupPosition) {
    return hmCategories.get(categories.get(groupPosition)).size();
  }

  @Override
  public Object getGroup(int groupPosition) {
    return categories.get(groupPosition);
  }

  @Override
  public Object getChild(int groupPosition, int childPosition) {
    return hmCategories.get(categories.get(groupPosition)).get(childPosition);
  }

  @Override
  public long getGroupId(int groupPosition) {
    return 0;
  }

  @Override
  public long getChildId(int groupPosition, int childPosition) {
    return ((com.example.todocloud.data.List) getChild(groupPosition, childPosition)).get_id();
  }

  public String getChildListOnlineId(int groupPosition, int childPosition) {
    return ((com.example.todocloud.data.List) getChild(groupPosition, childPosition)).
        getListOnlineId();
  }

  @Override
  public boolean hasStableIds() {
    return false;
  }

  @Override
  public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                           ViewGroup parent) {

    Category category = (Category) getGroup(groupPosition);

    LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(
        Context.LAYOUT_INFLATER_SERVICE);
    convertView = inflater.inflate(R.layout.category_item, null);

    TextView tvTitle = (TextView) convertView.findViewById(R.id.tvTitle);
    tvTitle.setText(category.getTitle());

    // A kategóriaindikátor beállítása:
    // Ha a Category elemszáma 0, akkor ne legyen kategóriaindikátor.
    if (getChildrenCount(groupPosition) == 0) {

    }
    // Ha le van nyitva a Category, akkor lefelé mutat a nyíl, egyébként pedig balra.
    else if (isExpanded) {
      ImageView groupIndicator = (ImageView) convertView.findViewById(R.id.imageViewGroupIndicator);
    groupIndicator.setImageResource(R.drawable.previous_18);
    } else {
      ImageView groupIndicator = (ImageView) convertView.findViewById(R.id.imageViewGroupIndicator);
      groupIndicator.setImageResource(R.drawable.expand_arrow_18);
    }

    return convertView;
  }

  @Override
  public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                           View convertView, ViewGroup parent) {

    final com.example.todocloud.data.List list =
        (com.example.todocloud.data.List) getChild(groupPosition, childPosition);

    LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(
        Context.LAYOUT_INFLATER_SERVICE);
    convertView = inflater.inflate(R.layout.list_in_category_item, null);

    TextView tvTitle = (TextView) convertView.findViewById(R.id.tvTitle);
    tvTitle.setText(list.getTitle());

    return convertView;
  }

  @Override
  public boolean isChildSelectable(int groupPosition, int childPosition) {
    return true;
  }

  /**
   * Törli az adapterből a megadott Category-t (a hozzá tartozó listával együtt).
   * @param category A törlendő Category.
   */
  public void deleteCategory(Category category) {
    if (categories.contains(category) && hmCategories.containsKey(category)) {
      categories.remove(category);
      hmCategories.remove(category);
    }
  }

  /**
   * Frissíti az adapter tartalmát a megadott adatokkal.
   * @param categories A megadott Category-k.
   * @param hmCategories A megadott Category-khez tartozó HashMap.
   */
  public void update(final List<Category> categories,
                     final HashMap<Category, List<com.example.todocloud.data.List>> hmCategories) {
    this.categories.clear();
    this.hmCategories.clear();
    this.categories.addAll(categories);
    this.hmCategories.putAll(hmCategories);
  }

}

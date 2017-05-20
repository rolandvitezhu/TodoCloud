package com.example.todocloud.customcomponent;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.widget.ListView;

/**
 * Ha a gyerek komponens egy ListView, akkor ezt az egyedi komponenst használva megelőzhetők a
 * görgetési gondok. Több View komponens esetén, ahol pl. már ScrollView-t használunk, a
 * ScrollView segítségével oldható meg ugyan ez a probléma.
 */
public class ScrollableSwipeRefreshLayout extends SwipeRefreshLayout {

  public ScrollableSwipeRefreshLayout(Context context) {
    super(context);
  }

  public ScrollableSwipeRefreshLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public boolean canChildScrollUp() {
    ListView listView = (ListView) this.findViewById(android.R.id.list);
    return listView.getChildCount() > 0 && listView.getChildAt(0).getTop() != 0;
  }

}

package com.rolandvitezhu.todocloud.customcomponent;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * Provide proper scrolling function, if the child view of SwipeRefreshLayout is a ListView. In
 * case, if we use more view components in a ScrollView, this class won't help, we can fix
 * scrolling behavior with the ScrollView itself.
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
    boolean isListViewScrolledToTop = listView.getChildAt(0).getTop() != 0;
    return listView.getChildCount() > 0 && isListViewScrolledToTop;
  }

}

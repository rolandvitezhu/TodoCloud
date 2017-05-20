package com.example.todocloud.customcomponent;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ListView;

public class ExpandableHeightListView extends ListView {

  boolean expanded = false;

  public ExpandableHeightListView(Context context) {
    super(context);
  }

  public ExpandableHeightListView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ExpandableHeightListView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  /*public ExpandableHeightListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }*/

  public boolean isExpanded() {
    return expanded;
  }

  public void setExpanded(boolean expanded) {
    this.expanded = expanded;
  }

  /**
   * Ha az expanded értéke true, akkor a ListView minden eleme látható, egyébként az eredeti visel-
   * kedés valósul meg.
   */
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

    if (isExpanded()) {
      // A legnagyobb érték beállítása szükséges, viszont az int 2 legnagyobb bitje nem használható
      // fel (ezért szükséges a ">> 2").
      int heightExpandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,
          MeasureSpec.AT_MOST);
      super.onMeasure(widthMeasureSpec, heightExpandSpec);
      ViewGroup.LayoutParams params = getLayoutParams();
      params.height = getMeasuredHeight();
    } else {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

  }

}

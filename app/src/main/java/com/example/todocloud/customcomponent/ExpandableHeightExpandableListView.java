package com.example.todocloud.customcomponent;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

/**
 * Provide proper scrolling function.
 */
public class ExpandableHeightExpandableListView extends ExpandableListView {

  boolean expanded = false;

  public ExpandableHeightExpandableListView(Context context) {
    super(context);
  }

  public ExpandableHeightExpandableListView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ExpandableHeightExpandableListView(
      Context context,
      AttributeSet attrs,
      int defStyleAttr
  ) {
    super(context, attrs, defStyleAttr);
  }

  public boolean isExpanded() {
    return expanded;
  }

  public void setExpanded(boolean expanded) {
    this.expanded = expanded;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    if (isExpanded()) {
      measureExpandedHeight(widthMeasureSpec);
      setExpandedLayoutHeight();
    } else {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

  }

  /**
   * Measure the view, using expanded height. The highest 2 bits of integer reserved for
   * MeasureSpec mode, hence don't use those.
   */
  @SuppressLint("WrongCall")
  private void measureExpandedHeight(int widthMeasureSpec) {
    int expandedHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
        Integer.MAX_VALUE >> 2,
        MeasureSpec.AT_MOST
    );
    super.onMeasure(widthMeasureSpec, expandedHeightMeasureSpec);
  }

  /**
   * Set expanded layout height to provide proper scrolling function, using the expanded height,
   * measured by calling measureExpandedHeight(int widthMeasureSpec) method.
   */
  private void setExpandedLayoutHeight() {
    ViewGroup.LayoutParams layoutParams = getLayoutParams();
    layoutParams.height = getMeasuredHeight();
  }

}

package com.example.todocloud.listener;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class RecyclerViewOnItemTouchListener implements RecyclerView.OnItemTouchListener {

  private ClickListener clickListener;
  private GestureDetector gestureDetector;

  public RecyclerViewOnItemTouchListener(
      Context context,
      final RecyclerView recyclerView,
      final ClickListener clickListener
  ) {
    this.clickListener = clickListener;
    gestureDetector = new GestureDetector(
        context,
        new GestureDetector.SimpleOnGestureListener() {

          @Override
          public boolean onSingleTapUp(MotionEvent e) {
            return true;
          }

          @Override
          public void onLongPress(MotionEvent e) {
            View childView = recyclerView.findChildViewUnder(e.getX(), e.getY());
            int childViewAdapterPosition = recyclerView.getChildAdapterPosition(childView);
            if (childView != null && clickListener != null) {
              clickListener.onLongClick(childView, childViewAdapterPosition);
            }
          }

        }
    );
  }

  @Override
  public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
    View childView = rv.findChildViewUnder(e.getX(), e.getY());
    int childViewAdapterPosition = rv.getChildAdapterPosition(childView);
    if (childView != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
      clickListener.onClick(childView, childViewAdapterPosition);
    }
    return false;
  }

  @Override
  public void onTouchEvent(RecyclerView rv, MotionEvent e) {

  }

  @Override
  public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

  }

  public interface ClickListener {
    void onClick(View childView, int childViewAdapterPosition);
    void onLongClick(View childView, int childViewAdapterPosition);
  }

}

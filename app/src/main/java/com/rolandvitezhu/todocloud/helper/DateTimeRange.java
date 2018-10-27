package com.rolandvitezhu.todocloud.helper;

public class DateTimeRange {

  private long startOfRangeLong;
  private long endOfRangeLong;

  public DateTimeRange(long startOfRangeLong, long endOfRangeLong) {
    this.startOfRangeLong = startOfRangeLong;
    this.endOfRangeLong = endOfRangeLong;
  }

  public DateTimeRange() {
  }

  public long getStartOfRangeLong() {
    return startOfRangeLong;
  }

  public void setStartOfRangeLong(long startOfRangeLong) {
    this.startOfRangeLong = startOfRangeLong;
  }

  public long getEndOfRangeLong() {
    return endOfRangeLong;
  }

  public void setEndOfRangeLong(long endOfRangeLong) {
    this.endOfRangeLong = endOfRangeLong;
  }
}

package com.example.todocloud.data;

public class PredefinedListItem {

  private String title;
  private String selectFromDB;
  private int numberOfTodos;

  public PredefinedListItem(String title, String selectFromDB, int numberOfTodos) {
    this.title = title;
    this.selectFromDB = selectFromDB;
    this.numberOfTodos = numberOfTodos;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getSelectFromDB() {
    return selectFromDB;
  }

  public void setSelectFromDB(String selectFromDB) {
    this.selectFromDB = selectFromDB;
  }

  public int getNumberOfTodos() {
    return numberOfTodos;
  }

  public void setNumberOfTodos(int numberOfTodos) {
    this.numberOfTodos = numberOfTodos;
  }

}

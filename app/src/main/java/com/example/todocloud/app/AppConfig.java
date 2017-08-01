package com.example.todocloud.app;

public class AppConfig {

  /*private static String prefix = "http://192.168.1.100/todo_cloud/";*/  // LAN IP
  private static String prefix = "http://192.168.56.1/todo_cloud/";  // Genymotion IP
  /*private static String prefix = "http://192.168.173.1/todo_cloud/";*/  // ad hoc network IP

  public static String URL_REGISTER = prefix + "v1/user/register";
  public static String URL_LOGIN = prefix + "v1/user/login";
  public static String URL_GET_TODOS = prefix + "v1/layout_appbar_todolist/:row_version";
  public static String URL_GET_LISTS = prefix + "v1/list/:row_version";
  public static String URL_GET_CATEGORIES = prefix + "v1/category/:row_version";
  public static String URL_UPDATE_TODO = prefix + "v1/layout_appbar_todolist/update";
  public static String URL_UPDATE_LIST = prefix + "v1/list/update";
  public static String URL_UPDATE_CATEGORY = prefix + "v1/category/update";
  public static String URL_INSERT_TODO = prefix + "v1/layout_appbar_todolist/insert";
  public static String URL_INSERT_LIST = prefix + "v1/list/insert";
  public static String URL_INSERT_CATEGORY = prefix + "v1/category/insert";

}

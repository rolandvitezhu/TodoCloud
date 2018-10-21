package com.example.todocloud.app;

public class AppConfig {

//  private static String prefix = "http://192.168.1.100/todo_cloud/";  // LAN IP
//  private static String prefix = "http://192.168.56.1/todo_cloud/";  // Genymotion IP
//  private static String prefix = "http://169.254.50.78/todo_cloud/";  // Genymotion IP - Current
//  private static String prefix = "http://10.0.2.2/todo_cloud/";  // AVD IP
//  private static String prefix = "http://192.168.173.1/todo_cloud/";  // ad hoc network IP
  private static String prefix = "http://todocloud.000webhostapp.com/todo_cloud/";  // 000webhost IP

  public static String URL_REGISTER = prefix + "v1/user/register";
  public static String URL_LOGIN = prefix + "v1/user/login";
  public static String URL_GET_NEXT_ROW_VERSION = prefix + "v1/get_next_row_version/:table";
  public static String URL_GET_TODOS = prefix + "v1/todo/:row_version";
  public static String URL_GET_LISTS = prefix + "v1/list/:row_version";
  public static String URL_GET_CATEGORIES = prefix + "v1/category/:row_version";
  public static String URL_UPDATE_TODO = prefix + "v1/todo/update";
  public static String URL_UPDATE_LIST = prefix + "v1/list/update";
  public static String URL_UPDATE_CATEGORY = prefix + "v1/category/update";
  public static String URL_INSERT_TODO = prefix + "v1/todo/insert";
  public static String URL_INSERT_LIST = prefix + "v1/list/insert";
  public static String URL_INSERT_CATEGORY = prefix + "v1/category/insert";

}

package com.example.todocloud.helper;

import android.util.Base64;

import com.example.todocloud.datastorage.DbConstants;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class OnlineIdGenerator {

  public static String generateUserOnlineId(long _id) {
    String installationId = InstallationIdHelper.getInstallationId();
    String table = DbConstants.User.DATABASE_TABLE;
    String onlineIdBase = installationId + table + _id;
    byte[] onlineIdBaseBytes = onlineIdBase.getBytes();
    MessageDigest messageDigest = null;
    try {
      messageDigest = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    messageDigest.update(onlineIdBaseBytes);
    byte[] onlineIdBytes = messageDigest.digest();
    return Base64.encodeToString(onlineIdBytes, Base64.DEFAULT).trim();
  }

  public static String generateOnlineId(String table, long _id, String apiKey) {
    String installationId = InstallationIdHelper.getInstallationId();
    String onlineIdBase = installationId + apiKey + table + _id;
    byte[] onlineIdBaseBytes = onlineIdBase.getBytes();
    MessageDigest messageDigest = null;
    try {
      messageDigest = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    messageDigest.update(onlineIdBaseBytes);
    byte[] onlineIdBytes = messageDigest.digest();
    return Base64.encodeToString(onlineIdBytes, Base64.DEFAULT).trim();
  }

}

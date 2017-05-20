package com.example.todocloud.helper;

import android.content.Context;
import android.util.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class OnlineIdGenerator {

  /**
   * Online_id-t generál bármelyik táblához tartozó objektum számára. Használata csak a user táb-
   * lához ajánlott.
   * @param context Kontextus.
   * @param table Azon tábla neve, amihez az online_id-t generáljuk.
   * @param _id Azon rekord _id-ja, amihez az online_id-t generáljuk.
   * @return Online_id-t ad vissza.
   */
  public static String generateOnlineId(Context context, String table, long _id) {
    MessageDigest messageDigest = null;
    try {
      messageDigest = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    String installationId = InstallationIdHelper.getInstallationId(context);
    String source = installationId + table + _id;
    messageDigest.update(source.getBytes());
    byte[] data = messageDigest.digest();
    String onlineId = Base64.encodeToString(data, 0, data.length, Base64.DEFAULT).trim();
    return onlineId;
  }

  /**
   * Online_id-t generál bármelyik táblához tartozó objektum számára. Használata nem ajánlott a
   * user táblához, az összes többihez viszont igen.
   * @param context Kontextus.
   * @param table Azon tábla neve, amihez az online_id-t generáljuk.
   * @param _id Azon rekord _id-ja, amihez az online_id-t generáljuk.
   * @param apiKey A felhasználó apiKey-e.
   * @return Online_id-t ad vissza.
   */
  public static String generateOnlineId(Context context, String table, long _id, String apiKey) {
    MessageDigest messageDigest = null;
    try {
      messageDigest = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    String installationId = InstallationIdHelper.getInstallationId(context);
    String source = installationId + apiKey + table + _id;
    messageDigest.update(source.getBytes());
    byte[] data = messageDigest.digest();
    String onlineId = Base64.encodeToString(data, 0, data.length, Base64.DEFAULT).trim();
    return onlineId;
  }

}

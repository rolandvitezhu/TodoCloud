package com.example.todocloud.helper;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

public class InstallationIdHelper {

  private static String installationId = null;
  private static final String FILE_NAME = "INSTALLATION_ID";

  /**
   * InstallationId-t olvas ki. Ha még nem létezik az installationId, akkor generáltatja.
   * @param context
   * @return A kiolvasott installationId.
   */
  public synchronized static String getInstallationId(Context context) {
    if (installationId == null) {
      File fInstallationId = new File(context.getFilesDir(), FILE_NAME);
      try {
        if (!fInstallationId.exists()) {
          writeInstallationId(fInstallationId);
        }
        installationId = readInstallationId(fInstallationId);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return installationId;
  }

  /**
   * Új installationId-t generáltat. Regisztráció során előfordulhatnak ütközések, ezek
   * elkerülésére használatos a metódus.
   * @param context
   * @return A generált installationId.
   */
  public synchronized static String getNewInstallationId(Context context) {
    File fInstallationId = new File(context.getFilesDir(), FILE_NAME);
    try {
      writeInstallationId(fInstallationId);
      installationId = readInstallationId(fInstallationId);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return installationId;
  }

  /**
   * InstallationId-t generál és tárol.
   * @param fInstallationId A fájl helye és neve.
   * @throws IOException
   */
  private static void writeInstallationId(File fInstallationId) throws IOException {
    FileOutputStream fileOutputStream = new FileOutputStream(fInstallationId);
    String id = UUID.randomUUID().toString();
    fileOutputStream.write(id.getBytes());
    fileOutputStream.close();
  }

  /**
   * Kiolvassa az installationId-t.
   * @param fInstallationId A fájl helye és neve.
   * @return A kiolvasott installationId.
   * @throws IOException
   */
  private static String readInstallationId(File fInstallationId) throws IOException {
    RandomAccessFile randomAccessFile = new RandomAccessFile(fInstallationId, "r");
    byte[] bytes = new byte[(int) randomAccessFile.length()];
    randomAccessFile.readFully(bytes);
    randomAccessFile.close();
    return new String(bytes);
  }

}

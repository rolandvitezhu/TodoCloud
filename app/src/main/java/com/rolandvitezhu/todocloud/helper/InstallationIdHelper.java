package com.rolandvitezhu.todocloud.helper;

import android.content.Context;

import com.rolandvitezhu.todocloud.app.AppController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

public class InstallationIdHelper {

  private static String installationId = null;
  private static final String FILE_NAME = "INSTALLATION_ID";

  public synchronized static String getInstallationId() {
    Context context = AppController.getAppContext();
    if (installationId == null) {
      File installationIdDir = context.getFilesDir();
      File installationIdFile = new File(installationIdDir, FILE_NAME);
      try {
        if (!installationIdFile.exists()) {
          writeInstallationId(installationIdFile);
        }
        installationId = readInstallationId(installationIdFile);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return installationId;
  }

  public synchronized static String getNewInstallationId() {
    Context context = AppController.getAppContext();
    File installationIdDir = context.getFilesDir();
    File installationIdFile = new File(installationIdDir, FILE_NAME);
    try {
      writeInstallationId(installationIdFile);
      installationId = readInstallationId(installationIdFile);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return installationId;
  }

  private static void writeInstallationId(File installationIdFile) throws IOException {
    String installationIdString = UUID.randomUUID().toString();
    byte[] installationId = installationIdString.getBytes();
    FileOutputStream fileOutputStream = new FileOutputStream(installationIdFile);
    fileOutputStream.write(installationId);
    fileOutputStream.close();
  }

  private static String readInstallationId(File installationIdFile) throws IOException {
    RandomAccessFile randomAccessFile = new RandomAccessFile(installationIdFile, "r");
    int installationIdFileLength = (int) randomAccessFile.length();
    byte[] installationId = new byte[installationIdFileLength];
    randomAccessFile.readFully(installationId);
    randomAccessFile.close();
    return new String(installationId);
  }

}

package com.arakamitech.arakamimessage.client.receiver;

import java.io.*;

public class FileReceiver {

    private static final String TEMP_FOLDER = "./tempDownloads/";

    public static File receiveFile(DataInputStream in, String filename, long size, String from) {
        try {
            File dir = new File(TEMP_FOLDER);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File tempFile = new File(dir, filename);
            FileOutputStream fos = new FileOutputStream(tempFile);

            byte[] buffer = new byte[4096];
            long remaining = size;
            while (remaining > 0) {
                int read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                fos.write(buffer, 0, read);
                remaining -= read;
            }
            fos.close();

            System.out.println("Archivo recibido de " + from + ": " + filename);
            return tempFile;

        } catch (IOException e) {
            System.out.println("Error recibiendo archivo: " + e.getMessage());
            return null;
        }
    }

    public static void cleanTempFolder() {
        File dir = new File(TEMP_FOLDER);
        if (dir.exists()) {
            for (File f : dir.listFiles()) {
                f.delete();
            }
        }
    }

}

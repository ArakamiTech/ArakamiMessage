package com.arakamitech.arakamimessage.client.receiver;

import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.util.stream.Stream;

public class FileReceiver {

    private FileReceiver() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final String TEMP_FOLDER = "./tempDownloads/";

    public static File receiveFile(DataInputStream in, String filename, long size, String from) {
        ensureTempFolderExists();

        var tempFile = Paths.get(TEMP_FOLDER, filename).toFile();

        try (var fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[4096];
            long remaining = size;

            while (remaining > 0) {
                int read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                fos.write(buffer, 0, read);
                remaining -= read;
            }

            showInfo("Archivo recibido de " + from + ": " + filename);
            return tempFile;

        } catch (IOException e) {
            showError("Error recibiendo archivo: " + e.getMessage());
            return null;
        }
    }

    public static void cleanTempFolder() {
        var dir = Paths.get(TEMP_FOLDER);
        if (Files.exists(dir)) {
            try (Stream<Path> paths = Files.list(dir)) {
                paths.forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        showError("Error eliminando archivo: " + path.getFileName() + " - " + e.getMessage());
                    }
                });
                showInfo("Carpeta temporal limpiada exitosamente.");
            } catch (IOException e) {
                showError("Error listando carpeta temporal: " + e.getMessage());
            }
        }
    }

    private static void ensureTempFolderExists() {
        var dir = new File(TEMP_FOLDER);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private static void showInfo(String message) {
        JOptionPane.showMessageDialog(null, message, "Información", JOptionPane.INFORMATION_MESSAGE);
    }

    private static void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}

package com.arakamitech.arakamimessage.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.HashMap;

public class ChatWindow {

    private JFrame frame;
    private final JTextField inputField;
    private final DataOutputStream out;
    private final String toUser;
    private String fromUser;
    private boolean isFocused = true;
    private final HashMap<JLabel, File> receivedFiles = new HashMap<>();
    private final JPanel messagePanel;
    private final JScrollPane scrollPane;

    public ChatWindow(String toUser, DataOutputStream out, String fromUser) {
        this.toUser = toUser;
        this.out = out;
        this.fromUser = fromUser;

        Font globalFont = new Font("Segoe UI", Font.PLAIN, 14);
        UIManager.put("Label.font", globalFont);
        UIManager.put("Button.font", globalFont);
        UIManager.put("TextField.font", globalFont);

        frame = new JFrame("Chat con " + toUser);

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.setBackground(new Color(230, 240, 250)); // azul claro
        header.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JLabel userIcon = new JLabel(new ImageIcon("icons.jpg"));
        JLabel nameLabel = new JLabel(toUser.equals(fromUser) ? "yo" : toUser);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        header.add(userIcon);
        header.add(Box.createRigidArea(new Dimension(5, 0)));
        header.add(nameLabel);
        header.add(Box.createHorizontalGlue());

        frame.add(header, BorderLayout.NORTH);

        messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.setBackground(Color.WHITE);
        messagePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        scrollPane = new JScrollPane(messagePanel);
        inputField = new JTextField(25);

        JButton sendFileButton = new JButton("Enviar archivo");
        sendFileButton.setBackground(new Color(0, 120, 215)); // azul oscuro
        sendFileButton.setForeground(Color.WHITE);
        sendFileButton.setFocusPainted(false);
        sendFileButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        sendFileButton.addActionListener(e -> sendFile());
        inputField.addActionListener(e -> sendMessage());

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(inputField, BorderLayout.CENTER);
        panel.add(sendFileButton, BorderLayout.EAST);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(panel, BorderLayout.SOUTH);
        frame.setSize(400, 500);
        frame.setMinimumSize(new Dimension(400, 500));
        frame.setVisible(true);
        frame.addWindowFocusListener(new WindowAdapter() {
            public void windowGainedFocus(WindowEvent e) {
                isFocused = true;
                frame.setTitle("Chat con " + toUser);
            }

            public void windowLostFocus(WindowEvent e) {
                isFocused = false;
            }
        });
    }

    private void sendMessage() {
        String msg = inputField.getText();
        try {
            out.writeUTF("MSGTO");
            out.writeUTF(toUser);
            out.writeUTF(msg);
            out.flush();
            appendMessage("yo: " + msg);
            inputField.setText("");
        } catch (IOException e) {
            appendMessage("Error enviando mensaje.");
        }
    }

    private void sendFile() {
        JFileChooser chooser = new JFileChooser();
        int option = chooser.showOpenDialog(frame);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (FileInputStream fis = new FileInputStream(file)) {
                out.writeUTF("SENDFILE");
                out.writeUTF(toUser);
                out.writeUTF(file.getName());
                out.writeLong(file.length());

                byte[] buffer = new byte[4096];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();

                appendMessage("yo: Archivo enviado: " + file.getName());
            } catch (IOException e) {
                appendMessage("Error enviando archivo.");
            }
        }
    }

    public void appendMessage(String msg) {
        JLabel label = new JLabel(msg);
        messagePanel.add(label);
        frame.revalidate();
        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
        if (!isFocused) {
            frame.setTitle("* Nuevo mensaje de " + toUser + " *");
        }
    }

    public void appendReceivedFileMessage(String from, String filename, File tempFile) {
        JLabel label = new JLabel("<html><a href=''>" + from + ": Archivo recibido: " + filename + "</a></html>");
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        label.setForeground(new Color(0, 102, 204)); // azul enlace

        receivedFiles.put(label, tempFile);

        label.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setSelectedFile(new File(filename));
                int option = chooser.showSaveDialog(frame);
                if (option == JFileChooser.APPROVE_OPTION) {
                    File dest = chooser.getSelectedFile();
                    try (FileInputStream fis = new FileInputStream(tempFile); FileOutputStream fos = new FileOutputStream(dest)) {

                        byte[] buffer = new byte[4096];
                        int read;
                        while ((read = fis.read(buffer)) != -1) {
                            fos.write(buffer, 0, read);
                        }
                        JOptionPane.showMessageDialog(frame, "Archivo guardado en: " + dest.getAbsolutePath());
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(frame, "Error guardando archivo: " + ex.getMessage());
                    }
                }
            }
        });

        messagePanel.add(label);
        frame.revalidate();
        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }
}

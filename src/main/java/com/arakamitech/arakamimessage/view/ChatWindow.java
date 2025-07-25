package com.arakamitech.arakamimessage.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ChatWindow {

    private JFrame frame;
    private final JTextField inputField;
    private final DataOutputStream out;
    private final String toUser;
    private boolean isFocused = true;
    private final Map<JLabel, File> receivedFiles = new HashMap<>();
    private final JPanel messagePanel;
    private final JScrollPane scrollPane;

    private static final Font GLOBAL_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Color HEADER_BG = new Color(230, 240, 250);
    private static final Color LINK_COLOR = new Color(0, 102, 204);
    private static final Color SEND_BUTTON_BG = new Color(0, 120, 215);

    public ChatWindow(String toUser, DataOutputStream out, String fromUser) {
        this.toUser = toUser;
        this.out = out;

        setupGlobalFont();

        frame = new JFrame("Chat con " + toUser);
        frame.add(createHeader(fromUser), BorderLayout.NORTH);

        messagePanel = createMessagePanel();
        scrollPane = new JScrollPane(messagePanel);
        inputField = new JTextField(25);

        var sendFileButton = createStyledButton("Enviar archivo", SEND_BUTTON_BG, Color.WHITE);
        sendFileButton.addActionListener(e -> sendFile());
        inputField.addActionListener(e -> sendMessage());

        var panel = new JPanel(new BorderLayout());
        panel.add(inputField, BorderLayout.CENTER);
        panel.add(sendFileButton, BorderLayout.EAST);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(panel, BorderLayout.SOUTH);
        frame.setSize(400, 500);
        frame.setMinimumSize(new Dimension(400, 500));
        frame.setVisible(true);

        frame.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                isFocused = true;
                frame.setTitle("Chat con " + toUser);
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                isFocused = false;
            }
        });
    }

    private void setupGlobalFont() {
        UIManager.put("Label.font", GLOBAL_FONT);
        UIManager.put("Button.font", GLOBAL_FONT);
        UIManager.put("TextField.font", GLOBAL_FONT);
    }

    private JPanel createHeader(String fromUser) {
        var header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.setBackground(HEADER_BG);
        header.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        var userIcon = new JLabel(new ImageIcon("icons.jpg"));
        var nameLabel = new JLabel(toUser.equals(fromUser) ? "yo" : toUser);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        header.add(userIcon);
        header.add(Box.createRigidArea(new Dimension(5, 0)));
        header.add(nameLabel);
        header.add(Box.createHorizontalGlue());

        return header;
    }

    private JPanel createMessagePanel() {
        var panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        return panel;
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        var button = new JButton(text);
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        return button;
    }

    private void sendMessage() {
        var msg = inputField.getText();
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
        var chooser = new JFileChooser();
        int option = chooser.showOpenDialog(frame);
        if (option == JFileChooser.APPROVE_OPTION) {
            var file = chooser.getSelectedFile();
            try (var fis = new FileInputStream(file)) {
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
        var label = new JLabel(msg);
        messagePanel.add(label);
        frame.revalidate();
        scrollToBottom();
        if (!isFocused) {
            frame.setTitle("* Nuevo mensaje de " + toUser + " *");
        }
    }

    public void appendReceivedFileMessage(String from, String filename, File tempFile) {
        var label = new JLabel("<html><a href=''>" + from + ": Archivo recibido: " + filename + "</a></html>");
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        label.setForeground(LINK_COLOR);

        receivedFiles.put(label, tempFile);

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                var chooser = new JFileChooser();
                chooser.setSelectedFile(new File(filename));
                int option = chooser.showSaveDialog(frame);
                if (option == JFileChooser.APPROVE_OPTION) {
                    var dest = chooser.getSelectedFile();
                    try (var fis = new FileInputStream(tempFile);
                         var fos = new FileOutputStream(dest)) {

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
        scrollToBottom();
    }

    private void scrollToBottom() {
        var vertical = scrollPane.getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }
}

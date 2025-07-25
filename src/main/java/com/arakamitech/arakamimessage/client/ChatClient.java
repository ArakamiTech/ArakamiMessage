package com.arakamitech.arakamimessage.client;

import com.arakamitech.arakamimessage.client.receiver.FileReceiver;
import com.arakamitech.arakamimessage.view.ChatWindow;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatClient {

    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 12345;
    private final String nickname;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private DefaultListModel<String> userListModel;
    private final Map<String, ChatWindow> chatWindows = new HashMap<>();

    public ChatClient() throws IOException {
        nickname = JOptionPane.showInputDialog("Ingresa tu nickname:");

        if (nickname == null || nickname.trim().isEmpty()) {
            showError("No se ingresó un nickname. La aplicación se cerrará.");
            System.exit(0);
        }

        socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        FileReceiver.cleanTempFolder();
        buildGUI();
        new Thread(this::listen).start();

        String response = in.readUTF();
        if ("SUBMITNICK".equals(response)) {
            out.writeUTF(nickname);
            out.flush();
        }
    }

    private void buildGUI() {
        var frame = new JFrame("ArakamiMessage - " + nickname);
        userListModel = new DefaultListModel<>();
        var userList = new JList<>(userListModel);

        userList.setCellRenderer(new UserListRenderer());

        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                var selectedUser = userList.getSelectedValue();
                if (selectedUser != null) {
                    openChatWindow(selectedUser);
                }
            }
        });

        frame.add(new JScrollPane(userList), BorderLayout.CENTER);
        frame.setSize(300, 400);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private void openChatWindow(String user) {
        chatWindows.computeIfAbsent(user, u -> new ChatWindow(u, out, nickname));
    }

    private void listen() {
        try {
            while (true) {
                var cmd = in.readUTF();

                if (cmd != null) {
                    switch (cmd) {
                        case "TEXT" -> caseText();
                        case "MSGFROM" -> caseMsgFrom();
                        case "SENDFILE" -> caseSendFile();
                        default -> {}
                    }
                }
            }
        } catch (IOException e) {
            showError("Conexión cerrada.");
        }
    }

    public void caseText() {
        try {
            var msg = in.readUTF();
            if (msg.startsWith("USERLIST:")) {
                var users = msg.substring(9).split(",");
                SwingUtilities.invokeLater(() -> {
                    userListModel.clear();
                    for (var u : users) {
                        if (!u.equals(nickname) && !u.isEmpty()) {
                            userListModel.addElement(u);
                        }
                    }
                });
            }
        } catch (IOException ex) {
            logError(ex, "Error recibiendo lista de usuarios");
        }
    }

    public void caseMsgFrom() {
        try {
            var msg = in.readUTF();
            var parts = msg.split(":", 2);
            var from = parts[0];
            var message = parts[1];

            var cw = chatWindows.computeIfAbsent(from, u -> new ChatWindow(from, out, nickname));
            cw.appendMessage(from + ": " + message);
        } catch (IOException ex) {
            logError(ex, "Error recibiendo mensaje");
        }
    }

    public void caseSendFile() {
        try {
            var from = in.readUTF();
            var filename = in.readUTF();
            var size = in.readLong();
            var tempFile = FileReceiver.receiveFile(in, filename, size, from);
            if (tempFile != null) {
                var cw = chatWindows.computeIfAbsent(from, u -> new ChatWindow(from, out, nickname));
                cw.appendReceivedFileMessage(from, filename, tempFile);
            }
        } catch (IOException ex) {
            logError(ex, "Error recibiendo archivo");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new ChatClient();
            } catch (IOException e) {
                showError("No se pudo conectar al servidor: " + e.getMessage());
            }
        });
    }

    private static void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static void logError(Exception ex, String context) {
        Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, context, ex);
        showError(context + ": " + ex.getMessage());
    }

    class UserListRenderer extends DefaultListCellRenderer {

        private final Icon onlineIcon = new ImageIcon("onlines.png");

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            var label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setIcon(onlineIcon);
            label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            return label;
        }
    }
}

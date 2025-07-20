package com.arakamitech.arakamimessage.client;

import com.arakamitech.arakamimessage.client.receiver.FileReceiver;
import com.arakamitech.arakamimessage.view.ChatWindow;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatClient {

    private static final String SERVERADDRES = "https://arakamimessageserver.onrender.com";
    private static final int SERVERPORT = 12345;
    private final String nickname;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private DefaultListModel<String> userListModel;
    private final Map<String, ChatWindow> chatWindows = new HashMap<>();

    public ChatClient() throws IOException {
        nickname = JOptionPane.showInputDialog("Ingresa tu nickname:");

        socket = new Socket(SERVERADDRES, SERVERPORT);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        FileReceiver.cleanTempFolder();
        buildGUI();
        new Thread(() -> listen()).start();

        String response = in.readUTF();
        if ("SUBMITNICK".equals(response)) {
            out.writeUTF(nickname);
            out.flush();
        }
    }

    private void buildGUI() {
        JFrame frame;
        frame = new JFrame("ArakamiMessage - " + nickname);
        userListModel = new DefaultListModel<>();
        JList<String> userList = new JList<>(userListModel);

        userList.setCellRenderer(new UserListRenderer());

        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedUser = userList.getSelectedValue();
                if (selectedUser != null) {
                    openChatWindow(selectedUser);
                }
            }
        });

        frame.add(new JScrollPane(userList), BorderLayout.CENTER);
        frame.setSize(300, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private void openChatWindow(String user) {
        if (!chatWindows.containsKey(user)) {
            ChatWindow cw = new ChatWindow(user, out, nickname);
            chatWindows.put(user, cw);
        }
    }

    private void listen() {
        try {
            while (true) {
                String cmd = in.readUTF();

                if (null != cmd) {
                    switch (cmd) {
                        case "TEXT" -> caseText();
                        case "MSGFROM" -> caseMsgFrom();
                        case "SENDFILE" -> caseSendFile();
                        default -> {}
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Conexión cerrada.");
        }
    }

    public void caseText() {
        try {
            String msg = in.readUTF();
            if (msg.startsWith("USERLIST:")) {
                String[] users = msg.substring(9).split(",");
                SwingUtilities.invokeLater(() -> {
                    userListModel.clear();
                    for (String u : users) {
                        if (!u.equals(nickname) && !u.isEmpty()) {
                            userListModel.addElement(u);
                        }
                    }
                });
            }
        } catch (IOException ex) {
            Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void caseMsgFrom() {
        try {
            String msg = in.readUTF();
            String[] parts = msg.split(":", 2);
            String from = parts[0];
            String message = parts[1];
            ChatWindow cw = chatWindows.get(from);
            if (cw == null) {
                cw = new ChatWindow(from, out, nickname);
                chatWindows.put(from, cw);
            }
            cw.appendMessage(from + ": " + message);
        } catch (IOException ex) {
            Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void caseSendFile() {
        try {
            String from = in.readUTF();
            String filename = in.readUTF();
            long size = in.readLong();
            File tempFile = FileReceiver.receiveFile(in, filename, size, from);
            if (tempFile != null) {
                ChatWindow cw = chatWindows.get(from);
                if (cw == null) {
                    cw = new ChatWindow(from, out, nickname);
                    chatWindows.put(from, cw);
                }
                cw.appendReceivedFileMessage(from, filename, tempFile);
            }
        } catch (IOException ex) {
            Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) throws Exception {
        new ChatClient();
    }

    class UserListRenderer extends DefaultListCellRenderer {

        private final Icon onlineIcon = new ImageIcon("onlines.png");
        private final Icon offlineIcon = new ImageIcon("offlines.png");

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            label.setIcon(onlineIcon); // en esta versión siempre online
            label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            return label;
        }
    }

}

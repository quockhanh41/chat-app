package src.client;

import javax.swing.*;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class ChatWindow extends JFrame {
    public JTextArea chatArea;
    public JTextField inputField;
    public JButton sendButton;
    public JButton sendAttachmentButton;
    public ArrayList<String> messageList;
    public HashMap<Integer, String> messageMap;
    public File chatFile;
    public String currentUser;
    public String chatName;
    private final PrintWriter writer;
    private final OutputStream fileWriter;
    private final boolean isGroupChat;

    public ChatWindow(String currentUser, String chatName, PrintWriter writer, OutputStream fileWriter, boolean isGroupChat) {
        super(isGroupChat ? "Group Chat: " + chatName : "Private Chat with " + chatName);
        this.currentUser = currentUser;
        this.fileWriter = fileWriter;
        this.chatName = chatName;
        this.writer = writer;
        this.isGroupChat = isGroupChat;
        this.messageList = new ArrayList<>();
        this.messageMap = new HashMap<>();
        initializeChatFile();
        createChatWindow();
        loadChatHistory();
    }

    private void initializeChatFile() {
        if (isGroupChat) {
            this.chatFile = new File("src/chatGroup_logs/" + chatName + ".txt");
        } else {
            String chatFileName = chatName + "_" + currentUser + ".txt";
            if (currentUser.compareTo(chatName) < 0) {
                chatFileName = currentUser + "_" + chatName + ".txt";
            }
            this.chatFile = new File("src/chat_logs/" + chatFileName);
        }
    }

    private void createChatWindow() {
        setSize(400, 300);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setBorder(BorderFactory.createTitledBorder(isGroupChat ? "Chat in " + chatName : "Chat with " + chatName));
        add(chatScrollPane, BorderLayout.CENTER);

        // Input panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Send");
        sendAttachmentButton = new JButton("Send Attachment");
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(sendButton);
        buttonPanel.add(sendAttachmentButton);
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // Add action listener for sendButton
        sendButton.addActionListener(e -> {
            String message = inputField.getText();
            if (!message.isEmpty()) {
                if (isGroupChat) {
                    writer.println("CMD_GROUP_MESSAGE " + chatName + " " + currentUser + " " + message);
                } else {
                    writer.println("CMD_MESSAGE " + currentUser + " " + chatName + " " + message);
                }
                inputField.setText("");
            }
            loadChatHistory();
        });

        // Add action listener for sendAttachmentButton
        sendAttachmentButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int option = fileChooser.showOpenDialog(this);
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                sendFile(file);
            }
        });

        // Add context menu for recall
        chatArea.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    int position = chatArea.viewToModel(e.getPoint());
                    int line = getLineFromPosition(chatArea, position);
                    if (line >= 0 && line < messageList.size()) {
                        showContextMenu(e, line);
                    }
                }
            }
        });

        setVisible(true);
    }

    private void sendFile(File file) {
        try {
            // Thông báo bắt đầu gửi file
            // format: CMD_FILE/CMD_GROUP_FILE <sendUser> <receiveUser> <fileSize> <fileName>
            if (isGroupChat) {
                writer.println("CMD_GROUP_FILE " + currentUser + " " + chatName + " " + file.length() + " " + file.getName());
            } else {
                writer.println("CMD_FILE " + currentUser + " " + chatName + " " + file.length() + " " + file.getName());
            }
            writer.flush(); // Đảm bảo lệnh được gửi đi

            // Gửi dữ liệu file nhị phân qua OutputStream
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fileWriter.write(buffer, 0, bytesRead);
            }
            fileWriter.flush(); // Đảm bảo toàn bộ dữ liệu đã được gửi

            fis.close();
            chatArea.append("File sent: " + file.getName() + "\n");
        } catch (IOException e) {
            chatArea.append("Failed to send file: " + file.getName() + "\n");
            e.printStackTrace();
        }
    }


    void loadChatHistory() {
        if (chatFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(chatFile))) {
                String line;
                int lineNumber = 0;
                // empty the messageList, chatArea and messageMap
                messageList.clear();
                chatArea.setText("");
                messageMap.clear();
                while ((line = reader.readLine()) != null) {
                    messageList.add(line);
                    messageMap.put(lineNumber, line);
                    chatArea.append(line + "\n");
                    lineNumber++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showContextMenu(MouseEvent e, int line) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem recallItem = new JMenuItem("Unsent Message");
        recallItem.addActionListener(event -> recallMessage(line));
        menu.add(recallItem);
        menu.show(chatArea, e.getX(), e.getY());
    }

    private int getLineFromPosition(JTextArea textArea, int position) {
        Element root = textArea.getDocument().getDefaultRootElement();
        return root.getElementIndex(position);
    }

    private void recallMessage(int line) {
        if (line >= 0 && line < messageList.size()) {
            String recallMessage = currentUser + " unsent a message";
            messageList.set(line, recallMessage);

            // Update the display
            chatArea.setText(String.join("\n", messageList));

            // Update the file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(chatFile))) {
                for (String msg : messageList) {
                    writer.write(msg);
                    writer.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
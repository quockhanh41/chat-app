package src.client;

import javax.swing.*;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class PrivateChatWindow extends JFrame {
    public JTextArea privateChatArea;
    public JTextField privateInputField;
    public JButton privateSendButton;
    public ArrayList<String> messageList;
    public HashMap<Integer, String> messageMap;
    public File chatFile;
    public String currentUser;
    public String selectedUser;
    private PrintWriter writer;

    public PrivateChatWindow(String currentUser, String selectedUser, PrintWriter writer) {
        super("Private Chat with " + selectedUser);
        this.currentUser = currentUser;
        this.selectedUser = selectedUser;
        this.writer = writer;
        this.messageList = new ArrayList<>();
        this.messageMap = new HashMap<>();
        initializeChatFile();
        createChatWindow();
        loadChatHistory();
    }

    private void initializeChatFile() {
        String chatFileName = selectedUser + "_" + currentUser + ".txt";
        if (currentUser.compareTo(selectedUser) < 0) {
            chatFileName = currentUser + "_" + selectedUser + ".txt";
        }
        this.chatFile = new File("src/chat_logs/" + chatFileName);
    }

    private void createChatWindow() {
        setSize(400, 300);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // Chat area
        privateChatArea = new JTextArea();
        privateChatArea.setEditable(false);
        JScrollPane privateChatScrollPane = new JScrollPane(privateChatArea);
        privateChatScrollPane.setBorder(BorderFactory.createTitledBorder("Chat with " + selectedUser));
        add(privateChatScrollPane, BorderLayout.CENTER);

        // Input panel
        JPanel privateBottomPanel = new JPanel(new BorderLayout());
        privateInputField = new JTextField();
        privateSendButton = new JButton("Send");
        privateBottomPanel.add(privateInputField, BorderLayout.CENTER);
        privateBottomPanel.add(privateSendButton, BorderLayout.EAST);
        add(privateBottomPanel, BorderLayout.SOUTH);

        // Add mouse listener for privateSendButton
        privateSendButton.addActionListener(e -> {
            String message = privateInputField.getText();
            if (!message.isEmpty()) {
                writer.println("CMD_MESSAGE " + currentUser + " " + selectedUser + " " + message);
                privateInputField.setText("");
            }
//            loadChatHistory();
        });


        // Add context menu for recall
        privateChatArea.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    int position = privateChatArea.viewToModel(e.getPoint());
                    int line = getLineFromPosition(privateChatArea, position);
                    if (line >= 0 && line < messageList.size()) {
                        showContextMenu(e, line);
                    }
                }
            }
        });

        setVisible(true);
    }

    void loadChatHistory() {
        if (chatFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(chatFile))) {
                String line;
                int lineNumber = 0;
                // empty the messageList, privateChatArea and messageMap
                messageList.clear();
                privateChatArea.setText("");
                messageMap.clear();
                while ((line = reader.readLine()) != null) {
                    messageList.add(line);
                    messageMap.put(lineNumber, line);
                    privateChatArea.append(line + "\n");
                    lineNumber++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    //            String formattedMessage = currentUser + ": " + message;
//            messageList.add(formattedMessage);
//            privateChatArea.append(formattedMessage + "\n");


    private void showContextMenu(MouseEvent e, int line) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem recallItem = new JMenuItem("Unsent Message");
        recallItem.addActionListener(event -> recallMessage(line));
        menu.add(recallItem);
        menu.show(privateChatArea, e.getX(), e.getY());
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
            privateChatArea.setText(String.join("\n", messageList));

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

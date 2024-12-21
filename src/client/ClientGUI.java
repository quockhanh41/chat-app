package src.client;

import javax.swing.*;
import java.awt.*;

public class ClientGUI extends JFrame {
    public JToolBar toolBar;
    public JTextArea chatArea;
    public JScrollPane chatScrollPane;
    public JPanel onlinePanel;
    public JScrollPane userScrollPane;
    public JPanel bottomPanel;
    public JTextField inputField;
    public JButton sendButton;
    public String[] usersOnline;
    JList<String> userList;

    // Constructor to set up the UI components
    public ClientGUI(String username) {
        // Set up the frame properties
        setTitle("You are logged in as: " + username);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // Top Tool Bar
        toolBar = new JToolBar();
        toolBar.add(myCreateButton("File Sharing", "src/client/image/account.png"));
        toolBar.add(myCreateButton("Account", "src/client/image/account.png"));
        toolBar.add(myCreateButton("Video Call", "src/client/image/video.png"));
        toolBar.add(myCreateButton("Voice Call", "src/client/image/voice.png"));
        toolBar.add(myCreateButton("Tools", "src/client/image/tools.png"));
        toolBar.add(myCreateButton("Contacts", "src/client/image/contacts.png"));
        add(toolBar, BorderLayout.NORTH);

        // Main Chat Area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setBorder(BorderFactory.createTitledBorder("Chat Area"));
        add(chatScrollPane, BorderLayout.CENTER);

        // Online Users Panel
        onlinePanel = new JPanel(new BorderLayout());
        userList = new JList<>();

        // Set the custom cell renderer for the online user list
        userList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JPanel panel = new JPanel(new BorderLayout());
            JLabel iconLabel = new JLabel(new ImageIcon(new ImageIcon("src/client/image/green_icon.png").getImage()
                    .getScaledInstance(10, 10, Image.SCALE_SMOOTH))); // Set the green icon
            JLabel textLabel = new JLabel(value);
            panel.add(iconLabel, BorderLayout.WEST);
            panel.add(textLabel, BorderLayout.CENTER);
            panel.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            return panel;
        });

        userScrollPane = new JScrollPane(userList);
        onlinePanel.add(new JLabel("Online Users"), BorderLayout.NORTH);
        onlinePanel.add(userScrollPane, BorderLayout.CENTER);
        add(onlinePanel, BorderLayout.EAST);

        // Bottom Panel (Input Field and Send Button)
        bottomPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = myCreateButton("Send Message", "src/client/image/send.png");
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

    }

    // Helper method to create a JButton with text and icon
    private static JButton myCreateButton(String text, String iconPath) {
        ImageIcon icon = new ImageIcon(iconPath);
        Image image = icon.getImage(); // transform it
        Image newimg = image.getScaledInstance(24, 24, java.awt.Image.SCALE_SMOOTH); // scale it the smooth way
        icon = new ImageIcon(newimg);  // transform it back
        return new JButton(text, icon);
    }

    // Helper method to append a message to the chat area
    public void appendMessage(String message) {
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    // Helper method to update the online user list
    public void updateOnlineUsers(String[] users) {
        DefaultListModel<String> userListModel = new DefaultListModel<>();
        for (String user : users) {
            userListModel.addElement(user);
        }
        userList.setModel(userListModel);
    }
}
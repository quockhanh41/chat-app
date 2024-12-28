package src.client;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.StringTokenizer;

public class ClientGUI extends JFrame {
    public JToolBar toolBar;
    public JTextArea chatArea;
    public JScrollPane chatScrollPane;
    public JPanel onlinePanel;
    public JScrollPane userScrollPane;
    public JPanel bottomPanel;
    public JTextField inputField;
    public JButton sendButton;
    public JList<String> userList;
    public String selectedUser;
    public ChatWindow privateChatWindow;
    public ChatWindow groupChatWindow;
    private final PrintWriter writer;
    private OutputStream fileWriter;

    public ClientGUI(String username, PrintWriter writer, OutputStream fileWriter) {
        this.writer = writer;
        this.fileWriter = fileWriter;
        setTitle("You are logged in as: " + username);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        toolBar = new JToolBar();
        toolBar.add(myCreateButton("Groups", "src/client/image/group.png"));
        toolBar.add(myCreateButton("Create Group", "src/client/image/group.png"));
        add(toolBar, BorderLayout.NORTH);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setBorder(BorderFactory.createTitledBorder("Chat Area"));
        add(chatScrollPane, BorderLayout.CENTER);

        onlinePanel = new JPanel(new BorderLayout());
        userList = new JList<>();

        userList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JPanel panel = new JPanel(new BorderLayout());
            JLabel iconLabel = new JLabel(new ImageIcon(new ImageIcon("src/client/image/green_icon.png").getImage()
                    .getScaledInstance(10, 10, Image.SCALE_SMOOTH)));
            JLabel textLabel = new JLabel(value);
            panel.add(iconLabel, BorderLayout.WEST);
            panel.add(textLabel, BorderLayout.CENTER);
            panel.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            return panel;
        });

        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    selectedUser = userList.getSelectedValue();
                    if (selectedUser != null) {
                        privateChatWindow = new ChatWindow(username, selectedUser, writer, fileWriter, false);
                        privateChatWindow.setVisible(true);
                    }
                }
            }
        });

        userScrollPane = new JScrollPane(userList);
        onlinePanel.add(new JLabel("Online Users"), BorderLayout.NORTH);
        onlinePanel.add(userScrollPane, BorderLayout.CENTER);
        add(onlinePanel, BorderLayout.EAST);

        bottomPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = myCreateButton("Send Message", "src/client/image/send.png");
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // show the groups including the user
        toolBar.getComponent(0).addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showGroups(username);
            }
        });
        toolBar.getComponent(1).addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                createGroupDialog(username);
            }
        });
    }

    private static JButton myCreateButton(String text, String iconPath) {
        ImageIcon icon = new ImageIcon(iconPath);
        Image image = icon.getImage();
        Image newimg = image.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        icon = new ImageIcon(newimg);
        return new JButton(text, icon);
    }

    public void updateOnlineUsers(StringTokenizer tokenizer, String currentUsername) {
        DefaultListModel<String> userListModel = new DefaultListModel<>();
        while (tokenizer.hasMoreTokens()) {
            String client = tokenizer.nextToken();
            if (!client.equals(currentUsername)) {
                userListModel.addElement(client);
            }
        }
        userList.setModel(userListModel);
    }

    public static String getCurrentTime() {
        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd:MMM:yy");
        return currentTime.format(formatter);
    }

    public void playNotificationSound() {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
                    new File("src/client/sound/notification.wav").getAbsoluteFile());
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.start();
        } catch (Exception e) {
            System.err.println("Error playing notification sound: " + e.getMessage());
        }
    }

    private void createGroupDialog(String username) {
        JTextField groupNameField = new JTextField();
        JList<String> userSelectionList = new JList<>(userList.getModel());
        userSelectionList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane userScrollPane = new JScrollPane(userSelectionList);

        Object[] message = {
                "Group Name:", groupNameField,
                "Select Members:", userScrollPane
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Create Group", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String groupName = groupNameField.getText().trim();
            List<String> selectedUsers = userSelectionList.getSelectedValuesList();

            if (groupName.isEmpty() || selectedUsers.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Group name and members cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String command = "CMD_GROUP_CREATE " + groupName + " " + String.join(" ", selectedUsers) + " " + username;
            writer.println(command);
            chatArea.append("Group creation request sent for: " + groupName + "\n");
        }
    }

    private void showGroups(String username) {
        File groupFile = new File("src/chatGroup_logs/group_members.txt");
        if (!groupFile.exists()) {
            JOptionPane.showMessageDialog(this, "No groups available.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        DefaultListModel<String> groupListModel = new DefaultListModel<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(groupFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                StringTokenizer tokenizer = new StringTokenizer(line);
                String groupName = tokenizer.nextToken();
                if (line.contains(username)) {
                    groupListModel.addElement(groupName);
                }
            }
            JList<String> groupList = new JList<>(groupListModel);
            JScrollPane groupScrollPane = new JScrollPane(groupList);
            // Add listener for groupList
            groupList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) { // Double-click detected
                        String selectedGroup = groupList.getSelectedValue();
                        if (selectedGroup != null) {
                            // close the existing group chat window if any
                            if (groupChatWindow != null) {
                                groupChatWindow.dispose();
                            }
                            // close groupScrollPane dialog
                            Window window = SwingUtilities.windowForComponent(groupScrollPane);
                            window.dispose();

                            groupChatWindow = new ChatWindow(username, selectedGroup, writer, fileWriter, true);
                            groupChatWindow.toFront(); // Đưa cửa sổ ra phía trước
                        }
                    }
                }
            });
            JOptionPane.showMessageDialog(this, groupScrollPane, "Groups", JOptionPane.PLAIN_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading group file.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public String showStoreFileNotification(String sender, String fileName) {
        String[] options = {"Accept", "Decline"};
        int choice = JOptionPane.showOptionDialog(this, sender + " wants to send you a file: " + fileName,
                "File Transfer Request", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice == 0) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File(fileName));
            int option = fileChooser.showSaveDialog(this);
            if (option == JFileChooser.APPROVE_OPTION) {
                return fileChooser.getSelectedFile().getAbsolutePath();
            }
        }
        return null;
    }
}
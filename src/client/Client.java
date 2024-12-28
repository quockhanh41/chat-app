package src.client;

import java.io.*;
import java.net.Socket;
import java.util.StringTokenizer;
import javax.swing.*;

public class Client {
    private static ClientGUI clientGUI;
    private final String clientUsername;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    private volatile boolean running = true;
    private String addressStorage;

    public Client(String serverIp, int serverPort, String username) {
        this.clientUsername = username;

        try {
            // Initialize socket and streams
            socket = new Socket(serverIp, serverPort);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Notify the server of the client's username
            writer.println("CMD_JOIN " + clientUsername);

            // Initialize and display the GUI
            clientGUI = new ClientGUI(clientUsername, writer, socket.getOutputStream());
            clientGUI.setVisible(true);
            clientGUI.chatArea.append("Connected to server.\n");

            // add action listener to the send button
            clientGUI.sendButton.addActionListener(e -> {
                String message = clientGUI.inputField.getText();
                if (!message.isEmpty()) {
                    writer.println("CMD_MESSAGE " + clientUsername + " " + message);
                    clientGUI.inputField.setText("");
                }
            });

            // Listen for messages from the server in a separate thread
            new Thread(() -> {
                String serverMessage;
                try {
                    while ((serverMessage = reader.readLine()) != null) {
                        processServerResponse(serverMessage);
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from the server.");
                }
            }).start();
        } catch (IOException e) {
            if (clientGUI != null) {
                clientGUI.chatArea.append("Connection error: " + e.getMessage() + "\n");
            }
            e.printStackTrace();
        }
    }

    private void processServerResponse(String response) {
        StringTokenizer tokenizer = new StringTokenizer(response);
        if (!tokenizer.hasMoreTokens()) return;

        String command = tokenizer.nextToken();
        switch (command) {
            // format: CMD_MESSAGE <sendUser> <receiveUser> <message>
            case "CMD_MESSAGE":
                String sendUser = tokenizer.nextToken();
                String receiveUser = tokenizer.nextToken();
                if (receiveUser.equals("ALL") || receiveUser.equals(clientUsername)) {
                    clientGUI.playNotificationSound();
                    // append the notification to the chat area: "<hh:mm dd:mmm:yy> you have received a message from <sendUser>"
                    appendMessage("[" + ClientGUI.getCurrentTime() + "] You have received a message from " + sendUser);
                    //check if the private chat window is open, if it is, load the chat history
                    if (clientGUI.privateChatWindow != null && clientGUI.privateChatWindow.chatName.equals(sendUser)) {
                        clientGUI.privateChatWindow.loadChatHistory();
                    }
                }
                break;
            case "CMD_GROUP_MESSAGE":
                // format: CMD_GROUP_MESSAGE <sendUser> <groupName> <message>
                String groupSendUser = tokenizer.nextToken();
                String groupName = tokenizer.nextToken();

                clientGUI.playNotificationSound();
                appendMessage("[" + ClientGUI.getCurrentTime() + "] You have received a message from " + groupName);

                if (clientGUI.groupChatWindow != null && clientGUI.groupChatWindow.chatName.equals(groupName)) {
                    clientGUI.groupChatWindow.loadChatHistory();
                }
                break;

            // format: CMD_FILE_NOTIFICATION <sender> <filename>
            case "CMD_FILE_NOTIFICATION":
                String fileSendUser = tokenizer.nextToken();
                // fileName is the rest of the string
                String fileName = response.substring(command.length() + fileSendUser.length() + 2);
                clientGUI.playNotificationSound();

                addressStorage = clientGUI.showStoreFileNotification(fileSendUser, fileName);

                if (addressStorage != null) {
                    // send a message to the server to notify that the client is ready to receive the file
                    writer.println("CMD_READY_TO_RECEIVE_FILE " + clientUsername + " " + fileName);
                }
                break;

            // format: CMD_FILE <fileName>
            case "CMD_FILE":
                String file_name = tokenizer.nextToken();

                // store the file in the addressStorage using the fileReader
                if (addressStorage != null) {
                    try {
                        FileOutputStream fos = new FileOutputStream(addressStorage);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = socket.getInputStream().read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                        fos.close();
                        appendMessage("File received: " + file_name);
                    } catch (IOException e) {
                        appendMessage("Failed to receive file: " + file_name);
                        e.printStackTrace();
                    }
                }
                break;
            // format: CMD_ONLINE <user1> <user2> ...
            case "CMD_ONLINE":
                clientGUI.updateOnlineUsers(tokenizer, clientUsername);
                break;

            default:
                appendMessage("Unknown command: " + response);
                break;
        }
    }

    private void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> clientGUI.chatArea.append(message + "\n"));
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java Client <server_ip> <server_port> <username>");
            return;
        }

        String serverIp = args[0];
        int serverPort;
        try {
            serverPort = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number.");
            return;
        }

        String username = args[2];
        new Client(serverIp, serverPort, username);
    }
}
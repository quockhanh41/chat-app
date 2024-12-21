package src.client;

import src.server.Server;

import java.io.*;
import java.net.Socket;
import java.util.Objects;
import java.util.StringTokenizer;
import javax.swing.*;

public class Client {
    private static ClientGUI clientGUI;
    private final String clientUsername;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private volatile boolean running = true;

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
            clientGUI = new ClientGUI(clientUsername);
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
                String message = response.substring(command.length() + sendUser.length() + receiveUser.length() + 3);
                if (receiveUser.equals("ALL") || receiveUser.equals(clientUsername)) {
                    appendMessage(sendUser + ": " + message);
                }
                break;

            // format: CMD_ONLINE <user1> <user2> ...
            case "CMD_ONLINE":
                clientGUI.updateOnlineUsers(tokenizer);
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

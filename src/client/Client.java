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

            // Set up the send button action
            clientGUI.sendButton.addActionListener(e -> {
                String message = clientGUI.inputField.getText().trim();
                if (!message.isEmpty()) {
                    writer.println("CMD_CHAT " + message);
                    clientGUI.inputField.setText("");
                }
            });

            // Listen for messages from the server in a separate thread
            new Thread(() -> {
                try {
                    while (running) {
                        String response = reader.readLine();
                        if (response == null) break; // Server closed the connection
                        processServerResponse(response);
                    }
                } catch (IOException e) {
                    if (running) {
                        clientGUI.chatArea.append("Connection error: " + e.getMessage() + "\n");
                    }
                } finally {
                    close(); // Ensure resources are closed when the thread exits
                }
            }).start();
        } catch (IOException e) {
            if (clientGUI != null) {
                clientGUI.chatArea.append("Connection error: " + e.getMessage() + "\n");
            }
            e.printStackTrace();
        }
    }

    /**
     * Processes incoming messages from the server.
     *
     * @param response The message received from the server.
     */
    private void processServerResponse(String response) {
        StringTokenizer tokenizer = new StringTokenizer(response);
        if (!tokenizer.hasMoreTokens()) return;

        String command = tokenizer.nextToken();
        switch (command) {
            case "CMD_CHAT":
                appendMessage(response.substring(command.length()).trim());
                break;

            case "CMD_JOIN":
                appendMessage(tokenizer.nextToken() + " has joined the chat.");
                break;

            case "CMD_LEAVE":
                appendMessage(tokenizer.nextToken() + " has left the chat.");
                break;

            case "CMD_ONLINE":
                // Update the online user list
                DefaultListModel<String> userListModel = new DefaultListModel<>();
                while (tokenizer.hasMoreTokens()) {
                    userListModel.addElement(tokenizer.nextToken());
                }
                clientGUI.userList.setModel(userListModel);
                break;

            default:
                appendMessage("Unknown command: " + response);
                break;
        }
    }

    /**
     * Appends a message to the chat area.
     *
     * @param message The message to append.
     */
    private void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> clientGUI.chatArea.append(message + "\n"));
    }

    /**
     * Closes the client's resources and stops the connection.
     */
    public void close() {
        running = false;
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

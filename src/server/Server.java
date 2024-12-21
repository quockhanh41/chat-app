package src.server;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    ServerGUI serverGUI;
    ServerSocket serverSocket;
    final Map<String, ClientHandler> clients = Collections.synchronizedMap(new HashMap<>());
    boolean isRunning = false;

    public Server() {
        serverGUI = new ServerGUI();
        serverGUI.startButton.addActionListener(e -> startServer());
        serverGUI.stopButton.addActionListener(e -> stopServer());
    }

    private void startServer() {
        try {
            int port = Integer.parseInt(serverGUI.portField.getText());
            serverSocket = new ServerSocket(port);
            isRunning = true;

            serverGUI.log("Server started on port " + port);

            serverGUI.startButton.setEnabled(false);
            serverGUI.stopButton.setEnabled(true);
            serverGUI.portField.setEditable(false);
            // Start accepting clients in a separate thread
            new Thread(() -> {
                while (isRunning) {
                    try {
                        serverGUI.log("Waiting for clients...\n");
                        Socket clientSocket = serverSocket.accept();
                        serverGUI.log("New client connected: " + clientSocket.getInetAddress() + "\n");
                        // Create and start ClientHandler thread
                        new Thread(new ClientHandler(clientSocket, this)).start();

                    } catch (IOException ex) {
                        if (isRunning) {
                            serverGUI.log("Error accepting client: " + ex.getMessage() + "\n");
                        }
                    }
                }
            }).start();
        } catch (IOException ex) {
            serverGUI.log("Error starting server: " + ex.getMessage() + "\n");
        }
    }

    private void stopServer() {
        try {
            isRunning = false;
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            serverGUI.log("Server stopped.\n");

            serverGUI.startButton.setEnabled(true);
            serverGUI.stopButton.setEnabled(false);
            serverGUI.portField.setEditable(true);

            synchronized (clients) {
                for (ClientHandler client : clients.values()) {
                    client.closeConnection();
                }
                clients.clear();
            }
        } catch (IOException ex) {
            serverGUI.log("Error stopping server: " + ex.getMessage() + "\n");
        }
    }

    void addClient(String username, ClientHandler clientHandler) {
        synchronized (clients) {
            this.clients.put(username, clientHandler);
        }
    }

    void broadcast(String message) {
        synchronized (clients) {
            for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
                System.out.println("Sending message to " + entry.getKey());
                ClientHandler clientHandler = entry.getValue();
                clientHandler.out.println(message);
            }
        }
    }

    void broadcastOnlineUsers() {
        StringBuilder onlineUsers = new StringBuilder("CMD_ONLINE ");
        synchronized (clients) {
            for (String user : clients.keySet()) {
                onlineUsers.append(user).append(" ");
            }
        }
        broadcast(onlineUsers.toString().trim());
    }

    void removeClient(String username) {
        synchronized (clients) {
            this.clients.remove(username);
            serverGUI.log(username + " removed from the client list.\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Server::new);
    }
}

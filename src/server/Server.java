package src.server;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    ServerGUI serverGUI;
    ServerSocket serverSocket;
    private static final Map<String, PrintWriter> clientHandlers = new HashMap<>();
    boolean isRunning = false;

    public Server() {
        serverGUI = new ServerGUI();
        serverGUI.startButton.addActionListener(e -> startServer());
        serverGUI.stopButton.addActionListener(e -> stopServer());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Server::new);
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
                        ClientHandler clientHandler = new ClientHandler(clientSocket);
                        clientHandler.start();
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

            synchronized (clientHandlers) {
                clientHandlers.clear();
            }
        } catch (IOException ex) {
            serverGUI.log("Error stopping server: " + ex.getMessage() + "\n");
        }
    }

    private static void broadcastMessage(String message) {
        synchronized (clientHandlers) {
            for (PrintWriter writer : clientHandlers.values()) {
                writer.println(message);
            }
        }
    }

    private static class ClientHandler extends Thread {
        Socket socket;
        PrintWriter out;
        BufferedReader in;
        String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String message;
                while ((message = in.readLine()) != null) {
                    processMessage(message);
                }
            } catch (IOException e) {
                System.err.println("Connection error with client: " + username);
            } finally {
                disconnect();
            }
        }

        public void disconnect() {
            try {
                socket.close();
                // Remove the client from the list of online users
                synchronized (clientHandlers) {
                    clientHandlers.remove(username);
                }
                ServerGUI.logArea.append(username + " has left the chat.\n");
                broadcastMessage(getOnlineUsersString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        public void sendMessage(String message) {
            out.println(message);
        }

        // Process incoming messages
        public void processMessage(String message) {
            if (message.startsWith("CMD_JOIN ")) {
                username = message.substring(9);
                ServerGUI.logArea.append(username + " has joined the chat.\n");
                synchronized (clientHandlers) {
                    clientHandlers.put(username, out);
                }
                sendMessage("Welcome to the chat, " + username + "!");
                broadcastMessage(getOnlineUsersString());
            } else if (message.startsWith("CMD_MESSAGE ")) {
                // message format: "CMD_MESSAGE <sender> <receiver> <message>"
                StringTokenizer tokenizer = new StringTokenizer(message);
                tokenizer.nextToken();
                String sender = tokenizer.nextToken();
                String receiver = tokenizer.nextToken();
                String msg = message.substring(14 + sender.length() + receiver.length());

                if (clientHandlers.containsKey(receiver)) {
                    clientHandlers.get(receiver).println(message);
                }

                writeMessageToFile(sender, receiver, msg);

            } else if (message.equals("CMD_QUIT")) {
                broadcastMessage(username + " has left the chat.");
                synchronized (clientHandlers) {
                    clientHandlers.remove(username);
                }
            }
        }

        // write message between 2 users to a file with the format "sender_receiver.txt" in the chat_logs directory
        public void writeMessageToFile(String sender, String receiver, String message) {
            // compare the usernames to determine the filename
            String filename = "src/chat_logs/" + sender + "_" + receiver + ".txt";
            if (sender.compareTo(receiver) > 0) {
                filename = "src/chat_logs/" + receiver + "_" + sender + ".txt";
            }
            try (PrintWriter writer = new PrintWriter(new FileWriter(filename, true))) {
                writer.println(sender + ": " + message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Create a String of all online users, format: "CMD_ONLINE <user1> <user2> ..."
        public String getOnlineUsersString() {
            StringBuilder users = new StringBuilder("CMD_ONLINE");
            synchronized (clientHandlers) {
                for (String user : clientHandlers.keySet()) {
                    users.append(" ").append(user);
                }
            }
            return users.toString();
        }
    }
}

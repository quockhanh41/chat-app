package src.server;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    ServerGUI serverGUI;
    ServerSocket serverSocket;
    private static final Map<String, ClientHandler> clientHandlers = new HashMap<>();
    private static final Map<String, Set<String>> groupMembers = new HashMap<>();
    boolean isRunning = false;

    public Server() {
        serverGUI = new ServerGUI();
        serverGUI.startButton.addActionListener(e -> startServer());
        serverGUI.stopButton.addActionListener(e -> stopServer());
        // read group members from the file
        readGroupMembers();
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
            for (ClientHandler writer : clientHandlers.values()) {
                writer.sendMessage(message);
            }
        }
    }

    private void readGroupMembers() {
        // check if the group_members.txt file exists
        File groupFile = new File("src/chatGroup_logs/group_members.txt");
        if (!groupFile.exists()) {
            try {
                groupFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (BufferedReader reader = new BufferedReader(new FileReader("src/chatGroup_logs/group_members.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                StringTokenizer tokenizer = new StringTokenizer(line);
                String groupName = tokenizer.nextToken();
                Set<String> members = new HashSet<>();
                while (tokenizer.hasMoreTokens()) {
                    members.add(tokenizer.nextToken());
                }
                groupMembers.put(groupName, members);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        public Socket socket;
        public PrintWriter out;
        public BufferedReader in;
        public String username;

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
                    clientHandlers.put(username, this);
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
                    clientHandlers.get(receiver).out.println(message);
                }
                writeMessageToFile(sender, receiver, msg);
            } else if (message.startsWith("CMD_GROUP_CREATE ")) {
                // message format: "CMD_GROUP_CREATE <group_name> <member1> <member2> ..."
                StringTokenizer tokenizer = new StringTokenizer(message);
                tokenizer.nextToken();
                String groupName = tokenizer.nextToken();
                Set<String> members = new HashSet<>();
                while (tokenizer.hasMoreTokens()) {
                    String member = tokenizer.nextToken();
                    members.add(member);
                }
                // if the group name is not already in the groupMembers map, add the group
                if (!groupMembers.containsKey(groupName)) {
                    groupMembers.put(groupName, members);
                    try (PrintWriter writer = new PrintWriter(new FileWriter("src/chatGroup_logs/group_members.txt", true))) {
                        writer.print(groupName);
                        for (String member : members) {
                            writer.print(" " + member);
                        }
                        writer.println();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else if (message.startsWith("CMD_GROUP_MESSAGE ")) {
                // message format: "CMD_GROUP_MESSAGE <group_name> <sender> <message>"
                StringTokenizer tokenizer = new StringTokenizer(message);
                tokenizer.nextToken();
                String groupName = tokenizer.nextToken();
                String sender = tokenizer.nextToken();
                String msg = message.substring(20 + groupName.length() + sender.length());
                if (groupMembers.containsKey(groupName)) {
                    for (String member : groupMembers.get(groupName)) {
                        if (clientHandlers.containsKey(member)) {
                            clientHandlers.get(member).out.println("CMD_GROUP_MESSAGE " + sender + " " + groupName + " " + msg);
                        }
                    }
                }
                // write the message to the group chat log
                try (PrintWriter writer = new PrintWriter(new FileWriter("src/chatGroup_logs/" + groupName + ".txt", true))) {
                    writer.println(sender + ": " + msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (message.startsWith("CMD_FILE ")) {
                // format: "CMD_FILE <sender> <receiver> <file_size> <filename>"
                StringTokenizer tokenizer = new StringTokenizer(message);
                tokenizer.nextToken();
                String sender = tokenizer.nextToken();
                String receiver = tokenizer.nextToken();
                String fileSize = tokenizer.nextToken();
                long fileSize_long = Long.parseLong(fileSize); // Read file size
                // get the filename, filename is the rest of the message
                String filename = message.substring(12 + sender.length() + receiver.length() + fileSize.length());

                // Store the file in the src/server/storage directory
                storeToStorage(fileSize_long, filename);

                // send the notification to the receiver format: "CMD_FILE_NOTIFICATION <sender> <filename>"
                if (clientHandlers.containsKey(receiver)) {
                    clientHandlers.get(receiver).out.println("CMD_FILE_NOTIFICATION " + sender + " " + filename);
                }

            } else if (message.startsWith("CMD_GROUP_FILE")) {
                // format: "CMD_GROUP_FILE <sendUser> <groupName> <fileSize> <fileName>"
                StringTokenizer tokenizer = new StringTokenizer(message);
                tokenizer.nextToken();
                String sendUser = tokenizer.nextToken();
                String groupName = tokenizer.nextToken();
                String fileSize = tokenizer.nextToken();
                long fileSize_long = Long.parseLong(fileSize); // Read file size
                // get the filename, filename is the rest of the message
                String filename = message.substring(16 + sendUser.length() + groupName.length() + fileSize.length());

                // Store the file in the src/server/storage directory
                storeToStorage(fileSize_long, filename);

                // send the notification to the group members format: "CMD_GROUP_FILE_NOTIFICATION <sendUser> <groupName> <filename>"
                if (groupMembers.containsKey(groupName)) {
                    for (String member : groupMembers.get(groupName)) {
                        if (clientHandlers.containsKey(member)) {
                            clientHandlers.get(member).out.println("CMD_FILE_NOTIFICATION " + groupName + " " + filename);
                        }
                    }
                }
            } else if (message.startsWith("CMD_READY_TO_RECEIVE_FILE ")) {
                // format: "CMD_READY_TO_RECEIVE_FILE <receiver> <filename>"
                StringTokenizer tokenizer = new StringTokenizer(message);
                tokenizer.nextToken();
                String receiver = tokenizer.nextToken();
                String filename = tokenizer.nextToken();

                // send a notification to the receiver that the sever is start to send the file
                if (clientHandlers.containsKey(receiver)) {
                    clientHandlers.get(receiver).out.println("CMD_FILE " + filename);
                }
                // send the file to the receiver
                try (FileInputStream fileIn = new FileInputStream("src/server/storage/" + filename)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    OutputStream out = socket.getOutputStream();
                    while ((bytesRead = fileIn.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if (message.equals("CMD_ONLINE")) {
                sendMessage(getOnlineUsersString());
            } else if (message.equals("CMD_QUIT")) {
                broadcastMessage(username + " has left the chat.");
                synchronized (clientHandlers) {
                    clientHandlers.remove(username);
                }
            }
        }

        private void storeToStorage(long fileSize_long, String filename) {
            try (FileOutputStream fileOut = new FileOutputStream("src/server/storage/" + filename)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                long remaining = fileSize_long;
                while (remaining > 0 && (bytesRead = socket.getInputStream().read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }
                fileOut.flush();
            } catch (IOException e) {
                e.printStackTrace();
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

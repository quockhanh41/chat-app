package src.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// Handle individual client connections
class ClientHandler implements Runnable {
    Socket socket;
    Server server;
    PrintWriter out;
    BufferedReader in;
    String username;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Get the username from the client with format: CMD_JOIN <username>
            String joinMessage = in.readLine();
            if (joinMessage == null || !joinMessage.startsWith("CMD_JOIN ")) {
                out.println("Invalid join message.");
                return;
            }
            username = joinMessage.substring(9);

            out.println("Welcome to the chat, " + username + "!");
            ServerGUI.logArea.append(username + " has joined the chat.\n");

            // Add the client to the server
            server.addClient(username, this);

            server.broadcastOnlineUsers();

            // Handle incoming messages
        } catch (IOException e) {
            System.err.println("Connection error with client: " + username);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void sendMessage(String message) {
        out.println(message);
    }

    public void closeConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
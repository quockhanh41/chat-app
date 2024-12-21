package src.server;// Server.java
import java.io.*;
import java.net.*;
import java.util.*;

public class originalServer {
    private static final int PORT = 12345;
    private static Map<String, PrintWriter> clientHandlers = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("Chat server is running...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                // Create a new thread for each client
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Request username
                out.println("Enter your username: ");
                username = in.readLine();

                synchronized (clientHandlers) {
                    if (clientHandlers.containsKey(username)) {
                        out.println("Username already taken. Connection closing...");
                        socket.close();
                        return;
                    }
                    clientHandlers.put(username, out);
                }

                broadcastMessage(username + " has joined the chat.");

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println(username + ": " + message);
                    broadcastMessage(username + ": " + message);
                }
            } catch (IOException e) {
                System.out.println("Client disconnected: " + socket.getInetAddress());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                synchronized (clientHandlers) {
                    clientHandlers.remove(username);
                }
                broadcastMessage(username + " has left the chat.");
            }
        }

        private void broadcastMessage(String message) {
            synchronized (clientHandlers) {
                for (PrintWriter writer : clientHandlers.values()) {
                    writer.println(message);
                }
            }
        }
    }
}

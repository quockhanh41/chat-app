package src.server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ServerGUI extends JFrame {
    public JTextField portField; // Port number input
    public static JTextArea logArea;    // Log display area
    public JButton startButton, stopButton; // Control buttons

    public ServerGUI() {
        // JFrame setup
        super("Server Control Panel");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Top Panel for Port Input
        JPanel topPanel = new JPanel(new FlowLayout());
        JLabel portLabel = new JLabel("Port Number:");
        portField = new JTextField("4000", 10);
        startButton = new JButton("Start Server");
        stopButton = new JButton("Stop Server");
        stopButton.setEnabled(false);

        topPanel.add(portLabel);
        topPanel.add(portField);
        topPanel.add(startButton);
        topPanel.add(stopButton);

        add(topPanel, BorderLayout.NORTH);

        // Log Area
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.CENTER);


        setVisible(true);
    }

    // Helper method to append text to the log area
    public void log(String text) {
        logArea.append(text + "\n");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Server::new);
    }
}

package src.client.authen;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class RegisterForm extends JFrame {

    private JLabel lblTitle, lblTagline, lblRegister, lblUsername, lblPassword;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JCheckBox chkShowPassword;
    private JButton btnRegister, btnBack;

    public RegisterForm() {
        // Set up the frame
        setTitle("Chat App Project");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setLayout(null);

        // Title
        lblTitle = new JLabel("Chat App");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 24));
        lblTitle.setBounds(130, 10, 200, 30);
        add(lblTitle);

        lblTagline = new JLabel("Chat together");
        lblTagline.setFont(new Font("Arial", Font.PLAIN, 12));
        lblTagline.setBounds(140, 40, 200, 20);
        add(lblTagline);

        lblRegister = new JLabel("Register Account");
        lblRegister.setFont(new Font("Arial", Font.BOLD, 18));
        lblRegister.setBounds(120, 70, 200, 20);
        add(lblRegister);

        // Username
        lblUsername = new JLabel("Username");
        lblUsername.setBounds(50, 100, 100, 20);
        add(lblUsername);

        txtUsername = new JTextField();
        txtUsername.setBounds(150, 100, 200, 25);
        add(txtUsername);

        // Password
        lblPassword = new JLabel("Password");
        lblPassword.setBounds(50, 140, 100, 20);
        add(lblPassword);

        txtPassword = new JPasswordField();
        txtPassword.setBounds(150, 140, 200, 25);
        add(txtPassword);

        // Show password checkbox
        chkShowPassword = new JCheckBox("Show password");
        chkShowPassword.setBounds(150, 170, 150, 20);
        add(chkShowPassword);

        // ActionListener to toggle password visibility
        chkShowPassword.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (chkShowPassword.isSelected()) {
                    txtPassword.setEchoChar((char) 0); // Show password
                } else {
                    txtPassword.setEchoChar('•'); // Hide password
                }
            }
        });

        // Register button
        btnRegister = new JButton("Register");
        btnRegister.setBounds(70, 210, 120, 30);
        add(btnRegister);

        // Back button
        btnBack = new JButton("Back");
        btnBack.setBounds(220, 210, 120, 30);
        add(btnBack);

        // Add action listener to back button
        btnBack.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Close the register form
                dispose();
                // Create and show the login form
                SwingUtilities.invokeLater(LoginForm::new);
            }
        });


        btnRegister.addActionListener(e -> {
            String username = txtUsername.getText();
            String password = new String(txtPassword.getPassword());
            if (AuthenticationUtilities.isAvailableUsername(username)) {
                if (AuthenticationUtilities.registerUser(username, password)) {
                    JOptionPane.showMessageDialog(null, "Register successfully");
                    dispose();
                    SwingUtilities.invokeLater(LoginForm::new);
                } else {
                    JOptionPane.showMessageDialog(null, "Register failed");
                }
            } else {
                JOptionPane.showMessageDialog(null, "Username is already taken");
            }
        });
        // Set the frame to be visible
        setVisible(true);
    }

    public static void main(String[] args) {
        new RegisterForm();
    }
}

package src.client.authen;

import src.client.Client;

import javax.swing.*;
import java.awt.*;

public class LoginForm extends JFrame {
    // Components declaration
    private final JTextField txtUsername;
    private JTextField txtIP1;
    private JTextField txtIP2;
    private JTextField txtIP3;
    private JTextField txtIP4;
    private JTextField txtPort;
    private final JPasswordField txtPassword;
    private final JCheckBox chkShowPassword;
    private JButton btnLogin, btnRegister;

    public LoginForm() {
        // JFrame settings
        setTitle("Chat App Project");
        setSize(450, 400);
        setLayout(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Title
        JLabel lblTitle = new JLabel("Chat App");
        lblTitle.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 30));
        lblTitle.setBounds(150, 10, 200, 40);
        add(lblTitle);

        JLabel lblTagline = new JLabel("Chat together");
        lblTagline.setFont(new Font("Arial", Font.PLAIN, 12));
        lblTagline.setBounds(165, 50, 200, 20);
        add(lblTagline);

        // Username
        JLabel lblUsername = new JLabel("Username");
        lblUsername.setBounds(50, 90, 100, 25);
        add(lblUsername);

        txtUsername = new JTextField();
        txtUsername.setBounds(150, 90, 200, 25);
        add(txtUsername);

        // Password
        JLabel lblPassword = new JLabel("Password");
        lblPassword.setBounds(50, 130, 100, 25);
        add(lblPassword);

        txtPassword = new JPasswordField();
        txtPassword.setBounds(150, 130, 200, 25);
        add(txtPassword);

        chkShowPassword = new JCheckBox("Show password");
        chkShowPassword.setBounds(150, 160, 150, 20);
        add(chkShowPassword);

        // Show/hide password logic
        chkShowPassword.addActionListener(e -> {
            if (chkShowPassword.isSelected()) {
                txtPassword.setEchoChar((char) 0);
            } else {
                txtPassword.setEchoChar('•');
            }
        });

        // Forgot Password
        JLabel lblForgotPassword = new JLabel("<html><a href='#'>Forgot password?</a></html>");
        lblForgotPassword.setBounds(310, 160, 120, 20);
        lblForgotPassword.setCursor(new Cursor(Cursor.HAND_CURSOR));
        add(lblForgotPassword);

        // IP Address
        JLabel lblIP = new JLabel("IP Address");
        lblIP.setBounds(50, 200, 100, 25);
        add(lblIP);

        txtIP1 = new JTextField("127");
        txtIP1.setBounds(150, 200, 50, 25);
        txtIP2 = new JTextField("0");
        txtIP2.setBounds(205, 200, 50, 25);
        txtIP3 = new JTextField("0");
        txtIP3.setBounds(260, 200, 50, 25);
        txtIP4 = new JTextField("1");
        txtIP4.setBounds(315, 200, 50, 25);

        add(txtIP1);
        add(txtIP2);
        add(txtIP3);
        add(txtIP4);

        // Port Number
        JLabel lblPort = new JLabel("Port Number");
        lblPort.setBounds(50, 240, 100, 25);
        add(lblPort);

        txtPort = new JTextField("4000");
        txtPort.setBounds(150, 240, 215, 25);
        add(txtPort);

        // Buttons
        btnLogin = new JButton("Login");
        btnLogin.setBounds(70, 300, 120, 35);
        add(btnLogin);

        btnRegister = new JButton("Register");
        btnRegister.setBounds(230, 300, 120, 35);
        add(btnRegister);

        // Add action listeners for buttons (optional)
        btnLogin.addActionListener(e -> {
            String username = txtUsername.getText();
            String password = new String(txtPassword.getPassword());
            String ip = txtIP1.getText() + "." + txtIP2.getText() + "." + txtIP3.getText() + "." + txtIP4.getText();
            int port = Integer.parseInt(txtPort.getText());

            if (AuthenticationUtilities.isValidUser(username, password)) {
                dispose();
                new Client(ip, port, username);
            } else {
                JOptionPane.showMessageDialog(null, "Invalid username or password", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Register button action listener, opens RegisterForm, closes LoginForm
        btnRegister.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(RegisterForm::new);
        });

        // Set frame visible
        setVisible(true);
    }

    public static void main(String[] args) {
        new LoginForm();
    }
}

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientGUI {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JTextField portField;
    private JTextField hostField;
    private JButton connectButton;
    private JButton sendButton;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new);
    }

    public ClientGUI() {
        initializeGUI();
    }

    private void initializeGUI() {
        frame = new JFrame("Chat Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 500);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        messageField = new JTextField();
        messageField.setEnabled(false);

        sendButton = new JButton("Send");
        sendButton.setEnabled(false);

        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        portField = new JTextField("777", 5); // Default port is 777
        hostField = new JTextField("localhost", 10);

        connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connectToServer());

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Host:"));
        topPanel.add(hostField);
        topPanel.add(new JLabel("Port:"));
        topPanel.add(portField);
        topPanel.add(connectButton);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void connectToServer() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            String host = hostField.getText().trim();

            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            new Thread(this::listenForMessages).start();

            String username = JOptionPane.showInputDialog(frame, "Enter your username:", "Login", JOptionPane.PLAIN_MESSAGE);
            if (username == null || username.trim().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Username is required to join the chat.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            out.println(username);
            messageField.setEnabled(true);
            sendButton.setEnabled(true);
            connectButton.setEnabled(false);

            hostField.setEnabled(false);
            portField.setEnabled(false);
        } catch (IOException | NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Unable to connect to the server: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            out.println(message);
            messageField.setText("");
        }
    }

    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                chatArea.append(message + "\n");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Connection lost.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

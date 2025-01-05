// Aggiornamento di ClientGUI.java per supportare il trasferimento di file

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class ClientGUI {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JTextField portField;
    private JTextField hostField;
    private JButton connectButton;
    private JButton sendButton;
    private JButton disconnectButton;
    private JButton sendFileButton; // Pulsante per inviare file

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
        frame.setSize(700, 500);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        messageField = new JTextField();
        messageField.setEnabled(false);

        sendButton = new JButton("Send");
        sendButton.setEnabled(false);

        disconnectButton = new JButton("Disconnect");
        disconnectButton.setEnabled(false);

        sendFileButton = new JButton("Send File"); // Inizializzazione pulsante
        sendFileButton.setEnabled(false);

        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        disconnectButton.addActionListener(e -> disconnectFromServer());
        sendFileButton.addActionListener(e -> sendFile()); // Azione per inviare file

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        portField = new JTextField("7777", 5);
        hostField = new JTextField("localhost", 10);

        connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connectToServer());

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Host:"));
        topPanel.add(hostField);
        topPanel.add(new JLabel("Port:"));
        topPanel.add(portField);
        topPanel.add(connectButton);
        topPanel.add(disconnectButton);
        topPanel.add(sendFileButton);

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
            disconnectButton.setEnabled(true);
            connectButton.setEnabled(false);
            sendFileButton.setEnabled(true);

            hostField.setEnabled(false);
            portField.setEnabled(false);
        } catch (IOException | NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Unable to connect to the server: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendMessage() {
        try {
            String message = messageField.getText().trim();
            if (message.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Message cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (message.length() > 200) {
                JOptionPane.showMessageDialog(frame, "Message too long (max 200 characters).", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            out.println(message);
            messageField.setText("");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error sending message: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(frame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(file));
                 OutputStream socketOut = socket.getOutputStream()) {

                out.println("FILE:" + file.getName()); // Invia intestazione file

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    socketOut.write(buffer, 0, bytesRead);
                }
                socketOut.flush();
                chatArea.append("File sent: " + file.getName() + "\n");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Error sending file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("FILE:")) {
                    receiveFile(message.substring(5));
                } else {
                    chatArea.append(message + "\n");
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Connection lost.", "Error", JOptionPane.ERROR_MESSAGE);
            disconnectFromServer();
        }
    }

    private void receiveFile(String fileName) {
        try (BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(fileName));
             InputStream socketIn = socket.getInputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = socketIn.read(buffer)) != -1) {
                fileOut.write(buffer, 0, bytesRead);
            }
            fileOut.flush();
            chatArea.append("File received: " + fileName + "\n");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error receiving file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void disconnectFromServer() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            out = null;
            in = null;
            socket = null;

            messageField.setEnabled(false);
            sendButton.setEnabled(false);
            disconnectButton.setEnabled(false);
            connectButton.setEnabled(true);
            sendFileButton.setEnabled(false);

            hostField.setEnabled(true);
            portField.setEnabled(true);

            chatArea.append("Disconnected from server.\n");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error disconnecting: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

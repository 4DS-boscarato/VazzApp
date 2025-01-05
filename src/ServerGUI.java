// Aggiornamento di ServerGUI.java per gestire utenti attivi e disconnessi

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerGUI {
    private JFrame frame;
    private JTextArea logArea;
    private JList<String> activeUserList;
    private JList<String> disconnectedUserList;
    private DefaultListModel<String> activeUserListModel;
    private DefaultListModel<String> disconnectedUserListModel;
    private JButton startButton;
    private JButton stopButton;
    private JTextField portField;
    private ServerSocket serverSocket;
    private boolean isRunning = false;

    // Gestione utenti e messaggi
    private final Map<String, PrintWriter> connectedUsers = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerGUI::new);
    }

    public ServerGUI() {
        initializeGUI();
    }

    private void initializeGUI() {
        frame = new JFrame("Server - Vazzapp");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 600);
        frame.setLayout(new BorderLayout());

        // Area di log
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);

        // Liste utenti attivi e disconnessi
        activeUserListModel = new DefaultListModel<>();
        activeUserList = new JList<>(activeUserListModel);
        JScrollPane activeUserScrollPane = new JScrollPane(activeUserList);
        activeUserScrollPane.setBorder(BorderFactory.createTitledBorder("Active Users"));

        disconnectedUserListModel = new DefaultListModel<>();
        disconnectedUserList = new JList<>(disconnectedUserListModel);
        JScrollPane disconnectedUserScrollPane = new JScrollPane(disconnectedUserList);
        disconnectedUserScrollPane.setBorder(BorderFactory.createTitledBorder("Disconnected Users"));

        // Pannello superiore con controlli
        startButton = new JButton("Start Server");
        stopButton = new JButton("Stop Server");
        stopButton.setEnabled(false);
        portField = new JTextField("7777", 5);

        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Port:"));
        topPanel.add(portField);
        topPanel.add(startButton);
        topPanel.add(stopButton);

        // Layout finale
        JPanel userPanel = new JPanel(new GridLayout(1, 2));
        userPanel.add(activeUserScrollPane);
        userPanel.add(disconnectedUserScrollPane);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(logScrollPane, BorderLayout.CENTER);
        frame.add(userPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void startServer() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            if (port < 1024 || port > 65535) {
                throw new IllegalArgumentException("Port must be between 1024 and 65535.");
            }

            serverSocket = new ServerSocket(port);
            log("[DEBUG] ServerSocket created and listening on port " + port);
            isRunning = true;
            log("Server started on port " + port);
            startButton.setEnabled(false);
            stopButton.setEnabled(true);

            new Thread(() -> {
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        log("[DEBUG] New client connection accepted: " + clientSocket.getRemoteSocketAddress());
                        new Thread(() -> handleClient(clientSocket)).start();
                    } catch (IOException e) {
                        log("Error accepting client: " + e.getMessage());
                    }
                }
            }).start();
        } catch (NumberFormatException e) {
            log("Invalid port number.");
        } catch (IllegalArgumentException e) {
            log(e.getMessage());
        } catch (IOException e) {
            log("Error starting server: " + e.getMessage());
        }
    }

    private void stopServer() {
        try {
            isRunning = false;
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            log("Server stopped.");
            log("[DEBUG] Server socket closed.");
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
        catch (IOException e) {
            log("Error stopping server: " + e.getMessage());
        }
    }

    private void handleClient(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        )
        {
            log("Client connected: " + socket.getRemoteSocketAddress());
            out.println("Enter your username:");
            out.println("(Or type 'register' to create a new account)");

            BufferedReader userFileReader = new BufferedReader(new FileReader("users.txt"));
            Map<String, String> credentials = new ConcurrentHashMap<>();
            String line;
            while ((line = userFileReader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    credentials.put(parts[0], parts[1]);
                }
            }
            userFileReader.close();

            String username;
            while (true) {
                username = in.readLine();
                if (username == null || username.trim().isEmpty()) {
                    out.println("Username cannot be empty. Enter another:");
                } else if (connectedUsers.containsKey(username)) {
                    out.println("Username already in use. Enter another:");
                } else {
                    synchronized (connectedUsers) {
                        connectedUsers.put(username, out);
                        log("[DEBUG] User added: " + username);
                        activeUserListModel.addElement(username + " (" + socket.getRemoteSocketAddress() + ")");
                        disconnectedUserListModel.removeElement(username); // Rimuove dalla lista disconnessi
                    }
                    broadcast("[Server]: " + username + " joined the chat.");
                    break;
                }
            }

            String message;
            while ((message = in.readLine()) != null) {
                if (message.length() > 200) {
                    out.println("Message too long (max 200 characters).");
                    continue;
                }
                String logEntry = username + ": " + message;
                broadcast(logEntry);
            }
        } catch (IOException e) {
            log("Client disconnected unexpectedly.");
        } finally {
            synchronized (connectedUsers) {
                connectedUsers.remove(socket.getRemoteSocketAddress());
                log("[DEBUG] User removed: " + socket.getRemoteSocketAddress());
                activeUserListModel.removeElement(socket.getRemoteSocketAddress());
                disconnectedUserListModel.addElement(socket.getRemoteSocketAddress().toString());
                broadcast("[Server]: " + socket.getRemoteSocketAddress() + " left the chat.");
            }
        }
    }

    private void broadcast(String message) {
        synchronized (connectedUsers) {
            log(message);
            for (PrintWriter writer : connectedUsers.values()) {
                writer.println(message);
            }
        }
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }
}

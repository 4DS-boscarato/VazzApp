import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ServerGUI {
    private static final int SERVER_PORT = 777;
    private JFrame frame;
    private JTextArea logArea;
    private JButton startButton;
    private JButton stopButton;
    private ServerSocket serverSocket;
    private boolean isRunning = false;

    // Mappe per gestire partecipanti e messaggi
    private final Map<String, PrintWriter> partecipanti = new HashMap<>();
    private final Map<String, String> messaggi = new HashMap<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerGUI::new);
    }

    public ServerGUI() {
        initializeGUI();
    }

    private void initializeGUI() {
        frame = new JFrame("Chat Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 500);

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        startButton = new JButton("Start Server");
        stopButton = new JButton("Stop Server");
        stopButton.setEnabled(false);

        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                isRunning = true;
                appendLog("Server started on port " + SERVER_PORT);
                startButton.setEnabled(false);
                stopButton.setEnabled(true);

                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleClient(clientSocket)).start();
                }
            } catch (IOException e) {
                appendLog("Error starting server: " + e.getMessage());
            }
        }).start();
    }

    private void stopServer() {
        try {
            isRunning = false;
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            appendLog("Server stopped.");
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        } catch (IOException e) {
            appendLog("Error stopping server: " + e.getMessage());
        }
    }

    private void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            appendLog("Client connected: " + socket.getRemoteSocketAddress());
            out.println("Enter your username:");

            String username;
            while (true) {
                username = in.readLine();
                if (username == null || username.trim().isEmpty() || partecipanti.containsKey(username)) {
                    out.println("Invalid or already used username. Enter another:");
                } else {
                    synchronized (partecipanti) {
                        partecipanti.put(username, out);
                        broadcast("[Server]: " + username + " joined the chat.");
                    }
                    break;
                }
            }

            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("READ:")) {
                    String messageId = message.substring(5);
                    synchronized (messaggi) {
                        messaggi.put(messageId, "Read");
                    }
                } else {
                    String messageId = String.valueOf(System.currentTimeMillis());
                    synchronized (messaggi) {
                        messaggi.put(messageId, "Sent");
                    }
                    broadcast(username + ": " + message + " (ID: " + messageId + ")");
                }
            }

        } catch (IOException e) {
            appendLog("Client disconnected: " + e.getMessage());
        } finally {
            synchronized (partecipanti) {
                if (socket != null) {
                    partecipanti.values().removeIf(out -> out.checkError());
                }
            }
        }
    }

    private void broadcast(String message) {
        synchronized (partecipanti) {
            appendLog(message);
            partecipanti.values().forEach(writer -> writer.println(message));
        }
    }

    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }
}

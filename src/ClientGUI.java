import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ClientGUI {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JTextField portField;
    private JTextField hostField;
    private JButton connectButton;
    private JButton sendButton;

    private static final Map<String, String> userDatabase = new HashMap<>(); // Mappa per memorizzare utenti

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new); // Punto d'ingresso dell'applicazione
    }

    public ClientGUI() {
        initializeFrame();
        showHomePage();
    }

    private void initializeFrame() {
        frame = new JFrame("Chat Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 500);
        frame.setLayout(new BorderLayout());

        // Creazione del menu
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("wgazione");

        JMenuItem homeItem = new JMenuItem("Homepage");
        homeItem.addActionListener(e -> showHomePage());

        JMenuItem loginItem = new JMenuItem("Accedi");
        loginItem.addActionListener(e -> showLoginPage());

        JMenuItem registerItem = new JMenuItem("Registrati");
        registerItem.addActionListener(e -> showRegistrationPage());

        JMenuItem exitItem = new JMenuItem("Esci");
        exitItem.addActionListener(e -> System.exit(0));

        menu.add(homeItem);
        menu.add(loginItem);
        menu.add(registerItem);
        menu.add(exitItem);

        menuBar.add(menu);
        frame.setJMenuBar(menuBar);

        // Rendi visibile il frame
        frame.setVisible(true);
    }

    private void showHomePage() {
        frame.getContentPane().removeAll();
        frame.setTitle("Chat Client - Homepage");

        // Logo e titolo
        JLabel logo = new JLabel("LOGO", SwingConstants.CENTER);
        logo.setFont(new Font("Arial", Font.BOLD, 48));

        JLabel title = new JLabel("Benvenuto in Vazzapp", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 30));

        // Pulsanti per accedere o registrarsi
        JButton loginButton = new JButton("Accedi");
        loginButton.addActionListener(e -> showLoginPage());

        JButton registerButton = new JButton("Registrati");
        registerButton.addActionListener(e -> showRegistrationPage());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(2, 1, 10, 10));
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        contentPanel.add(logo, BorderLayout.NORTH);
        contentPanel.add(title, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        frame.add(contentPanel, BorderLayout.CENTER);
        frame.revalidate();
        frame.repaint();
    }

    private void showLoginPage() {
        frame.getContentPane().removeAll();
        frame.setTitle("Chat Client - Login");

        JLabel userLabel = new JLabel("Username:");
        JTextField usernameField = new JTextField(15);
        JLabel passLabel = new JLabel("Password:");
        JPasswordField passwordField = new JPasswordField(15);
        JButton loginButton = new JButton("Accedi");

        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            if (authenticateUser(username, password)) {
                JOptionPane.showMessageDialog(frame, "Accesso effettuato con successo!", "Successo", JOptionPane.INFORMATION_MESSAGE);
                showChatPage(username);
            } else {
                JOptionPane.showMessageDialog(frame, "Credenziali non valide.", "Errore", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 2));
        panel.add(userLabel);
        panel.add(usernameField);
        panel.add(passLabel);
        panel.add(passwordField);
        panel.add(new JLabel());
        panel.add(loginButton);

        frame.add(panel);
        frame.revalidate();
        frame.repaint();
    }

    private void showRegistrationPage() {
        frame.getContentPane().removeAll();
        frame.setTitle("Chat Client - Registrazione");

        JLabel userLabel = new JLabel("Username:");
        JTextField usernameField = new JTextField(15);
        JLabel passLabel = new JLabel("Password:");
        JPasswordField passwordField = new JPasswordField(15);
        JButton registerButton = new JButton("Registrati");

        registerButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Compila tutti i campi.", "Errore", JOptionPane.ERROR_MESSAGE);
            } else if (registerUser(username, password)) {
                JOptionPane.showMessageDialog(frame, "Registrazione completata! Ora puoi accedere.", "Successo", JOptionPane.INFORMATION_MESSAGE);
                showLoginPage();
            } else {
                JOptionPane.showMessageDialog(frame, "Username già esistente.", "Errore", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 2));
        panel.add(userLabel);
        panel.add(usernameField);
        panel.add(passLabel);
        panel.add(passwordField);
        panel.add(new JLabel());
        panel.add(registerButton);

        frame.add(panel);
        frame.revalidate();
        frame.repaint();
    }

    private void showChatPage(String username) {
        frame.getContentPane().removeAll();
        frame.setTitle("Chat Client - Chat (" + username + ")");

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        messageField = new JTextField();
        sendButton = new JButton("Send");

        sendButton.addActionListener(e -> sendMessage(username));
        messageField.addActionListener(e -> sendMessage(username));

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        portField = new JTextField("777", 5);
        hostField = new JTextField("localhost", 10);
        connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connectToServer(username));

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Host:"));
        topPanel.add(hostField);
        topPanel.add(new JLabel("Port:"));
        topPanel.add(portField);
        topPanel.add(connectButton);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);

        frame.revalidate();
        frame.repaint();
    }

    private boolean registerUser(String username, String password) {
        if (userDatabase.containsKey(username)) {
            return false; // Username già esistente
        }
        userDatabase.put(username, password);
        return true;
    }

    private boolean authenticateUser(String username, String password) {
        return userDatabase.containsKey(username) && userDatabase.get(username).equals(password);
    }

    private void connectToServer(String username) {
        // Logica per connettersi al server
        JOptionPane.showMessageDialog(frame, "Connessione simulata per: " + username);
    }

    private void sendMessage(String username) {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            chatArea.append(username + ": " + message + "\n");
            messageField.setText("");
        }
    }
}

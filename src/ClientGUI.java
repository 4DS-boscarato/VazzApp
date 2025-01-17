import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientGUI {
    private JFrame finestra;
    private JTextArea areaChat;
    private JTextField campoMessaggio;
    private JTextField campoPorta;
    private JTextField campoHost;
    private JButton bottoneConnetti;
    private JButton bottoneInvia;
    private JButton bottoneDisconnetti;
    private JButton bottoneInviaFile;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private List<String> utentiConnessi = new ArrayList<>();
    private DefaultListModel<String> modelloListaUtenti;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new);
    }

    public ClientGUI() {
        inizializzaGUI();
    }

    private void inizializzaGUI() {
        finestra = new JFrame("Client di Chat");
        finestra.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        finestra.setSize(700, 500);

        areaChat = new JTextArea();
        areaChat.setEditable(false);
        JScrollPane scrollPaneChat = new JScrollPane(areaChat);

        campoMessaggio = new JTextField();
        campoMessaggio.setEnabled(false);

        bottoneInvia = new JButton("Invia");
        bottoneInvia.setEnabled(false);

        bottoneDisconnetti = new JButton("Disconnetti");
        bottoneDisconnetti.setEnabled(false);

        bottoneInviaFile = new JButton("Invia File");
        bottoneInviaFile.setEnabled(false);

        bottoneInvia.addActionListener(e -> inviaMessaggio());
        campoMessaggio.addActionListener(e -> inviaMessaggio());
        bottoneDisconnetti.addActionListener(e -> disconnettiDalServer());
        bottoneInviaFile.addActionListener(e -> inviaFile());

        JPanel pannelloInput = new JPanel(new BorderLayout());
        pannelloInput.add(campoMessaggio, BorderLayout.CENTER);
        pannelloInput.add(bottoneInvia, BorderLayout.EAST);

        campoPorta = new JTextField("7777", 5);
        campoHost = new JTextField("localhost", 10);

        bottoneConnetti = new JButton("Connetti");
        bottoneConnetti.addActionListener(e -> connettiAlServer());

        JPanel pannelloSuperiore = new JPanel();
        pannelloSuperiore.add(new JLabel("Host:"));
        pannelloSuperiore.add(campoHost);
        pannelloSuperiore.add(new JLabel("Porta:"));
        pannelloSuperiore.add(campoPorta);
        pannelloSuperiore.add(bottoneConnetti);
        pannelloSuperiore.add(bottoneDisconnetti);
        pannelloSuperiore.add(bottoneInviaFile);

        modelloListaUtenti = new DefaultListModel<>();
        JList<String> listaUtenti = new JList<>(modelloListaUtenti);
        listaUtenti.setFixedCellWidth(150);
        JScrollPane scrollPaneUtenti = new JScrollPane(listaUtenti);
        scrollPaneUtenti.setBorder(BorderFactory.createTitledBorder("Utenti Connessi"));

        finestra.setLayout(new BorderLayout());
        finestra.add(pannelloSuperiore, BorderLayout.NORTH);
        finestra.add(scrollPaneChat, BorderLayout.CENTER);
        finestra.add(pannelloInput, BorderLayout.SOUTH);
        finestra.add(scrollPaneUtenti, BorderLayout.EAST);

        finestra.setVisible(true);
    }

    private void connettiAlServer() {
        try {
            int porta = Integer.parseInt(campoPorta.getText().trim());
            String host = campoHost.getText().trim();

            socket = new Socket(host, porta);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            new Thread(this::ascoltaMessaggi).start();

            String nomeUtente = JOptionPane.showInputDialog(finestra, "Inserisci il tuo nome utente:", "Login", JOptionPane.PLAIN_MESSAGE);
            if (nomeUtente == null || nomeUtente.trim().isEmpty()) {
                JOptionPane.showMessageDialog(finestra, "Il nome utente è richiesto per unirsi alla chat.", "Errore", JOptionPane.ERROR_MESSAGE);
                return;
            }

            out.println(nomeUtente);
            campoMessaggio.setEnabled(true);
            bottoneInvia.setEnabled(true);
            bottoneDisconnetti.setEnabled(true);
            bottoneConnetti.setEnabled(false);
            bottoneInviaFile.setEnabled(true);

            campoHost.setEnabled(false);
            campoPorta.setEnabled(false);
        } catch (IOException | NumberFormatException e) {
            JOptionPane.showMessageDialog(finestra, "Impossibile connettersi al server: " + e.getMessage(), "Errore di Connessione", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void inviaMessaggio() {
        try {
            String messaggio = campoMessaggio.getText().trim();
            if (messaggio.isEmpty()) {
                JOptionPane.showMessageDialog(finestra, "Il messaggio non può essere vuoto.", "Errore", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (messaggio.length() > 200) {
                JOptionPane.showMessageDialog(finestra, "Messaggio troppo lungo (max 200 caratteri).", "Errore", JOptionPane.ERROR_MESSAGE);
                return;
            }

            out.println(messaggio);
            campoMessaggio.setText("");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(finestra, "Errore nell'invio del messaggio: " + e.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void inviaFile() {
        JFileChooser selettoreFile = new JFileChooser();
        int returnValue = selettoreFile.showOpenDialog(finestra);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = selettoreFile.getSelectedFile();

            if (!file.exists() || !file.isFile()) {
                JOptionPane.showMessageDialog(finestra, "File non valido selezionato.", "Errore", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try (Socket fileSocket = new Socket(socket.getInetAddress(), 7778); // Porta separata per file
                 BufferedInputStream inFile = new BufferedInputStream(new FileInputStream(file));
                 DataOutputStream outFileSocket = new DataOutputStream(fileSocket.getOutputStream())) {

                // Notifica al server che un file è in arrivo
                out.println("FILE:" + file.getName());

                // Invia nome e dimensione del file
                outFileSocket.writeUTF(file.getName());
                outFileSocket.writeLong(file.length());

                // Invia il contenuto del file
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inFile.read(buffer)) != -1) {
                    outFileSocket.write(buffer, 0, bytesRead);
                }
                outFileSocket.flush();

                JOptionPane.showMessageDialog(finestra, "File inviato correttamente: " + file.getName(), "Successo", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(finestra, "Errore nell'invio del file: " + e.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private void ascoltaMessaggi() {
        try {
            String messaggio;
            while ((messaggio = in.readLine()) != null) {
                if (messaggio.startsWith("FILE:")) {
                    riceviFile(messaggio.substring(5));
                } else if (messaggio.startsWith("[Server]: Utenti connessi:")) {
                    aggiornaListaUtenti(messaggio);
                } else {
                    areaChat.append(messaggio + "\n");
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(finestra, "Connessione persa.", "Errore", JOptionPane.ERROR_MESSAGE);
            disconnettiDalServer();
        }
    }

    private void riceviFile(String nomeFile) {
        try (BufferedOutputStream outFile = new BufferedOutputStream(new FileOutputStream(nomeFile));
             InputStream inSocket = socket.getInputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inSocket.read(buffer)) != -1) {
                outFile.write(buffer, 0, bytesRead);
            }
            outFile.flush();
            areaChat.append("File ricevuto: " + nomeFile + "\n");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(finestra, "Errore nella ricezione del file: " + e.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void aggiornaListaUtenti(String messaggio) {
        String listaUtentiString = messaggio.substring(messaggio.indexOf(":") + 2);
        String[] utenti = listaUtentiString.split(", ");

        SwingUtilities.invokeLater(() -> {
            modelloListaUtenti.clear();
            for (String utente : utenti) {
                modelloListaUtenti.addElement(utente);
            }
        });
    }

    private void disconnettiDalServer() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            out = null;
            in = null;
            socket = null;

            campoMessaggio.setEnabled(false);
            bottoneInvia.setEnabled(false);
            bottoneDisconnetti.setEnabled(false);
            bottoneConnetti.setEnabled(true);
            bottoneInviaFile.setEnabled(false);

            campoHost.setEnabled(true);
            campoPorta.setEnabled(true);

            areaChat.append("Disconnesso dal server.\n");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(finestra, "Errore nella disconnessione: " + e.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }
}
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ServerGUI {
    private JFrame finestra;
    private JTextArea areaLog;
    private JList<String> listaUtentiAttivi;
    private JList<String> listaUtentiDisconnessi;
    private DefaultListModel<String> modelloListaUtentiAttivi;
    private DefaultListModel<String> modelloListaUtentiDisconnessi;
    private JButton bottoneAvvio;
    private JButton bottoneFermo;
    private JTextField campoPorta;
    private ServerSocket socketServer;
    private ServerSocket socketFileServer;
    private boolean inEsecuzione = false;
    private final Map<String, PrintWriter> utentiConnessi = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerGUI::new);
    }

    public ServerGUI() {
        inizializzaGUI();
        preparaCartellaFile();
    }

    private void inizializzaGUI() {
        finestra = new JFrame("Server - Vazzapp");
        finestra.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        finestra.setSize(700, 600);
        finestra.setLayout(new BorderLayout());

        areaLog = new JTextArea();
        areaLog.setEditable(false);
        JScrollPane scrollPaneLog = new JScrollPane(areaLog);

        modelloListaUtentiAttivi = new DefaultListModel<>();
        listaUtentiAttivi = new JList<>(modelloListaUtentiAttivi);
        JScrollPane scrollPaneUtentiAttivi = new JScrollPane(listaUtentiAttivi);
        scrollPaneUtentiAttivi.setBorder(BorderFactory.createTitledBorder("Utenti Attivi"));

        modelloListaUtentiDisconnessi = new DefaultListModel<>();
        listaUtentiDisconnessi = new JList<>(modelloListaUtentiDisconnessi);
        JScrollPane scrollPaneUtentiDisconnessi = new JScrollPane(listaUtentiDisconnessi);
        scrollPaneUtentiDisconnessi.setBorder(BorderFactory.createTitledBorder("Utenti Disconnessi"));

        bottoneAvvio = new JButton("Avvia Server");
        bottoneFermo = new JButton("Ferma Server");
        bottoneFermo.setEnabled(false);
        campoPorta = new JTextField("7777", 5);

        bottoneAvvio.addActionListener(e -> avviaServer());
        bottoneFermo.addActionListener(e -> fermaServer());

        JPanel pannelloSuperiore = new JPanel();
        pannelloSuperiore.add(new JLabel("Porta:"));
        pannelloSuperiore.add(campoPorta);
        pannelloSuperiore.add(bottoneAvvio);
        pannelloSuperiore.add(bottoneFermo);

        JPanel pannelloUtenti = new JPanel(new GridLayout(1, 2));
        pannelloUtenti.add(scrollPaneUtentiAttivi);
        pannelloUtenti.add(scrollPaneUtentiDisconnessi);

        finestra.add(pannelloSuperiore, BorderLayout.NORTH);
        finestra.add(scrollPaneLog, BorderLayout.CENTER);
        finestra.add(pannelloUtenti, BorderLayout.SOUTH);

        finestra.setVisible(true);
    }

    private void avviaSocketFile(int portaFile) {
        try {
            socketFileServer = new ServerSocket(portaFile);
            registra("Socket file avviato sulla porta " + portaFile);
            new Thread(this::gestisciConnessioniFile).start();
        } catch (IOException e) {
            registra("Errore nell'avvio del socket file sulla porta " + portaFile + ": " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void avviaServer() {
        try {
            int porta = Integer.parseInt(campoPorta.getText().trim());
            if (porta < 1024 || porta > 65535) {
                throw new IllegalArgumentException("La porta deve essere tra 1024 e 65535.");
            }

            socketServer = new ServerSocket(porta);
            inEsecuzione = true;
            registra("Server avviato sulla porta " + porta);

            // Avvia il socket per i file sulla porta successiva
            avviaSocketFile(porta + 1);

            bottoneAvvio.setEnabled(false);
            bottoneFermo.setEnabled(true);

            new Thread(this::gestisciConnessioniClient).start();
        } catch (Exception e) {
            registra("Errore nell'avvio del server: " + e.getMessage());
        }
    }


    private void fermaServer() {
        try {
            inEsecuzione = false;
            if (socketServer != null && !socketServer.isClosed()) {
                socketServer.close();
            }
            if (socketFileServer != null && !socketFileServer.isClosed()) {
                socketFileServer.close();
            }
            registra("Server fermato.");
            bottoneAvvio.setEnabled(true);
            bottoneFermo.setEnabled(false);
        } catch (IOException e) {
            registra("Errore nella fermata del server: " + e.getMessage());
        }
    }

    private void gestisciConnessioniClient() {
        while (inEsecuzione) {
            try {
                Socket socketClient = socketServer.accept();
                new Thread(() -> gestisciClient(socketClient)).start();
            } catch (IOException e) {
                registra("Errore nell'accettazione del client: " + e.getMessage());
            }
        }
    }

    private void gestisciConnessioniFile() {
        while (inEsecuzione) {
            try {
                Socket socketFile = socketFileServer.accept();
                new Thread(() -> riceviFile(socketFile)).start();
            } catch (IOException e) {
                registra("Errore nel socket file: " + e.getMessage());
            }
        }
    }

    private void gestisciClient(Socket socket) {
        String nomeUtente = null;
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            out.println("Benvenuto! Inserisci il tuo nome utente:");
            while ((nomeUtente = in.readLine()) != null) {
                if (utentiConnessi.containsKey(nomeUtente)) {
                    out.println("Nome utente già in uso. Inserisci un altro:");
                } else {
                    utentiConnessi.put(nomeUtente, out);
                    modelloListaUtentiAttivi.addElement(nomeUtente);
                    trasmetti("[Server]: " + nomeUtente + " si è unito alla chat.");
                    trasmettiListaUtenti();
                    break;
                }
            }

            String messaggio;
            while ((messaggio = in.readLine()) != null) {
                trasmetti(nomeUtente + ": " + messaggio);
            }
        } catch (IOException e) {
            registra("Errore con il client: " + e.getMessage());
        } finally {
            if (nomeUtente != null) {
                utentiConnessi.remove(nomeUtente);
                modelloListaUtentiAttivi.removeElement(nomeUtente);
                modelloListaUtentiDisconnessi.addElement(nomeUtente);
                trasmetti("[Server]: " + nomeUtente + " ha lasciato la chat.");
                trasmettiListaUtenti();
            }
        }
    }

    private void riceviFile(Socket socket) {
        try (
                DataInputStream in = new DataInputStream(socket.getInputStream());
                FileOutputStream outFile = new FileOutputStream("files/" + in.readUTF())
        ) {
            long fileSize = in.readLong();
            byte[] buffer = new byte[4096];
            long bytesRead = 0;

            while (bytesRead < fileSize) {
                int read = in.read(buffer);
                if (read == -1) break;
                outFile.write(buffer, 0, read);
                bytesRead += read;
            }

            registra("File ricevuto correttamente.");
        } catch (IOException e) {
            registra("Errore nella ricezione del file: " + e.getMessage());
        }
    }

    private void trasmetti(String messaggio) {
        synchronized (utentiConnessi) {
            registra(messaggio);
            utentiConnessi.values().forEach(out -> out.println(messaggio));
        }
    }

    private void trasmettiListaUtenti() {
        String lista = utentiConnessi.keySet().stream().collect(Collectors.joining(", "));
        trasmetti("[Server]: Utenti connessi: " + lista);
    }

    private void registra(String messaggio) {
        SwingUtilities.invokeLater(() -> areaLog.append(messaggio + "\n"));
    }

    private void preparaCartellaFile() {
        File directory = new File("files");
        if (!directory.exists() && directory.mkdir()) {
            registra("Cartella 'files' creata per i file ricevuti.");
        }
    }
}

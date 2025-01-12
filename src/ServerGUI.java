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
    private boolean inEsecuzione = false;
    private final Map<String, PrintWriter> utentiConnessi = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerGUI::new);
    }

    public ServerGUI() {
        inizializzaGUI();
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

    private void avviaServer() {
        try {
            int porta = Integer.parseInt(campoPorta.getText().trim());
            if (porta < 1024 || porta > 65535) {
                throw new IllegalArgumentException("La porta deve essere tra 1024 e 65535.");
            }

            socketServer = new ServerSocket(porta);
            registra("[DEBUG] ServerSocket creato e in ascolto sulla porta " + porta);
            inEsecuzione = true;
            registra("Server avviato sulla porta " + porta);
            bottoneAvvio.setEnabled(false);
            bottoneFermo.setEnabled(true);

            new Thread(() -> {
                while (inEsecuzione) {
                    try {
                        Socket socketClient = socketServer.accept();
                        registra("[DEBUG] Nuova connessione client accettata: " + socketClient.getRemoteSocketAddress());
                        new Thread(() -> gestisciClient(socketClient)).start();
                    } catch (IOException e) {
                        registra("Errore nell'accettazione del client: " + e.getMessage());
                    }
                }
            }).start();
        } catch (NumberFormatException e) {
            registra("Numero di porta non valido.");
        } catch (IllegalArgumentException e) {
            registra(e.getMessage());
        } catch (IOException e) {
            registra("Errore nell'avvio del server: " + e.getMessage());
        }
    }

    private void fermaServer() {
        try {
            inEsecuzione = false;
            if (socketServer != null && !socketServer.isClosed()) {
                socketServer.close();
            }
            registra("Server fermato.");
            registra("[DEBUG] Server socket chiuso.");
            bottoneAvvio.setEnabled(true);
            bottoneFermo.setEnabled(false);
        } catch (IOException e) {
            registra("Errore nella fermata del server: " + e.getMessage());
        }
    }

    private void gestisciClient(Socket socket) {
        String nomeUtente = null;
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        )
        {
            registra("Client connesso: " + socket.getRemoteSocketAddress());

            while (true) {
                nomeUtente = in.readLine();
                if (nomeUtente == null) {
                    throw new IOException("Client disconnesso durante la lettura del nome utente.");
                }
                nomeUtente = nomeUtente.trim();

                if (nomeUtente.equalsIgnoreCase("registra")) {
                    // Implementazione per la registrazione di un nuovo utente
                    out.println("Inserisci un nuovo nome utente:");
                    nomeUtente = in.readLine();
                    if (nomeUtente == null || nomeUtente.trim().isEmpty()) {
                        out.println("Nome utente non valido. Disconnessione.");
                        return;
                    }
                    // Controlla se l'utente esiste già, salva nel file, ecc.
                }

                if (nomeUtente.isEmpty()) {
                    out.println("Il nome utente non può essere vuoto. Inserisci un altro:");
                }
                else if (utentiConnessi.containsKey(nomeUtente)) {
                    out.println("Nome utente già in uso. Inserisci un altro:");
                }
                else {
                    synchronized (utentiConnessi) {
                        utentiConnessi.put(nomeUtente, out);
                        registra("[DEBUG] Utente aggiunto: " + nomeUtente);
                        modelloListaUtentiAttivi.addElement(nomeUtente + " (" + socket.getRemoteSocketAddress() + ")");
                        modelloListaUtentiDisconnessi.removeElement(nomeUtente); // Rimuove dalla lista disconnessi
                        trasmettiListaUtenti();
                    }
                    trasmetti("[Server]: " + nomeUtente + " si è unito alla chat.");
                    break;
                }
            }

            String messaggio;
            while ((messaggio = in.readLine()) != null) {
                if (messaggio.startsWith("FILE:")) {
                    riceviFile(messaggio.substring(5), socket);
                } else if (messaggio.length() > 200) {
                    out.println("Messaggio troppo lungo (max 200 caratteri).");
                } else {
                    String voceLog = nomeUtente + ": " + messaggio;
                    trasmetti(voceLog);
                }
            }
        } catch (IOException e) {
            registra("Client disconnesso inaspettatamente.");
        }
        finally {
            if (nomeUtente != null) {
                synchronized (utentiConnessi) {
                    utentiConnessi.remove(nomeUtente);
                    registra("[DEBUG] Utente rimosso: " + nomeUtente);
                    modelloListaUtentiAttivi.removeElement(nomeUtente + " (" + socket.getRemoteSocketAddress() + ")");
                    modelloListaUtentiDisconnessi.addElement(nomeUtente + " (" + socket.getRemoteSocketAddress() + ")");
                    trasmetti("[Server]: " + nomeUtente + " ha lasciato la chat.");
                    trasmettiListaUtenti();
                }
            }
        }
    }

    private void riceviFile(String nomeFile, Socket socket) {
        try (BufferedOutputStream outFile = new BufferedOutputStream(new FileOutputStream(nomeFile));
             InputStream inSocket = socket.getInputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inSocket.read(buffer)) != -1) {
                outFile.write(buffer, 0, bytesRead);
                if (bytesRead == 0) { // Se read restituisce 0, potrebbe essere la fine del file
                    break;
                }
            }
            outFile.flush();
            registra("File ricevuto: " + nomeFile);
        } catch (IOException e) {
            registra("Errore nella ricezione del file: " + e.getMessage());
        }
    }

    private void trasmetti(String messaggio) {
        synchronized (utentiConnessi) {
            registra(messaggio);
            for (PrintWriter writer : utentiConnessi.values()) {
                writer.println(messaggio);
            }
        }
    }

    private void registra(String messaggio) {
        SwingUtilities.invokeLater(() -> areaLog.append(messaggio + "\n"));
    }

    private void trasmettiListaUtenti() {
        String listaUtenti = utentiConnessi.keySet().stream().collect(Collectors.joining(", "));
        trasmetti("[Server]: Utenti connessi: " + listaUtenti);
    }
}
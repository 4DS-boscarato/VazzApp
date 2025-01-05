package old;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatServer extends Thread {
    private Socket socket;
    private PrintWriter out;
    private static Map<String, PrintWriter> partecipanti = new HashMap();
    private static Map<String, String> messaggi = new HashMap();
    private String username;

    public ChatServer(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.out = new PrintWriter(this.socket.getOutputStream(), true);
            this.out.println("Inserisci il tuo nome utente:");

            for(this.username = in.readLine(); this.username == null || this.username.trim().isEmpty() || partecipanti.containsKey(this.username); this.username = in.readLine()) {
                this.out.println("Nome utente non valido o già in uso. Inserisci un altro nome utente:");
            }

            synchronized(partecipanti) {
                partecipanti.put(this.username, this.out);
                this.broadcast("[Server]: " + this.username + " si è unito alla chat.");
            }

            String message;
            while((message = in.readLine()) != null) {
                if (message.startsWith("READ:")) {
                    String messageId = message.substring(5);
                    synchronized(messaggi) {
                        messaggi.put(messageId, "Visualizzato");
                    }
                } else {
                    String messageId = UUID.randomUUID().toString();
                    synchronized(messaggi) {
                        messaggi.put(messageId, "Inviato");
                    }

                    this.broadcast(this.username + ": " + message + " (ID: " + messageId + ")");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            synchronized(partecipanti) {
                if (this.username != null) {
                    partecipanti.remove(this.username);
                    this.broadcast("[Server]: " + this.username + " ha lasciato la chat.");
                }

            }
        }

    }

    private void broadcast(String message) {
        synchronized(partecipanti) {
            for(PrintWriter writer : partecipanti.values()) {
                writer.println(message);
            }

        }
    }
}

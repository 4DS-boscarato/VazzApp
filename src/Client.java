import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 777;

    public Client() {
    }

    public static void main(String[] args) {
        Client c = new Client();
        c.comunica();
    }

    public void comunica() {
        try {
            Socket socket = new Socket("localhost", 777);

            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    try {
                        CompletableFuture.runAsync(() -> {
                            String message;
                            try {
                                while((message = in.readLine()) != null) {
                                    System.out.println("\nMessaggio ricevuto: " + message);
                                    if (message.contains("ID: ")) {
                                        String messageId = message.substring(message.indexOf("ID: ") + 4, message.indexOf(")"));
                                        out.println("READ:" + messageId);
                                    }
                                }
                            } catch (IOException e) {
                                System.out.println("Errore nel ricevere messaggi: " + e.getMessage());
                            }

                        });
                        Scanner scanner = new Scanner(System.in);
                        System.out.println("Connesso al server di chat");

                        while(true) {
                            System.out.print("Messaggio da Inviare: ");
                            String message = scanner.nextLine();
                            out.println(message);
                        }
                    } catch (Throwable var9) {
                        try {
                            in.close();
                        } catch (Throwable var8) {
                            var9.addSuppressed(var8);
                        }

                        throw var9;
                    }
                } catch (Throwable var10) {
                    try {
                        out.close();
                    } catch (Throwable var7) {
                        var10.addSuppressed(var7);
                    }

                    throw var10;
                }
            } catch (Throwable var11) {
                try {
                    socket.close();
                } catch (Throwable var6) {
                    var11.addSuppressed(var6);
                }

                throw var11;
            }
        } catch (IOException e) {
            System.out.println("Errore di connessione al server: " + e.getMessage());
        }
    }
}

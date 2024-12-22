//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import java.io.IOException;
import java.net.ServerSocket;

public class MultiServer {
    private static final int MAX_CLIENTS = 10;
    private static final int PORT = 777;

    public MultiServer() {
    }

    public void attendi() {
        try {
            new ServerSocket(1312);

            while(true) {
                try {
                    System.out.println("Chat server started...");

                    try {
                        ServerSocket serverSocket = new ServerSocket(777);

                        try {
                            while(true) {
                                new ChatServer(serverSocket.accept());
                            }
                        } catch (Throwable var6) {
                            try {
                                serverSocket.close();
                            } catch (Throwable var5) {
                                var6.addSuppressed(var5);
                            }

                            throw var6;
                        }
                    } catch (IOException e) {
                        System.out.println("Error starting server: " + e.getMessage());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        MultiServer s = new MultiServer();
        s.attendi();
    }
}

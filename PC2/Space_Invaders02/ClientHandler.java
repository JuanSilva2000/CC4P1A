package PC2.Space_Invaders02;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final int clientId;
    private final Server server;
    private PrintWriter out;
    private BufferedReader in;
    private boolean running = false;

    public ClientHandler(Socket socket, int clientId, Server server) {
        this.socket = socket;
        this.clientId = clientId;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            running = true;
            
            // Send client ID
            sendMessage("CLIENT_ID:" + clientId);
            
            String message;
            while (running && (message = in.readLine()) != null) {
                processMessage(message);
            }
        } catch (IOException e) {
            System.out.println("Error handling client " + clientId + ": " + e.getMessage());
        } finally {
            close();
            server.removeClient(clientId);
        }
    }

    private void processMessage(String message) {
        String[] parts = message.split(":", 2);
        if (parts.length != 2) return;
        
        String messageType = parts[0];
        String messageContent = parts[1];
        
        switch (messageType) {
            case "PLAYER_ACTION":
                server.handlePlayerAction(clientId, messageContent);
                break;
            default:
                System.out.println("Unknown message type from client " + clientId + ": " + messageType);
        }
    }

    public void sendMessage(String message) {
        if (out != null && !socket.isClosed()) {
            out.println(message);
        }
    }

    public void close() {
        running = false;
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing client connection: " + e.getMessage());
        }
    }
}

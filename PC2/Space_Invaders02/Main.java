package PC2.Space_Invaders02;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        String[] options = {"Server", "Client"};
        int choice = JOptionPane.showOptionDialog(
            null,
            "Would you like to start a server or client?",
            "Space Invaders Multiplayer",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );
        
        if (choice == 0) { // Server
            SwingUtilities.invokeLater(() -> {
                String portStr = JOptionPane.showInputDialog("Enter server port:", "5000");
                try {
                    int port = Integer.parseInt(portStr);
                    Server server = new Server(port);
                    new Thread(server::start).start();
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(null, "Invalid port number");
                }
            });
        } else if (choice == 1) { // Client
            SwingUtilities.invokeLater(Client::new);
        }
    }
}
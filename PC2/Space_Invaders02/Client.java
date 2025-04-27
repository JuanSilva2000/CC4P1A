package PC2.Space_Invaders02;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Client extends JFrame {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private GamePanel gamePanel;
    private int clientId = -1;
    private boolean connected = false;
    private final Map<Integer, Player> players = new HashMap<>();
    private final List<Alien> aliens = new ArrayList<>();
    private final List<UFO> ufos = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private int level = 1;
    private boolean gameOver = false;

    public Client() {
        setTitle("Space Invaders Multiplayer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Create connection panel
        JPanel connectionPanel = createConnectionPanel();
        add(connectionPanel, BorderLayout.NORTH);

        // Create game panel
        gamePanel = new GamePanel();
        add(gamePanel, BorderLayout.CENTER);

        // Set up key listeners
        setupKeyListeners();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());

        JLabel ipLabel = new JLabel("Server IP:");
        JTextField ipField = new JTextField("localhost", 10);

        JLabel portLabel = new JLabel("Port:");
        JTextField portField = new JTextField("5000", 5);

        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> {
            if (!connected) {
                String ip = ipField.getText();
                int port;
                try {
                    port = Integer.parseInt(portField.getText());
                    connect(ip, port);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid port number");
                }
            } else {
                disconnect();
                connectButton.setText("Connect");
            }
        });

        panel.add(ipLabel);
        panel.add(ipField);
        panel.add(portLabel);
        panel.add(portField);
        panel.add(connectButton);

        return panel;
    }

    private void setupKeyListeners() {
        gamePanel.setFocusable(true);
        gamePanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!connected)
                    return;

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        sendMessage("PLAYER_ACTION:MOVE_LEFT");
                        break;
                    case KeyEvent.VK_RIGHT:
                        sendMessage("PLAYER_ACTION:MOVE_RIGHT");
                        break;
                    case KeyEvent.VK_SPACE:
                        sendMessage("PLAYER_ACTION:SHOOT");
                        break;
                }
            }
        });
    }

    private void connect(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;

            // Start a thread to receive messages from the server
            new Thread(this::receiveMessages).start();

            gamePanel.requestFocus();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error connecting to server: " + e.getMessage());
        }
    }

    private void disconnect() {
        connected = false;
        try {
            if (out != null)
                out.close();
            if (in != null)
                in.close();
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            System.out.println("Error disconnecting: " + e.getMessage());
        }

        // Clear game state
        players.clear();
        aliens.clear();
        projectiles.clear();
        ufos.clear();
        gamePanel.repaint();
    }

    private void receiveMessages() {
        try {
            String message;
            while (connected && (message = in.readLine()) != null) {
                processMessage(message);
            }
        } catch (IOException e) {
            System.out.println("Connection to server lost: " + e.getMessage());
            connected = false;
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Connection to server lost");
            });
        }
    }

    private void processMessage(String message) {
        String[] parts = message.split(":", 2);
        if (parts.length < 2)
            return;

        String messageType = parts[0];
        String messageContent = parts[1];

        switch (messageType) {
            case "CLIENT_ID":
                clientId = Integer.parseInt(messageContent);
                System.out.println("Assigned client ID: " + clientId);
                break;
            case "INIT_GAME_STATE":
                parseInitialGameState(messageContent);
                break;
            case "GAME_STATE":
                parseGameState(messageContent);
                break;
            case "NEW_PLAYER":
                parseNewPlayer(messageContent);
                break;
            case "PLAYER_DISCONNECTED":
                int disconnectedId = Integer.parseInt(messageContent);
                players.remove(disconnectedId);
                break;
            case "ALIEN_DESTROYED":
                parseAlienDestroyed(messageContent);
                break;
            case "UFO_DESTROYED":
                parseUfoDestroyed(messageContent);
                break;
            case "SCORE_UPDATE":
                parseScoreUpdate(messageContent);
                break;
            case "PLAYER_HIT":
                parsePlayerHit(messageContent);
                break;
            case "GAME_OVER":
                gameOver = true;
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Game Over: " + messageContent);
                });
                break;
            case "GAME_RESET":
                gameOver = false;
                break;
            case "NEW_LEVEL":
                level = Integer.parseInt(messageContent);
                break;
        }

        // Repaint the game panel
        gamePanel.repaint();
    }

    private void parseInitialGameState(String content) {
        String[] sections = content.split(":");

        for (int i = 0; i < sections.length; i++) {
            if (sections[i].equals("LEVEL") && i + 1 < sections.length) {
                level = Integer.parseInt(sections[i + 1]);
            } else if (sections[i].equals("PLAYERS") && i + 1 < sections.length) {
                String playersData = sections[i + 1];
                parsePlayers(playersData);
            } else if (sections[i].equals("ALIENS") && i + 1 < sections.length) {
                String aliensData = sections[i + 1];
                parseAliens(aliensData);
            }
        }
    }

    private void parseGameState(String content) {
        String[] sections = content.split(":");

        for (int i = 0; i < sections.length; i++) {
            if (sections[i].equals("PLAYERS") && i + 1 < sections.length) {
                parsePlayers(sections[i + 1]);
            } else if (sections[i].equals("ALIENS") && i + 1 < sections.length) {
                parseAliens(sections[i + 1]);
            } else if (sections[i].equals("UFOS") && i + 1 < sections.length) {
                parseUfos(sections[i + 1]);
            } else if (sections[i].equals("PROJECTILES") && i + 1 < sections.length) {
                parseProjectiles(sections[i + 1]);
            }
        }
    }

    private void parsePlayers(String playersData) {
        synchronized (players) {
            if (playersData.isEmpty())
                return;

            String[] playerEntries = playersData.split(";");
            for (String entry : playerEntries) {
                if (entry.isEmpty())
                    continue;

                String[] playerAttrs = entry.split(",");
                if (playerAttrs.length >= 5) {
                    int id = Integer.parseInt(playerAttrs[0]);
                    float x = Float.parseFloat(playerAttrs[1]);
                    float y = Float.parseFloat(playerAttrs[2]);
                    int score = Integer.parseInt(playerAttrs[3]);
                    int lives = Integer.parseInt(playerAttrs[4]);

                    Player player = players.get(id);
                    if (player == null) {
                        player = new Player(id, x, y);
                        players.put(id, player);
                    } else {
                        player.setX(x);
                        player.setY(y);
                    }
                    player.setScore(score);
                    player.setLives(lives);
                }
            }
        }
    }

    private void parseAliens(String aliensData) {
        synchronized (aliens) {
            aliens.clear();
            if (aliensData.isEmpty())
                return;

            String[] alienEntries = aliensData.split(";");
            for (String entry : alienEntries) {
                if (entry.isEmpty())
                    continue;

                String[] alienAttrs = entry.split(",");
                if (alienAttrs.length >= 3) {
                    float x = Float.parseFloat(alienAttrs[0]);
                    float y = Float.parseFloat(alienAttrs[1]);
                    int type = Integer.parseInt(alienAttrs[2]);

                    aliens.add(new Alien(x, y, type));
                }
            }
        }
    }

    private void parseUfos(String ufosData) {
        synchronized (ufos) {
            ufos.clear();
            if (ufosData.isEmpty())
                return;

            String[] ufoEntries = ufosData.split(";");
            for (String entry : ufoEntries) {
                if (entry.isEmpty())
                    continue;

                String[] ufoAttrs = entry.split(",");
                if (ufoAttrs.length >= 3) {
                    float x = Float.parseFloat(ufoAttrs[0]);
                    float y = Float.parseFloat(ufoAttrs[1]);
                    int direction = Integer.parseInt(ufoAttrs[2]);

                    ufos.add(new UFO(x, y, direction));
                }
            }
        }
    }

    private void parseProjectiles(String projectilesData) {
        synchronized (projectiles) {
            projectiles.clear();
            if (projectilesData.isEmpty())
                return;

            String[] projectileEntries = projectilesData.split(";");
            for (String entry : projectileEntries) {
                if (entry.isEmpty())
                    continue;

                String[] projectileAttrs = entry.split(",");
                if (projectileAttrs.length >= 4) {
                    float x = Float.parseFloat(projectileAttrs[0]);
                    float y = Float.parseFloat(projectileAttrs[1]);
                    boolean fromAlien = Boolean.parseBoolean(projectileAttrs[2]);
                    int playerId = Integer.parseInt(projectileAttrs[3]);

                    projectiles.add(new Projectile(x, y, fromAlien, playerId));
                }
            }
        }
    }

    private void parseNewPlayer(String data) {
        String[] parts = data.split(":");
        if (parts.length >= 3) {
            int id = Integer.parseInt(parts[0]);
            float x = Float.parseFloat(parts[1]);
            float y = Float.parseFloat(parts[2]);

            Player player = new Player(id, x, y);
            players.put(id, player);
        }
    }

    private void parseAlienDestroyed(String data) {
        // We don't need to do anything here as the game state update will handle it
    }

    private void parseUfoDestroyed(String data) {
        // We don't need to do anything here as the game state update will handle it
    }

    private void parseScoreUpdate(String data) {
        String[] parts = data.split(":");
        if (parts.length >= 2) {
            int playerId = Integer.parseInt(parts[0]);
            int score = Integer.parseInt(parts[1]);

            Player player = players.get(playerId);
            if (player != null) {
                player.setScore(score);
            }
        }
    }

    private void parsePlayerHit(String data) {
        String[] parts = data.split(":");
        if (parts.length >= 2) {
            int playerId = Integer.parseInt(parts[0]);
            int lives = Integer.parseInt(parts[1]);

            Player player = players.get(playerId);
            if (player != null) {
                player.setLives(lives);
            }
        }
    }

    private void sendMessage(String message) {
        if (connected && out != null) {
            out.println(message);
        }
    }

    // Game panel for rendering
    private class GamePanel extends JPanel {
        public static final int WIDTH = 800;
        public static final int HEIGHT = 600;

        public GamePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setBackground(Color.BLACK);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            // Draw game elements
            if (connected) {
                // Draw aliens
                synchronized (aliens) {
                    for (Alien alien : aliens) {
                        drawAlien(g2d, alien);
                    }
                }

                // Draw UFOs
                synchronized (ufos) {
                    for (UFO ufo : ufos) {
                        drawUFO(g2d, ufo);
                    }
                }

                // Draw players
                synchronized (players) {
                    for (Player player : players.values()) {
                        drawPlayer(g2d, player);
                    }
                }

                // Draw projectiles
                synchronized (projectiles) {
                    for (Projectile projectile : projectiles) {
                        drawProjectile(g2d, projectile);
                    }
                }

                // Draw scoreboard
                drawScoreboard(g2d);

                // Draw level
                g2d.setColor(Color.WHITE);
                g2d.drawString("Level: " + level, 10, 20);

                // Draw game over message if needed
                if (gameOver) {
                    g2d.setColor(Color.RED);
                    g2d.setFont(new Font("Arial", Font.BOLD, 30));
                    g2d.drawString("GAME OVER", WIDTH / 2 - 100, HEIGHT / 2);
                }
            } else {
                // Display connection message
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 20));
                g2d.drawString("Connect to a server to play", WIDTH / 2 - 150, HEIGHT / 2);
            }
        }

        private void drawAlien(Graphics2D g2d, Alien alien) {
            g2d.setColor(getAlienColor(alien.getType()));

            int size = 30;
            int x = (int) alien.getX();
            int y = (int) alien.getY();

            // Draw alien based on type
            switch (alien.getType()) {
                case 1: // Large aliens
                    g2d.fillRect(x, y, size, size);
                    g2d.setColor(Color.BLACK);
                    g2d.fillRect(x + 5, y + 5, 5, 5);
                    g2d.fillRect(x + 20, y + 5, 5, 5);
                    g2d.fillRect(x + 10, y + 20, 10, 5);
                    break;
                case 2: // Medium aliens
                    g2d.fillOval(x, y, size, size);
                    g2d.setColor(Color.BLACK);
                    g2d.fillOval(x + 5, y + 5, 5, 5);
                    g2d.fillOval(x + 20, y + 5, 5, 5);
                    break;
                case 3: // Small aliens
                    g2d.fillPolygon(
                            new int[] { x + size / 2, x, x + size },
                            new int[] { y, y + size, y + size },
                            3);
                    break;
            }
        }

        private Color getAlienColor(int type) {
            switch (type) {
                case 1:
                    return Color.GREEN;
                case 2:
                    return Color.BLUE;
                case 3:
                    return Color.RED;
                default:
                    return Color.WHITE;
            }
        }

        private void drawUFO(Graphics2D g2d, UFO ufo) {
            g2d.setColor(Color.MAGENTA);
            int x = (int) ufo.getX();
            int y = (int) ufo.getY();

            // Draw UFO shape
            g2d.fillOval(x, y, 40, 20);
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillOval(x + 10, y + 5, 20, 10);
        }

        private void drawPlayer(Graphics2D g2d, Player player) {
            boolean isCurrentPlayer = player.getId() == clientId;

            // Draw player ship
            if (isCurrentPlayer) {
                g2d.setColor(Color.GREEN);
            } else {
                g2d.setColor(Color.YELLOW);
            }

            int x = (int) player.getX();
            int y = (int) player.getY();

            // Draw ship triangle
            g2d.fillPolygon(
                    new int[] { x + 15, x, x + 30 },
                    new int[] { y, y + 20, y + 20 },
                    3);

            // Draw player ID
            g2d.setColor(Color.WHITE);
            g2d.drawString("P" + player.getId(), x + 10, y - 5);

            // Draw lives indicators
            for (int i = 0; i < player.getLives(); i++) {
                g2d.fillPolygon(
                        new int[] { x + i * 10, x + i * 10 - 5, x + i * 10 + 5 },
                        new int[] { y - 15, y - 5, y - 5 },
                        3);
            }
        }

        private void drawProjectile(Graphics2D g2d, Projectile projectile) {
            if (projectile.isFromAlien()) {
                g2d.setColor(Color.RED);
            } else {
                g2d.setColor(Color.WHITE);
            }

            int x = (int) projectile.getX();
            int y = (int) projectile.getY();

            g2d.fillRect(x, y, 2, 10);
        }

        private void drawScoreboard(Graphics2D g2d) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));

            g2d.drawString("SCOREBOARD", WIDTH - 150, 20);
            g2d.drawLine(WIDTH - 150, 25, WIDTH - 10, 25);

            int y = 40;
            for (Player player : players.values()) {
                if (player.getId() == clientId) {
                    g2d.setColor(Color.GREEN);
                } else {
                    g2d.setColor(Color.WHITE);
                }

                g2d.drawString("Player " + player.getId() + ": " + player.getScore(), WIDTH - 150, y);
                y += 20;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}

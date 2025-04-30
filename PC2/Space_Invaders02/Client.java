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
import java.util.Objects;

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
    private String gameOverReason = "";
    private final List<Shield> shields = new ArrayList<>();

    public Client() {
        setTitle("Space Invaders Multiplayer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel connectionPanel = createConnectionPanel();
        add(connectionPanel, BorderLayout.NORTH);

        gamePanel = new GamePanel();
        add(gamePanel, BorderLayout.CENTER);

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
                if (!connected || gameOver)
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

            new Thread(this::receiveMessages).start();

            gamePanel.requestFocus();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error connecting to server: " + e.getMessage());
        }
    }

    private void disconnect() {
        connected = false;
        gameOver = true; // Asegurar que el estado de juego se marca como terminado

        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            players.clear();
            aliens.clear();
            projectiles.clear();
            ufos.clear();

            gamePanel.repaint();
        });
    }

    private void receiveMessages() {
        try {
            String message;
            while (connected && (message = in.readLine()) != null) {
                if(message.startsWith("GAME_OVER")){
                    connected = false;
                }
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
        if (message == null || message.isEmpty()) {
            return;
        }

        String[] parts = message.split(":", 2);
        if (parts.length < 2) {
            System.out.println("[CLIENT] Mensaje inválido recibido: " + message);
            return;
        }

        String messageType = parts[0];
        String messageContent = parts[1];

        try {
            switch (messageType) {
                case "CLIENT_ID":
                    clientId = Integer.parseInt(messageContent);
                    System.out.println("[CLIENT] Assigned client ID: " + clientId);
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
                case "SCORE_UPDATE":
                    parseScoreUpdate(messageContent);
                    break;
                case "PLAYER_HIT":
                    parsePlayerHit(messageContent);
                    break;
                case "GAME_OVER":
                    gameOver = true;
                    String reason = messageContent;
                    setGameOverReason(reason);
                    handleGameOver(reason);
                    break;
                case "NEW_LEVEL":
                    level = Integer.parseInt(messageContent);
                    break;
                case "SHIELDS":
                    parseShields(messageContent);
                    break;
                case "PROJECTILE_REMOVED":
                    int projectileHash = Integer.parseInt(messageContent);
                    removeProjectile(projectileHash);

                    Player player = players.get(clientId);
                    if (player != null) {
                        player.resetShoot();
                    }
                    break;
                case "PLAYER_SHOOT_RESET":
                    int playerId = Integer.parseInt(messageContent);
                    Player p = players.get(playerId);
                    if (p != null) {
                        p.resetShoot();
                    }
                    break;
                default:
                    System.out.println("[CLIENT] Tipo de mensaje desconocido: " + messageType);
            }
        } catch (Exception e) {
            System.err.println("[CLIENT] Error processing message '" + messageType + "': " + e.getMessage());
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {gamePanel.repaint(); gamePanel.revalidate();});
    }
    
    private void parseShields(String content) {
        synchronized (shields) {
            shields.clear();
            if (content == null || content.isEmpty()) return;

            // Extraer datos entre SHIELDS_START y SHIELDS_END
            int startIdx = content.indexOf("SHIELDS_START:");
            int endIdx = content.indexOf(":SHIELDS_END");
            if (startIdx == -1 || endIdx == -1) {
                System.out.println("[CLIENT] Formato de escudos incorrecto");
                return;
            }

            String shieldData = content.substring(startIdx + "SHIELDS_START:".length(), endIdx);
            String[] shieldEntries = shieldData.split(";");

            for (String entry : shieldEntries) {
                if (entry.isEmpty()) continue;

                try {
                    String[] parts = entry.split("\\|");
                    if (parts.length != 2) {
                        System.out.println("[CLIENT] Entrada de escudo inválida: " + entry);
                        continue;
                    }

                    // Coordenadas (x, y)
                    String[] coords = parts[0].split(",");
                    float x = Float.parseFloat(coords[0]);
                    float y = Float.parseFloat(coords[1]);

                    // Segmentos (15 caracteres: 0s o 1s)
                    String segmentsStr = parts[1];
                    boolean[][] segments = new boolean[5][3];
                    int index = 0;
                    for (int i = 0; i < 5; i++) {
                        for (int j = 0; j < 3; j++) {
                            segments[i][j] = segmentsStr.charAt(index++) == '1';
                        }
                    }

                    Shield shield = new Shield(x, y);
                    shield.setSegments(segments);
                    shields.add(shield);
                    System.out.println("[CLIENT] Escudo parseado en (" + x + ", " + y + ")");
                } catch (Exception e) {
                    System.out.println("[CLIENT] Error procesando escudo: " + e.getMessage());
                }
            }
        }
    }
    
    public void setGameOverReason(String reason) {
        this.gameOverReason = reason;
    }

    public String getGameOverReason() {
        return gameOverReason;
    }
    
    private void handleGameOver(String reason) {
        gameOver = true;
        connected = false;  // Detener todos los bucles de juego

        new Thread(() -> {
            disconnect();
            SwingUtilities.invokeLater(() -> {
                gamePanel.repaint();
                JOptionPane.showMessageDialog(
                    this, 
                    "Game Over: " + reason, 
                    "Fin del Juego", 
                    JOptionPane.INFORMATION_MESSAGE
                );
            });
        }).start();
    }
    
    private void removeProjectile(int hash){
        synchronized (projectiles){
            projectiles.removeIf(p -> System.identityHashCode(p)==hash);
        }
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
        if (content == null || content.isEmpty()) {
            System.out.println("[CLIENT] Estado de juego vacío recibido");
            return;
        }

        try {
            // Dividir el contenido en secciones usando el delimitador ":" pero solo cuando está precedido por un identificador
            Map<String, String> sections = new HashMap<>();
            String[] parts = content.split(":");
            String currentSection = null;

            for (int i = 0; i < parts.length; i++) {
                String part = parts[i].trim();

                // Si es un identificador de sección conocido
                if (part.equals("PLAYERS") || part.equals("ALIENS") || 
                    part.equals("UFOS") || part.equals("PROJECTILES") || 
                    part.equals("SHIELDS")) {

                    currentSection = part;
                    if (i + 1 < parts.length) {
                        sections.put(currentSection, parts[i+1]);
                    }
                }
            }

            // Procesar cada sección de datos
            if (sections.containsKey("PLAYERS")) {
                parsePlayers(sections.get("PLAYERS"));
            }

            if (sections.containsKey("ALIENS")) {
                parseAliens(sections.get("ALIENS"));
            }

            if (sections.containsKey("UFOS")) {
                parseUfos(sections.get("UFOS"));
            }

            if (sections.containsKey("PROJECTILES")) {
                parseProjectiles(sections.get("PROJECTILES"));
            }

            if (sections.containsKey("SHIELDS")) {
                parseShields(sections.get("SHIELDS"));
            }
        } catch (Exception e) {
            System.err.println("[CLIENT] Error parsing game state: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void parsePlayers(String playersData) {
        synchronized (players) {
            String[] entries = playersData.split(";");
            for (String entry : entries) {
                if (entry.isEmpty()) continue;

                String[] attrs = entry.split(",");
                if (attrs.length < 5) continue;

                int id = Integer.parseInt(attrs[0]);
                float x = Float.parseFloat(attrs[1]);
                float y = Float.parseFloat(attrs[2]);

                Player player = players.get(id);
                if (player == null) {
                    player = new Player(id, x, y);
                    players.put(id, player);
                    System.out.println("[CLIENT] Nuevo jugador: " + id);
                } else {
                    // Forzar actualización de posición
                    player.setX(x);
                    player.setY(y);
                    System.out.println("[CLIENT] Jugador " + id + " movido a: " + x + ", " + y);
                }
            }
        }
    }
    private void parseAliens(String aliensData) {
        synchronized (aliens) {
            aliens.clear();
            if (aliensData == null || aliensData.isEmpty()) {
                System.out.println("[CLIENT] Sin datos de aliens recibidos");
                return;
            }

            String[] alienEntries = aliensData.split(";");
            for (String entry : alienEntries) {
                if (entry == null || entry.isEmpty()) continue;

                try {
                    String[] parts = entry.split(",");
                    if (parts.length >= 3) {
                        float x = Float.parseFloat(parts[0]);
                        float y = Float.parseFloat(parts[1]);
                        int type = Integer.parseInt(parts[2]);

                        Alien alien = new Alien(x, y, type);
                        aliens.add(alien);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("[CLIENT] Error parsing alien entry: " + entry);
                }
            }

            if (aliens.size() > 0) {
                Alien firstAlien = aliens.get(0);
                System.out.println("[CLIENT] Recibidos " + aliens.size() + " aliens. Ejemplo: (" 
                    + firstAlien.getX() + ", " + firstAlien.getY() + ")");
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
        List<Projectile> newProjectiles = new ArrayList<>();

        if (projectilesData != null && !projectilesData.isEmpty()) {
            String[] entries = projectilesData.split(";");
            for (String entry : entries) {
                if (entry.isEmpty()) continue;

                String[] attrs = entry.split(",");
                if (attrs.length < 4) continue;

                float x = Float.parseFloat(attrs[0]);
                float y = Float.parseFloat(attrs[1]);
                boolean fromAlien = Boolean.parseBoolean(attrs[2]);
                int playerId = Integer.parseInt(attrs[3]);

                // Buscar si existe el proyectil por hashcode (único)
                int projectileHash = calculateProjectileHash(x, y, playerId);
                Projectile existing = findProjectile(projectileHash);

                if (existing != null) {
                    // Actualizar posición del proyectil existente
                    existing.setX(x);
                    existing.setY(y);
                    newProjectiles.add(existing);
                } else {
                    // Crear nuevo proyectil
                    Projectile p = new Projectile(x, y, fromAlien, playerId);
                    newProjectiles.add(p);
                }
            }
        }

        // Sincronizar y reemplazar lista
        synchronized (projectiles) {
            projectiles.clear();
            projectiles.addAll(newProjectiles);
        }
    }

    private Projectile findProjectile(int projectileHash) {
        for (Projectile p : projectiles) {
            if (System.identityHashCode(p) == projectileHash) {
                return p;
            }
        }
        return null;
    }
    
    private int calculateProjectileHash(float x, float y, int playerId) {
        return Objects.hash(x, y, playerId); // Hash único para identificar proyectiles
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

            if (gameOver) {
                drawGameOverScreen(g2d);
            } else if (connected) {
                drawGameElements(g2d);
            } else {
                drawConnectionScreen(g2d);
            }
        }
        
        private void drawConnectionScreen(Graphics2D g2d) {
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, WIDTH, HEIGHT);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            String text = "Conéctate al servidor";
            int textWidth = g2d.getFontMetrics().stringWidth(text);
            g2d.drawString(text, (WIDTH - textWidth) / 2, HEIGHT / 2);
        }

        private void drawGameElements(Graphics2D g2d) {
            synchronized (aliens) {
                aliens.forEach(alien -> drawAlien(g2d, alien));
            }

            synchronized (ufos) {
                ufos.forEach(ufo -> drawUFO(g2d, ufo));
            }

            synchronized (players) {
                players.values().forEach(player -> drawPlayer(g2d, player));
            }

            synchronized (projectiles) {
                projectiles.stream()
                          .filter(Projectile::isActive)
                          .forEach(p -> drawProjectile(g2d, p));
            }

            drawUIElements(g2d);
            drawShields(g2d);
        }
        
        private void drawShields(Graphics2D g2d) {
            synchronized (shields) {
                for (Shield shield : shields) {
                    int shieldX = (int) shield.getX();
                    int shieldY = (int) shield.getY();
                    boolean[][] segments = shield.getSegments();

                    // Dibujar cada segmento activo
                    for (int row = 0; row < 5; row++) {
                        for (int col = 0; col < 3; col++) {
                            if (segments[row][col]) {
                                int x = shieldX + (row * 20); // 20px de ancho por fila
                                int y = shieldY + (col * 10); // 10px de alto por columna
                                g2d.setColor(new Color(0, 200, 50)); // Verde brillante
                                g2d.fillRect(x, y, 20, 10);
                            }
                        }
                    }
                }
            }
        }
        
        private void drawGameOverScreen(Graphics2D g2d) {
            // Fondo oscurecido
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);

            // Texto principal
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 40));
            String text = "GAME OVER";
            int textWidth = g2d.getFontMetrics().stringWidth(text);
            g2d.drawString(text, (WIDTH - textWidth) / 2, HEIGHT/2 - 30);

            // Razón del fin del juego
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.PLAIN, 20));
            String reason = "Razón: " + gameOverReason;
            int reasonWidth = g2d.getFontMetrics().stringWidth(reason);
            g2d.drawString(reason, (WIDTH - reasonWidth) / 2, HEIGHT/2 + 20);

        }

        private void drawUIElements(Graphics2D g2d) {
            drawScoreboard(g2d);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("Nivel: " + level, 10, 20);

            Player player = players.get(clientId);
            if(player != null) {
                g2d.drawString("Vidas: " + player.getLives(), 10, 40);
            }
        }

        private void drawAlien(Graphics2D g2d, Alien alien) {
            if (alien == null) return;

            int x = (int) alien.getX();
            int y = (int) alien.getY();
            int size = 30;

            g2d.setColor(getAlienColor(alien.getType()));

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

            g2d.fillOval(x, y, 40, 20);
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillOval(x + 10, y + 5, 20, 10);
        }

        private void drawPlayer(Graphics2D g2d, Player player) {
            boolean isCurrentPlayer = player.getId() == clientId;
            g2d.setColor(isCurrentPlayer ? Color.GREEN : Color.YELLOW);

            // Asegurar que las coordenadas están dentro de los límites
            int x = (int) Math.max(0, Math.min(player.getX(), WIDTH - 30)); // Limitar entre 0 y 770
            int y = (int) player.getY();

            // Debug
            System.out.println("[DEBUG] Dibujando jugador " + player.getId() + " en (" + x + ", " + y + ")");

            // Cuerpo del jugador (triángulo)
            g2d.fillPolygon(
                new int[]{x + 15, x, x + 30},
                new int[]{y, y + 20, y + 20},
                3
            );

            // Texto y vidas
            g2d.setColor(Color.WHITE);
            g2d.drawString("P" + player.getId(), x + 10, y - 5);
            for (int i = 0; i < player.getLives(); i++) {
                g2d.fillPolygon(
                    new int[]{x + i * 10, x + i * 10 - 5, x + i * 10 + 5},
                    new int[]{y - 15, y - 5, y - 5},
                    3
                );
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
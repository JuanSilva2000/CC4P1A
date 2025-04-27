package PC2.Space_Invaders02;

// Server.java
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private ServerSocket serverSocket;
    private final int port;
    private boolean running = false;
    private final Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private final AtomicInteger clientIdCounter = new AtomicInteger(0);
    private final GameState gameState;
    private final Thread gameUpdateThread;
    private final List<Alien> aliens = new ArrayList<>();
    private final List<UFO> ufos = new ArrayList<>();
    private final Random random = new Random();
    private int level = 1;

    public Server(int port) {
        this.port = port;
        this.gameState = new GameState();
        this.gameUpdateThread = new Thread(this::updateGame);
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("Server started on port " + port);
            
            // Initialize game elements
            initializeGameElements();
            
            // Start game update thread
            gameUpdateThread.start();
            
            // Accept client connections
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    int clientId = clientIdCounter.incrementAndGet();
                    ClientHandler client = new ClientHandler(clientSocket, clientId, this);
                    clients.put(clientId, client);
                    new Thread(client).start();
                    
                    // Create player for the new client
                    Player newPlayer = new Player(clientId, 400, 550);
                    gameState.addPlayer(clientId, newPlayer);
                    
                    System.out.println("Client " + clientId + " connected.");
                    
                    // Send current game state to the new client
                    sendGameStateToClient(client);
                    
                    // Notify all clients about the new player
                    broadcastMessage("NEW_PLAYER:" + clientId + ":" + newPlayer.getX() + ":" + newPlayer.getY());
                } catch (IOException e) {
                    System.out.println("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Could not start server: " + e.getMessage());
        } finally {
            stop();
        }
    }

    private void initializeGameElements() {
        // Initialize aliens based on the level
        createAliens();
        
        // Schedule UFO spawning
        new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(10000); // Spawn a UFO every 10 seconds
                    if (random.nextBoolean()) { // 50% chance to spawn
                        UFO ufo = new UFO(0, 50, 1); // Start from left
                        synchronized (ufos) {
                            ufos.add(ufo);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    private void createAliens() {
        synchronized (aliens) {
            aliens.clear();
            
            // Create aliens in a grid formation
            int rows = 5;
            int cols = 11;
            int startX = 100;
            int startY = 100;
            int spacingX = 50;
            int spacingY = 40;
            
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    int x = startX + col * spacingX;
                    int y = startY + row * spacingY;
                    int type;
                    
                    // Determine alien type based on row
                    if (row < 1) {
                        type = 3; // Small aliens (30 points)
                    } else if (row < 3) {
                        type = 2; // Medium aliens (20 points)
                    } else {
                        type = 1; // Large aliens (10 points)
                    }
                    
                    aliens.add(new Alien(x, y, type));
                }
            }
        }
    }

    private void updateGame() {
        long lastUpdateTime = System.currentTimeMillis();
        int alienDirectionX = 1; // 1 for right, -1 for left
        
        while (running) {
            long currentTime = System.currentTimeMillis();
            long delta = currentTime - lastUpdateTime;
            
            if (delta >= 16) { // ~60 updates per second
                // Update all game elements
                updateAliens(delta, alienDirectionX);
                updateUFOs(delta);
                updateProjectiles(delta);
                checkCollisions();
                
                // Check if we need to change alien direction
                boolean changeDirection = checkAlienBoundary();
                if (changeDirection) {
                    alienDirectionX *= -1;
                    moveAliensDown();
                }
                
                // Check if all aliens are destroyed to create new level
                if (aliens.isEmpty()) {
                    level++;
                    createAliens();
                    broadcastMessage("NEW_LEVEL:" + level);
                }
                
                // Send updated game state to all clients
                broadcastGameState();
                
                lastUpdateTime = currentTime;
            }
            
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void updateAliens(long delta, int directionX) {
        synchronized (aliens) {
            // Move all aliens
            float moveSpeed = 0.05f * (1 + level * 0.1f); // Increase speed with level
            for (Alien alien : aliens) {
                alien.move(directionX * moveSpeed * delta, 0);
                
                // Random chance to fire
                if (random.nextInt(1000) < 5) { // 0.5% chance per update
                    Projectile shot = alien.shoot();
                    gameState.addProjectile(shot);
                }
            }
        }
    }

    private boolean checkAlienBoundary() {
        synchronized (aliens) {
            for (Alien alien : aliens) {
                if (alien.getX() <= 20 || alien.getX() >= 780) {
                    return true;
                }
            }
        }
        return false;
    }

    private void moveAliensDown() {
        synchronized (aliens) {
            for (Alien alien : aliens) {
                alien.move(0, 20);
                
                // Check if aliens reached bottom
                if (alien.getY() >= 550) {
                    broadcastMessage("GAME_OVER:ALIENS_REACHED_BOTTOM");
                    resetGame();
                    return;
                }
            }
        }
    }

    private void updateUFOs(long delta) {
        synchronized (ufos) {
            Iterator<UFO> ufoIterator = ufos.iterator();
            while (ufoIterator.hasNext()) {
                UFO ufo = ufoIterator.next();
                ufo.move(delta);
                
                // Remove UFO if it goes off-screen
                if (ufo.getX() < -50 || ufo.getX() > 850) {
                    ufoIterator.remove();
                }
            }
        }
    }

    private void updateProjectiles(long delta) {
        List<Projectile> projectiles = gameState.getProjectiles();
        synchronized (projectiles) {
            Iterator<Projectile> iterator = projectiles.iterator();
            while (iterator.hasNext()) {
                Projectile projectile = iterator.next();
                projectile.update(delta);
                
                // Remove projectiles that go off-screen
                if (projectile.getY() < 0 || projectile.getY() > 600) {
                    iterator.remove();
                }
            }
        }
    }

    private void checkCollisions() {
        List<Projectile> projectiles = gameState.getProjectiles();
        Map<Integer, Player> players = gameState.getPlayers();
        
        synchronized (projectiles) {
            Iterator<Projectile> projectileIterator = projectiles.iterator();
            
            while (projectileIterator.hasNext()) {
                Projectile projectile = projectileIterator.next();
                
                // Skip alien projectiles when checking alien collisions
                if (projectile.isFromAlien()) {
                    // Check player collisions with alien projectiles
                    synchronized (players) {
                        for (Player player : players.values()) {
                            if (player.isColliding(projectile)) {
                                player.hit();
                                projectileIterator.remove();
                                broadcastMessage("PLAYER_HIT:" + player.getId() + ":" + player.getLives());
                                
                                if (player.getLives() <= 0) {
                                    broadcastMessage("PLAYER_DIED:" + player.getId());
                                }
                                break;
                            }
                        }
                    }
                } else {
                    // Check alien collisions with player projectiles
                    boolean removedProjectile = false;
                    
                    // Check UFO collisions
                    synchronized (ufos) {
                        Iterator<UFO> ufoIterator = ufos.iterator();
                        while (ufoIterator.hasNext()) {
                            UFO ufo = ufoIterator.next();
                            if (ufo.isColliding(projectile)) {
                                // Add score to the player who shot
                                Player shooter = players.get(projectile.getPlayerId());
                                if (shooter != null) {
                                    int points = ufo.getPoints();
                                    shooter.addScore(points);
                                    broadcastMessage("SCORE_UPDATE:" + shooter.getId() + ":" + shooter.getScore());
                                }
                                
                                ufoIterator.remove();
                                projectileIterator.remove();
                                removedProjectile = true;
                                broadcastMessage("UFO_DESTROYED:" + ufo.getX() + ":" + ufo.getY());
                                break;
                            }
                        }
                    }
                    
                    if (!removedProjectile) {
                        // Check regular alien collisions
                        synchronized (aliens) {
                            Iterator<Alien> alienIterator = aliens.iterator();
                            while (alienIterator.hasNext()) {
                                Alien alien = alienIterator.next();
                                if (alien.isColliding(projectile)) {
                                    // Add score to the player who shot
                                    Player shooter = players.get(projectile.getPlayerId());
                                    if (shooter != null) {
                                        int points = alien.getPoints();
                                        shooter.addScore(points);
                                        broadcastMessage("SCORE_UPDATE:" + shooter.getId() + ":" + shooter.getScore());
                                    }
                                    
                                    alienIterator.remove();
                                    projectileIterator.remove();
                                    broadcastMessage("ALIEN_DESTROYED:" + alien.getX() + ":" + alien.getY() + ":" + alien.getType());
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void resetGame() {
        level = 1;
        createAliens();
        
        // Reset all player positions but keep scores
        Map<Integer, Player> players = gameState.getPlayers();
        synchronized (players) {
            for (Player player : players.values()) {
                player.resetPosition();
            }
        }
        
        // Clear all projectiles
        gameState.getProjectiles().clear();
        
        // Clear all UFOs
        synchronized (ufos) {
            ufos.clear();
        }
        
        broadcastMessage("GAME_RESET");
    }

    public void broadcastGameState() {
        // Create a compact representation of the game state
        StringBuilder sb = new StringBuilder();
        sb.append("GAME_STATE:");
        
        // Append player data
        synchronized (gameState.getPlayers()) {
            sb.append("PLAYERS:");
            for (Player player : gameState.getPlayers().values()) {
                sb.append(player.getId()).append(",")
                  .append(player.getX()).append(",")
                  .append(player.getY()).append(",")
                  .append(player.getScore()).append(",")
                  .append(player.getLives()).append(";");
            }
        }
        
        // Append alien data
        synchronized (aliens) {
            sb.append(":ALIENS:");
            for (Alien alien : aliens) {
                sb.append(alien.getX()).append(",")
                  .append(alien.getY()).append(",")
                  .append(alien.getType()).append(";");
            }
        }
        
        // Append UFO data
        synchronized (ufos) {
            sb.append(":UFOS:");
            for (UFO ufo : ufos) {
                sb.append(ufo.getX()).append(",")
                  .append(ufo.getY()).append(",")
                  .append(ufo.getDirection()).append(";");
            }
        }
        
        // Append projectile data
        synchronized (gameState.getProjectiles()) {
            sb.append(":PROJECTILES:");
            for (Projectile projectile : gameState.getProjectiles()) {
                sb.append(projectile.getX()).append(",")
                  .append(projectile.getY()).append(",")
                  .append(projectile.isFromAlien()).append(",")
                  .append(projectile.getPlayerId()).append(";");
            }
        }
        
        broadcastMessage(sb.toString());
    }

    private void sendGameStateToClient(ClientHandler client) {
        // Send the full game state to a specific client
        StringBuilder sb = new StringBuilder();
        sb.append("INIT_GAME_STATE:");
        sb.append("LEVEL:").append(level).append(":");
        
        // Append player data
        synchronized (gameState.getPlayers()) {
            sb.append("PLAYERS:");
            for (Player player : gameState.getPlayers().values()) {
                sb.append(player.getId()).append(",")
                  .append(player.getX()).append(",")
                  .append(player.getY()).append(",")
                  .append(player.getScore()).append(",")
                  .append(player.getLives()).append(";");
            }
        }
        
        // Append alien data
        synchronized (aliens) {
            sb.append(":ALIENS:");
            for (Alien alien : aliens) {
                sb.append(alien.getX()).append(",")
                  .append(alien.getY()).append(",")
                  .append(alien.getType()).append(";");
            }
        }
        
        client.sendMessage(sb.toString());
    }

    public void broadcastMessage(String message) {
        synchronized (clients) {
            for (ClientHandler client : clients.values()) {
                client.sendMessage(message);
            }
        }
    }

    public void removeClient(int clientId) {
        clients.remove(clientId);
        gameState.removePlayer(clientId);
        broadcastMessage("PLAYER_DISCONNECTED:" + clientId);
        System.out.println("Client " + clientId + " disconnected.");
    }

    public void handlePlayerAction(int clientId, String action) {
        Player player = gameState.getPlayer(clientId);
        if (player == null) return;
        
        switch (action) {
            case "MOVE_LEFT":
                player.move(-10, 0);
                break;
            case "MOVE_RIGHT":
                player.move(10, 0);
                break;
            case "SHOOT":
                Projectile projectile = player.shoot();
                if (projectile != null) {
                    gameState.addProjectile(projectile);
                }
                break;
            default:
                System.out.println("Unknown action: " + action);
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
            // Close all client connections
            for (ClientHandler client : clients.values()) {
                client.close();
            }
            clients.clear();
            
        } catch (IOException e) {
            System.out.println("Error closing server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter server port: ");
        int port = scanner.nextInt();
        scanner.close();
        
        Server server = new Server(port);
        server.start();
    }
}

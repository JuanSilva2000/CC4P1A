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
    private int alienDirectionX = 1;
    private final List<Shield> shields = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();

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
            
            initializeGameElements();
            
            gameUpdateThread.start();
            
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    int clientId = clientIdCounter.incrementAndGet();
                    ClientHandler client = new ClientHandler(clientSocket, clientId, this);
                    clients.put(clientId, client);
                    new Thread(client).start();
                    
                    Player newPlayer = new Player(clientId, 400, 550);
                    gameState.addPlayer(clientId, newPlayer);
                    
                    System.out.println("Client " + clientId + " connected.");
                    
                    sendGameStateToClient(client);
                    
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
        createAliens();
        createShields();
        
        new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(20000); // Spawn a UFO every 10 seconds
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
    
    private void createShields() {
        synchronized (shields) {
            shields.clear();
            int[] xPositions = {150, 350, 550}; // Posiciones X de los escudos
            for (int x : xPositions) {
                shields.add(new Shield(x, 450)); // Y debajo de los jugadores
            }
        }
    }

    private void createAliens() {
        synchronized (aliens) {
            aliens.clear();

            int rows = 5;
            int cols = 11;
            int startX = 100;
            int startY = 50; // Los alienígenas comienzan un poco más abajo en cada nivel
            int spacingX = 50;
            int spacingY = 40;

            System.out.println("[SERVER] Creating aliens for level " + level + " starting at Y: " + startY);

            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    int x = startX + col * spacingX;
                    int y = startY + row * spacingY;
                    int type;

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

            System.out.println("[SERVER] Created " + aliens.size() + " aliens");
        }
    }

    private void updateGame() {
        long lastUpdateTime = System.currentTimeMillis();
        final long TARGET_FRAME_TIME_MS = 16;
        int frameCounter = 0; // Para debugging

        while (running) {
            // Verificar si hay jugadores activos
            boolean hasPlayers = !gameState.getPlayers().isEmpty();

            if (hasPlayers) { // Solo actualizar si hay jugadores
                long currentTime = System.currentTimeMillis();
                long delta = currentTime - lastUpdateTime;

                if (delta >= TARGET_FRAME_TIME_MS) {
                    synchronized (gameState) {
                        // Calcular un valor delta normalizado para movimientos más suaves
                        float normalizedDelta = delta / 16.0f;

                        updateAliens(normalizedDelta, alienDirectionX);
                        updateUFOs(delta);
                        updateProjectiles(delta);
                        checkCollisions();
                        checkShieldCollisions();

                        boolean changeDirection = checkAlienBoundary();
                        if (changeDirection) {
                            alienDirectionX *= -1;
                            moveAliensDown();
                        }

                        if (aliens.isEmpty()) {
                            level++;
                            createAliens();
                            createShields();
                            broadcastMessage("NEW_LEVEL:" + level);
                        }
                    }

                    broadcastGameState();
                    lastUpdateTime = currentTime;

                    // Log cada 60 frames (~1 segundo) para debugging
                    frameCounter++;
                    if (frameCounter >= 60) {
                        System.out.println("[SERVER] Game state broadcasted. Level: " + level);
                        System.out.println("[SERVER] Aliens count: " + aliens.size());
                        if (!aliens.isEmpty()) {
                            Alien firstAlien = aliens.get(0);
                            System.out.println("[SERVER] Sample alien position: " + firstAlien.getX() + ", " + firstAlien.getY());
                        }
                        frameCounter = 0;
                    }
                }

                try {
                    Thread.sleep(1); // Permitir que otros hilos se ejecuten
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } else {
                try {
                    Thread.sleep(100); // Si no hay jugadores, dormir más tiempo
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    private void checkShieldCollisions() {
        List<Projectile> projectiles = new ArrayList<>(gameState.getProjectiles());
        synchronized (shields) {
            for (Shield shield : shields) {
                Iterator<Projectile> it = projectiles.iterator();
                while (it.hasNext()) {
                    Projectile p = it.next();
                    if (shield.checkCollision(p)) {
                        it.remove();
                        broadcastMessage("PROJECTILE_REMOVED:" + p.hashCode());
                    }
                }
            }
        }
    }

    private void updateAliens(float normalizedDelta, int directionX) {
        synchronized (aliens) {
            // Ajustar la velocidad base para que sea más predecible
            float baseSpeed = 0.2f; // Reducir velocidad base
            float levelModifier = Math.min(0.05f * level, 0.3f);
            float moveSpeed = baseSpeed + levelModifier;
            float actualMoveAmount = moveSpeed * directionX; 

            // Probabilidad de disparo ajustada
            float shootProbability = 1.0f + (level * 0.5f); // 1% al inicio, aumenta con niveles
            shootProbability = Math.min(shootProbability, 5.0f); // Máximo 5%

            // Seleccionar alienígenas que pueden disparar (las filas más bajas primero)
            Map<Integer, List<Alien>> aliensByColumn = new HashMap<>();

            // Organizar alienígenas por columna
            for (Alien alien : aliens) {
                int column = (int)(alien.getX() / 50);
                aliensByColumn.computeIfAbsent(column, k -> new ArrayList<>()).add(alien);
            }

            // Para cada columna, solo el alienígena más bajo puede disparar
            List<Alien> shootableAliens = new ArrayList<>();
            for (List<Alien> columnAliens : aliensByColumn.values()) {
                if (!columnAliens.isEmpty()) {
                    columnAliens.sort(Comparator.comparingDouble(Alien::getY).reversed());
                    shootableAliens.add(columnAliens.get(0));
                }
            }

            // Disparos de alienígenas
            if (!shootableAliens.isEmpty() && random.nextFloat() * 100 < shootProbability) {
                Alien shooter = shootableAliens.get(random.nextInt(shootableAliens.size()));
                if (shooter.canShoot()) {
                    Projectile shot = shooter.shoot();
                    synchronized (gameState.getProjectiles()) {
                        gameState.addProjectile(shot);
                    }
                    shooter.resetShotCooldown();
                }
            }

            // Movimiento de alienígenas
            for (Alien alien : aliens) {
                alien.move(actualMoveAmount, 0);
            }
        }
    }


    private boolean checkAlienBoundary() {
        synchronized (aliens) {
            if (aliens.isEmpty()) return false;

            // Calcular formación completa
            float minFormationX = Float.MAX_VALUE;
            float maxFormationX = Float.MIN_VALUE;
            float alienWidth = 30;

            for (Alien alien : aliens) {
                if (alien.getX() < minFormationX) minFormationX = alien.getX();
                if (alien.getX() > maxFormationX) maxFormationX = alien.getX();
            }

            // Considerar el ancho total de la formación
            float formationWidth = (maxFormationX - minFormationX) + alienWidth;
            float rightBoundary = 800 - formationWidth; // Ancho de pantalla 800

            boolean hitRight = (maxFormationX + alienWidth >= 800);
            boolean hitLeft = (minFormationX <= 0);

            if (hitRight || hitLeft) {
                System.out.println("[SERVER] Aliens hit boundary. Min X: " + minFormationX + ", Max X: " + maxFormationX);
                return true;
            }
            return false;
        }
    }

    private void moveAliensDown() {
        synchronized (aliens) {
            if (aliens.isEmpty()) {
                return;
            }

            float moveDownAmount = 20.0f;
            float bottomBoundary = 550.0f; // Límite inferior
            boolean reachedBottom = false;

            for (Alien alien : aliens) {
                alien.move(0, moveDownAmount);

                // Verificar si algún alien ha alcanzado el fondo
                if (alien.getY() + 30 >= bottomBoundary) { // Añadir la altura del alien
                    reachedBottom = true;
                }
            }

            System.out.println("[SERVER] Aliens moving down. Sample Y: " + aliens.get(0).getY());

            if (reachedBottom) {
                System.out.println("[SERVER] Aliens reached bottom! Game Over");
                broadcastMessage("GAME_OVER:ALIENS_REACHED_BOTTOM");
                resetGame();
            }
        }
    }

    private void updateUFOs(long delta) {
        synchronized (ufos) {
            Iterator<UFO> ufoIterator = ufos.iterator();
            while (ufoIterator.hasNext()) {
                UFO ufo = ufoIterator.next();
                ufo.move(delta);
                
                if (ufo.getX() < -50 || ufo.getX() > 850) {
                    ufoIterator.remove();
                }
            }
        }
    }

    private void updateProjectiles(long delta) {
        synchronized (gameState.getProjectiles()) {
            Iterator<Projectile> it = gameState.getProjectiles().iterator();
            while (it.hasNext()) {
                Projectile p = it.next();
                p.update(delta); // Actualizar posición

                // Eliminar si sale de la pantalla
                if (p.getY() < 0 || p.getY() > 600) {
                    it.remove();

                    // Notificar al cliente para removerlo
                    broadcastMessage("PROJECTILE_REMOVED:" + System.identityHashCode(p));

                    // Habilitar disparo del jugador
                    if (!p.isFromAlien()) {
                        Player shooter = gameState.getPlayer(p.getPlayerId());
                        if (shooter != null) shooter.resetShoot();
                    }
                }
            }
        }
    }

    private void checkCollisions() {
        List<Projectile> projectiles = new ArrayList<>(gameState.getProjectiles());

        for (Projectile projectile : projectiles) {
            if (projectile.isFromAlien()) {
                checkAlienProjectileCollisions(projectile);
            } else {
                checkPlayerProjectileCollisions(projectile);
            }
        }
    }
    
    private void checkAlienProjectileCollisions(Projectile projectile) {
        Map<Integer, Player> players = gameState.getPlayers();

        synchronized (players) {
            Iterator<Player> playerIterator = players.values().iterator();
            while (playerIterator.hasNext()) {
                Player player = playerIterator.next();
                if (player.isColliding(projectile)) {
                    handlePlayerHit(player, projectile);

                    // Eliminar proyectil de forma segura
                    synchronized (gameState.getProjectiles()) {
                        gameState.getProjectiles().remove(projectile);
                    }
                    broadcastMessage("PROJECTILE_REMOVED:" + projectile.hashCode());

                    break;
                }
            }
        }
    }

    private void checkPlayerProjectileCollisions(Projectile projectile) {
        boolean collisionHandled = checkUfoCollisions(projectile);

        if (!collisionHandled) {
            collisionHandled = checkAlienCollisions(projectile);
        }

        if (collisionHandled) {
            synchronized (gameState.getProjectiles()) {
                gameState.getProjectiles().remove(projectile);
            }
            broadcastMessage("PROJECTILE_REMOVED:" + projectile.hashCode());
        }
    }

    private boolean checkUfoCollisions(Projectile projectile) {
        boolean collisionDetected = false;
        synchronized (ufos) {
            Iterator<UFO> ufoIterator = ufos.iterator();
            while (ufoIterator.hasNext()) {
                UFO ufo = ufoIterator.next();
                if (ufo.isColliding(projectile)) {
                    handleUfoDestruction(ufo, projectile);
                    ufoIterator.remove();

                    if(!projectile.isFromAlien()) {
                        Player shooter = gameState.getPlayer(projectile.getPlayerId());
                        if(shooter != null) {
                            shooter.resetShoot();
                        }
                    }

                    collisionDetected = true;
                    break;
                }
            }
        }
        return collisionDetected;
    }

    private boolean checkAlienCollisions(Projectile projectile) {
        boolean collisionDetected = false;
        synchronized (aliens) {
            Iterator<Alien> alienIterator = aliens.iterator();
            while (alienIterator.hasNext()) {
                Alien alien = alienIterator.next();
                if (alien.isColliding(projectile)) {
                    handleAlienDestruction(alien, projectile);
                    alienIterator.remove();

                    // Habilitar disparo del jugador si es su proyectil
                    if(!projectile.isFromAlien()) {
                        Player shooter = gameState.getPlayer(projectile.getPlayerId());
                        if(shooter != null) {
                            shooter.resetShoot();
                        }
                    }

                    collisionDetected = true;
                    break;
                }
            }
        }
        return collisionDetected;
    }

    private void handlePlayerHit(Player player, Projectile projectile) {
        player.hit();
        broadcastMessage("PLAYER_HIT:" + player.getId() + ":" + player.getLives());

        if (player.getLives() <= 0) {
            broadcastMessage("GAME_OVER:PLAYER_DIED:" + player.getId());
            System.out.println("[SERVER] Player " + player.getId() + " eliminado. Razón: Sin vidas");

            synchronized (clients) {
                synchronized (gameState.getPlayers()) {
                    gameState.removePlayer(player.getId());
                    clients.remove(player.getId());
                }
            }

            if (clients.isEmpty()) {
                System.out.println("[SERVER] Todos los jugadores desconectados. Reiniciando juego...");
                resetGame();
            } else {
                System.out.println("[SERVER] Jugadores restantes: " + clients.size());
                synchronized (gameState.getPlayers()) {
                    gameState.getPlayers().values().forEach(p -> {
                        p.resetPosition();
                        p.resetLives();
                    });
                }
                broadcastMessage("GAME_RESET");
            }
        }
    }

    private void handleUfoDestruction(UFO ufo, Projectile projectile) {
        Player shooter = gameState.getPlayer(projectile.getPlayerId());
        if (shooter != null) {
            shooter.addScore(ufo.getPoints());
            broadcastMessage("SCORE_UPDATE:" + shooter.getId() + ":" + shooter.getScore());
        }
        broadcastMessage("UFO_DESTROYED:" + ufo.getX() + ":" + ufo.getY());
    }

    private void handleAlienDestruction(Alien alien, Projectile projectile) {
        Player shooter = gameState.getPlayer(projectile.getPlayerId());
        if (shooter != null) {
            shooter.addScore(alien.getPoints());
            broadcastMessage("SCORE_UPDATE:" + shooter.getId() + ":" + shooter.getScore());
        }
        broadcastMessage("ALIEN_DESTROYED:" + alien.getX() + ":" + alien.getY() + ":" + alien.getType());
    }

    private void resetGame() {
        if(!clients.isEmpty()) return;
        synchronized (gameState) {
            level = 1;
            alienDirectionX = 1;

            createAliens();

            synchronized (gameState.getPlayers()) {
                gameState.getPlayers().values().forEach(player -> {
                    player.resetPosition();
                    player.resetLives();
                    player.resetScore();
                });
            }

            synchronized (gameState.getProjectiles()) {
                gameState.getProjectiles().clear();
            }

            synchronized (ufos) {
                ufos.clear();
            }
        }

        broadcastMessage("GAME_RESET");
        System.out.println("Game has been fully reset");
    }

    public void broadcastGameState() {
        StringBuilder sb = new StringBuilder();
        sb.append("GAME_STATE:");
        
        synchronized (shields) {
            sb.append("SHIELDS_START:");
            for (Shield shield : shields) {
                sb.append(shield.getX()).append(",")
                  .append(shield.getY()).append("|");
                boolean[][] segments = shield.getSegments();
                for (int i = 0; i < 5; i++) {
                    for (int j = 0; j < 3; j++) {
                        sb.append(segments[i][j] ? "1" : "0");
                    }
                }
                sb.append(";");
            }
            sb.append("SHIELDS_END:");
        }
        
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
        
        synchronized (aliens) {
            sb.append(":ALIENS:");
            for (Alien alien : aliens) {
                sb.append(alien.getX()).append(",")
                  .append(alien.getY()).append(",")
                  .append(alien.getType()).append(";"); // Formato: X,Y,Tipo;
            }
        }
        
        synchronized (ufos) {
            sb.append(":UFOS:");
            for (UFO ufo : ufos) {
                sb.append(ufo.getX()).append(",")
                  .append(ufo.getY()).append(",")
                  .append(ufo.getDirection()).append(";");
            }
        }
        
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
        StringBuilder sb = new StringBuilder();
        sb.append("INIT_GAME_STATE:");
        sb.append("LEVEL:").append(level).append(":");
        
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
        
        synchronized (aliens) {
            sb.append(":ALIENS:");
            for (Alien alien : aliens) {
                sb.append(alien.getX()).append(",")
                  .append(alien.getY()).append(",")
                  .append(alien.getType()).append(";");
            }
        }
        
        synchronized (shields) {
            sb.append(":SHIELDS:");
            for (Shield shield : shields) {
                sb.append(shield.getX()).append(",")
                  .append(shield.getY()).append(";");
                boolean[][] segments = shield.getSegments();
                for (int i = 0; i < 5; i++) {
                    for (int j = 0; j < 3; j++) {
                        sb.append(segments[i][j] ? "1" : "0");
                    }
                }
                sb.append(";");
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
        synchronized (clients) {
            clients.remove(clientId);
            gameState.removePlayer(clientId);
            broadcastMessage("PLAYER_DISCONNECTED:" + clientId);

            if(clients.isEmpty()) {
                resetGame();
                System.out.println("All players disconnected. Resetting game state");
            }
        }
    }

    public void handlePlayerAction(int clientId, String action) {
        Player player = gameState.getPlayer(clientId);
        if (player == null) return;
        
        switch (action) {
            case "MOVE_LEFT":
                player.move(-10, 0);
                System.out.println("[SERVER] Player " + clientId + " moved LEFT. New position: " + player.getX() + ", " + player.getY());
                break;
            case "MOVE_RIGHT":
                player.move(10, 0);
                System.out.println("[SERVER] Player " + clientId + " moved RIGHT. New position: " + player.getX() + ", " + player.getY());
                break;
            case "SHOOT":
                Projectile projectile = player.shoot();
                if(projectile != null) {
                    gameState.addProjectile(projectile);
                    broadcastMessage("PROJECTILE_ADDED:" + 
                        projectile.getX() + "," + 
                        projectile.getY() + "," + 
                        projectile.isFromAlien() + "," + 
                        projectile.getPlayerId());
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
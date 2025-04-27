package PC2.Space_Invaders02;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameState {
    private final Map<Integer, Player> players = new ConcurrentHashMap<>();
    private final List<Projectile> projectiles = new ArrayList<>();

    public Map<Integer, Player> getPlayers() {
        return players;
    }

    public Player getPlayer(int id) {
        return players.get(id);
    }

    public void addPlayer(int id, Player player) {
        players.put(id, player);
    }

    public void removePlayer(int id) {
        players.remove(id);
    }

    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    public void addProjectile(Projectile projectile) {
        synchronized (projectiles) {
            projectiles.add(projectile);
        }
    }
}
package PC2.Space_Invaders02;

public class Projectile {
    private float x;
    private float y;
    private final boolean fromAlien;
    private final int playerId;
    private static final float PLAYER_PROJECTILE_SPEED = -0.3f; // Moving up
    private static final float ALIEN_PROJECTILE_SPEED = 0.2f; // Moving down

    public Projectile(float x, float y, boolean fromAlien, int playerId) {
        this.x = x;
        this.y = y;
        this.fromAlien = fromAlien;
        this.playerId = playerId;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public boolean isFromAlien() {
        return fromAlien;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void update(long delta) {
        if (fromAlien) {
            y += ALIEN_PROJECTILE_SPEED * delta;
        } else {
            y += PLAYER_PROJECTILE_SPEED * delta;
        }
    }
}

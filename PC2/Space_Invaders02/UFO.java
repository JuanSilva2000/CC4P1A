package PC2.Space_Invaders02;

public class UFO {
    private float x;
    private float y;
    private final int direction; // 1 = right, -1 = left
    private static final float SPEED = 0.2f;

    public UFO(float x, float y, int direction) {
        this.x = x;
        this.y = y;
        this.direction = direction;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getDirection() {
        return direction;
    }

    public void move(long delta) {
        x += direction * SPEED * delta;
    }

    public int getPoints() {
        return 100 + (int)(Math.random() * 200); // Random between 100-300 points
    }

    public boolean isColliding(Projectile projectile) {
        // Simple collision detection
        return projectile.getX() >= x && 
               projectile.getX() <= x + 40 && 
               projectile.getY() >= y && 
               projectile.getY() <= y + 20;
    }
}

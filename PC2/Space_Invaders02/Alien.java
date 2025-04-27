package PC2.Space_Invaders02;

public class Alien {
    private float x;
    private float y;
    private final int type; // 1=large (10 pts), 2=medium (20 pts), 3=small (30 pts)

    public Alien(float x, float y, int type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getType() {
        return type;
    }

    public void move(float dx, float dy) {
        x += dx;
        y += dy;
    }

    public int getPoints() {
        switch (type) {
            case 1: return 10; // Large alien
            case 2: return 20; // Medium alien
            case 3: return 30; // Small alien
            default: return 10;
        }
    }

    public Projectile shoot() {
        return new Projectile(x + 15, y + 30, true, -1);
    }

    public boolean isColliding(Projectile projectile) {
        // Simple collision detection
        return projectile.getX() >= x && 
               projectile.getX() <= x + 30 && 
               projectile.getY() >= y && 
               projectile.getY() <= y + 30;
    }
}

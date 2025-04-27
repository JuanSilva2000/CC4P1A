package PC2.Space_Invaders02;

public class Player {
    private final int id;
    private float x;
    private float y;
    private int score;
    private int lives;
    private long lastShotTime;
    private static final long SHOT_COOLDOWN = 500; // 500ms cooldown between shots

    public Player(int id, float x, float y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.score = 0;
        this.lives = 3;
        this.lastShotTime = 0;
    }

    public int getId() {
        return id;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getLives() {
        return lives;
    }

    public void setLives(int lives) {
        this.lives = lives;
    }

    public void move(float dx, float dy) {
        x += dx;
        // Keep player within bounds
        if (x < 0) {
            x = 0;
        } else if (x > 770) { // 800 - 30 (player width)
            x = 770;
        }
        
        y += dy;
    }

    public void addScore(int points) {
        score += points;
    }

    public void hit() {
        lives--;
    }

    public void resetPosition() {
        x = 400;
        y = 550;
    }

    public Projectile shoot() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShotTime >= SHOT_COOLDOWN) {
            lastShotTime = currentTime;
            return new Projectile(x + 15, y, false, id);
        }
        return null;
    }

    public boolean isColliding(Projectile projectile) {
        // Simple collision detection
        return projectile.getX() >= x && 
               projectile.getX() <= x + 30 && 
               projectile.getY() >= y && 
               projectile.getY() <= y + 20;
    }
}
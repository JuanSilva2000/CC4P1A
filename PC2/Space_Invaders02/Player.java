package PC2.Space_Invaders02;

public class Player {
    private int id;
    private float x;
    private float y;
    private int score;
    private int lives;
    private boolean canShoot;
    private static final int WIDTH = 30;
    private static final int HEIGHT = 20;
    
    public Player(int id, float x, float y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.score = 0;
        this.lives = 3;
        this.canShoot = true;
    }
    
    public int getId() {
        return id;
    }
    
    public float getX() {
        return x;
    }
    
    public float getY() {
        return y;
    }
    
    public void setX(float x) {
        this.x = x;
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
        setX(x + dx);
    }
    
    public Projectile shoot() {
        if (canShoot) {
            canShoot = false;
            return new Projectile(x + WIDTH / 2, y - 10, false, id);
        }
        return null;
    }
    
    public void resetShoot() {
        canShoot = true;
    }
    
    public void hit() {
        lives--;
    }
    
    public void addScore(int points) {
        score += points;
    }
    
    public boolean isColliding(Projectile projectile) {
        if (!projectile.isFromAlien() || projectile.getPlayerId() == id) {
            return false; // Los jugadores no colisionan con sus propios proyectiles
        }
        
        // Colisión basada en rectángulos
        return projectile.getX() >= x && 
               projectile.getX() <= x + WIDTH &&
               projectile.getY() >= y && 
               projectile.getY() <= y + HEIGHT;
    }
    
    public void resetPosition() {
        x = 400; // Posición central
        y = 550; // Parte inferior de la pantalla
    }
    
    public void resetLives() {
        lives = 3;
    }
    
    public void resetScore() {
        score = 0;
    }
}
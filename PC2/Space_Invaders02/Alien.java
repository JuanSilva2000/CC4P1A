package PC2.Space_Invaders02;

public class Alien {
    private float x;
    private float y;
    private int type; // 1=grande (10pts), 2=mediano (20pts), 3=pequeño (30pts)
    private long lastShotTime;
    private static final long SHOT_COOLDOWN = 3000; // 3 segundos entre disparos
    
    public Alien(float x, float y, int type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.lastShotTime = System.currentTimeMillis() - (long)(Math.random() * SHOT_COOLDOWN);
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
    
    public boolean canShoot() {
        return System.currentTimeMillis() - lastShotTime > SHOT_COOLDOWN;
    }
    
    public void resetShotCooldown() {
        lastShotTime = System.currentTimeMillis();
    }
    
    public Projectile shoot() {
        if (canShoot()) {
            resetShotCooldown();
            return new Projectile(x + 15, y + 30, true, -1);
        }
        return null;
    }
    
    public boolean isColliding(Projectile projectile) {
        if (projectile.isFromAlien()) {
            return false; // Los alienígenas no colisionan con sus propios proyectiles
        }
        
        // Colisión simple basada en rectángulos (30x30 para alienígenas)
        return projectile.getX() >= x && 
               projectile.getX() <= x + 30 &&
               projectile.getY() >= y && 
               projectile.getY() <= y + 30;
    }
    
    public int getPoints() {
        switch (type) {
            case 1: return 10; // Grandes
            case 2: return 20; // Medianos
            case 3: return 30; // Pequeños
            default: return 10;
        }
    }
}

package PC2.Space_Invaders02;

public class Projectile {
    private float x;
    private float y;
    private final boolean fromAlien;
    private final int playerId;
    private boolean active = true;
    private static final float PLAYER_PROJECTILE_SPEED = -0.15f;
    private static final float ALIEN_PROJECTILE_SPEED = 0.10f;
    
    public Projectile(float x, float y, boolean fromAlien, int playerId){
        this.x = x;
        this.y = y;
        this.fromAlien = fromAlien;
        this.playerId = playerId;
    }
    
    public float getX(){
        return x;
    }
    
    public float getY(){
        return y;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    
    public boolean isFromAlien(){
        return fromAlien;
    }
    
    public int getPlayerId(){
        return playerId;
    }
    
    public boolean isActive(){
        return active;
    }
    
    public void deactivate(){
        active = false;
    }
    
    public void update(long delta){
        if(active){
            if(fromAlien){
                y += ALIEN_PROJECTILE_SPEED * delta;
            }
            else{
                y += PLAYER_PROJECTILE_SPEED * delta;
            }
        }
    }
}

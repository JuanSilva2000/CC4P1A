package PC2.Space_Invaders02;

public class Shield {
    private float x;
    private float y;
    private boolean[][] segments;
    private static final int SEGMENT_WIDTH = 20;
    private static final int SEGMENT_HEIGHT = 10;
    
    public Shield(float x, float y) {
        this.x = x;
        this.y = y;
        // Crear un escudo de 5x3 segmentos
        this.segments = new boolean[5][3];
        // Inicializar todos los segmentos como activos
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 3; j++) {
                segments[i][j] = true;
            }
        }
    }
    
    public float getX() {
        return x;
    }
    
    public float getY() {
        return y;
    }
    
    public boolean[][] getSegments() {
        return segments;
    }
    
    public void setSegments(boolean[][] segments) {
        this.segments = segments;
    }
    
    public boolean checkCollision(Projectile projectile) {
        if (projectile == null) return false;
        
        // Convertir coordenadas del proyectil a índices de segmento
        int segX = (int)((projectile.getX() - x) / SEGMENT_WIDTH);
        int segY = (int)((projectile.getY() - y) / SEGMENT_HEIGHT);
        
        // Verificar si las coordenadas están dentro de los límites del escudo
        if (segX >= 0 && segX < 5 && segY >= 0 && segY < 3) {
            // Verificar si el segmento está activo
            if (segments[segX][segY]) {
                // Desactivar el segmento al recibir un impacto
                segments[segX][segY] = false;
                return true;
            }
        }
        
        return false;
    }
    
    // Método para verificar si el escudo está completamente destruido
    public boolean isDestroyed() {
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 3; j++) {
                if (segments[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }
}

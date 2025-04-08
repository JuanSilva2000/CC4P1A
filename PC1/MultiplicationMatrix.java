import java.util.Random;

public class MultiplicationMatrix {
    static final int L = 4; // Tamaño de la matriz L x L
    static final int h = 5; // Número de hilos

    static int[][] A = new int[L][L];
    static int[][] B = new int[L][L];
    static int[][] C = new int[L][L];

    public static void main(String[] args) {
        generarMatrices();
        System.out.println("Matriz A:");
        imprimirMatriz(A);

        System.out.println("\nMatriz B:");
        imprimirMatriz(B);

        // Multiplicación en paralelo
        Thread[] hilos = new Thread[h];
        int filasPorHilo = L / h;

        for (int i = 0; i < h; i++) {
            int inicio = i * filasPorHilo;
            int fin = (i == h - 1) ? L : inicio + filasPorHilo; // el último hilo puede tomar más si L no es divisible por h
            hilos[i] = new Thread(new Multiplicador(inicio, fin));
            hilos[i].start();
        }

        // Esperar que todos los hilos terminen
        for (int i = 0; i < h; i++) {
            try {
                hilos[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Imprimir la matriz resultado
        System.out.println("\nMatriz resultado C = A x B:");
        imprimirMatriz(C);
    }

    static void generarMatrices() {
        Random rand = new Random();
        for (int i = 0; i < L; i++) {
            for (int j = 0; j < L; j++) {
                A[i][j] = rand.nextInt(10); // valores entre 0 y 9
                B[i][j] = rand.nextInt(10);
            }
        }
    }

    static void imprimirMatriz(int[][] M) {
        for (int[] fila : M) {
            for (int val : fila) {
                System.out.printf("%4d", val);
            }
            System.out.println();
        }
    }

    static class Multiplicador implements Runnable {
        int inicioFila, finFila;

        Multiplicador(int inicio, int fin) {
            this.inicioFila = inicio;
            this.finFila = fin;
        }

        @Override
        public void run() {
            // Algoritmo clásico en paralelo
            for (int i = inicioFila; i < finFila; i++) {
                for (int j = 0; j < L; j++) {
                    int suma = 0;
                    for (int k = 0; k < L; k++) {
                        suma += A[i][k] * B[k][j];
                    }
                    C[i][j] = suma;
                }
            }
        }
    }
}
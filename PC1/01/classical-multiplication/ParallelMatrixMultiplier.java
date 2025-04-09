import java.util.Random;

public class ParallelMatrixMultiplier {
    static final int L = 500;
    static final int h = 10;

    static int[][] A = new int[L][L];
    static int[][] B = new int[L][L];
    static int[][] C = new int[L][L];

    public static void main(String[] args) {
        generarMatrices();
        // System.out.println("Matriz A:");
        // imprimirMatriz(A);

        // System.out.println("\nMatriz B:");
        // imprimirMatriz(B);

        long inicioTime = System.nanoTime();

        Thread[] hilos = new Thread[h];
        int filasPorHilo = L / h;

        for (int i = 0; i < h; i++) {
            int inicio = i * filasPorHilo;
            int fin = (i == h - 1) ? L : inicio + filasPorHilo;
            hilos[i] = new Thread(new Multiplicador(inicio, fin));
            hilos[i].start();
        }

        for (int i = 0; i < h; i++) {
            try {
                hilos[i].join();
            } catch (Exception e) {
                System.out.println("error: " + e.getMessage());
            }
        }

        long finTime = System.nanoTime();
        long tiempoTotal = finTime - inicioTime;

        // Imprimir la matriz resultado
        System.out.println("\nMatriz resultado C = A x B:");
        // imprimirMatriz(C);

        System.out.println("\nTiempo total: " + tiempoTotal + " ns");
    }

    static void generarMatrices() {
        Random rand = new Random();
        for (int i = 0; i < L; i++) {
            for (int j = 0; j < L; j++) {
                A[i][j] = rand.nextInt(10);
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
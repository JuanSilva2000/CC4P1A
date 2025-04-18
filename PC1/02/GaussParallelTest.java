import java.util.Random;

public class GaussParallelTest {
    static final int L = 100; // Tamaño del sistema
    static final int h = 8;   // Número de hilos
    static double[][] A = new double[L][L];
    static double[] b = new double[L];
    static double[] x_real = new double[L];

    public static void main(String[] args) throws InterruptedException {
        generarSistemaConSolucion();

        double[][] A_copy = copiarMatriz(A);
        double[] b_copy = b.clone();

        long inicioSecuencial = System.nanoTime();
        double[] resultadoSecuencial = resolverSecuencial(A_copy, b_copy);
        long finSecuencial = System.nanoTime();
        double tiempoSecuencial = (finSecuencial - inicioSecuencial) / 1e6;

        A_copy = copiarMatriz(A);
        b_copy = b.clone();

        long inicioParalelo = System.nanoTime();
        double[] resultadoParalelo = resolverParalelo(A_copy, b_copy, h);
        long finParalelo = System.nanoTime();
        double tiempoParalelo = (finParalelo - inicioParalelo) / 1e6;

        System.out.printf("⏱ Tiempo secuencial: %.4f ms%n", tiempoSecuencial);
        System.out.printf("⏱ Tiempo en paralelo: %.4f ms%n%n", tiempoParalelo);

        System.out.println("✅ Comparación con solución real:");
        for (int i = 0; i < L; i++) {
            System.out.printf("x%d = %.4f\t(real: %.4f)%n", i, resultadoParalelo[i], x_real[i]);
        }

        // Mostrar sistema
        System.out.println("\n🧾 Sistema generado:");
        for (int i = 0; i < Math.min(L, 10); i++) { // Imprimir primeras 10 filas para no saturar
            for (int j = 0; j < L; j++) {
                System.out.printf("%.2f ", A[i][j]);
            }
            System.out.printf("| %.2f%n", b[i]);
        }
    }

    public static void generarSistemaConSolucion() {
        Random rand = new Random();
        for (int i = 0; i < L; i++) {
            x_real[i] = rand.nextDouble() * 20 - 10; // Solución real entre -10 y 10
            for (int j = 0; j < L; j++) {
                A[i][j] = rand.nextDouble() * 10; // Coeficientes entre 0 y 10
            }
        }
        for (int i = 0; i < L; i++) {
            b[i] = 0;
            for (int j = 0; j < L; j++) {
                b[i] += A[i][j] * x_real[j];
            }
        }
    }

    public static double[] resolverSecuencial(double[][] A, double[] b) {
        int n = b.length;
        for (int k = 0; k < n; k++) {
            for (int i = k + 1; i < n; i++) {
                double factor = A[i][k] / A[k][k];
                for (int j = k; j < n; j++) {
                    A[i][j] -= factor * A[k][j];
                }
                b[i] -= factor * b[k];
            }
        }

        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            x[i] = b[i];
            for (int j = i + 1; j < n; j++) {
                x[i] -= A[i][j] * x[j];
            }
            x[i] /= A[i][i];
        }
        return x;
    }

    public static double[] resolverParalelo(double[][] A, double[] b, int hilos) throws InterruptedException {
        int n = b.length;
        for (int k = 0; k < n; k++) {
            int k_final = k;
            Thread[] threads = new Thread[hilos];

            for (int t = 0; t < hilos; t++) {
                final int hilo = t;
                threads[t] = new Thread(() -> {
                    for (int i = k_final + 1 + hilo; i < n; i += hilos) {
                        double factor = A[i][k_final] / A[k_final][k_final];
                        for (int j = k_final; j < n; j++) {
                            A[i][j] -= factor * A[k_final][j];
                        }
                        b[i] -= factor * b[k_final];
                    }
                });
                threads[t].start();
            }

            for (Thread thread : threads) thread.join();
        }

        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            x[i] = b[i];
            for (int j = i + 1; j < n; j++) {
                x[i] -= A[i][j] * x[j];
            }
            x[i] /= A[i][i];
        }
        return x;
    }

    public static double[][] copiarMatriz(double[][] original) {
        double[][] copia = new double[original.length][];
        for (int i = 0; i < original.length; i++) {
            copia[i] = original[i].clone();
        }
        return copia;
    }
}

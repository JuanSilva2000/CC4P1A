import java.util.Random;

public class StrassenParallel {
    static final int N = 1024;
    static final int MAX_DEPTH = 1;

    public static void main(String[] args) {
        int[][] A = generarMatrizAleatoria(N);
        int[][] B = generarMatrizAleatoria(N);

        System.out.println("Matriz A:");
        // imprimirMatriz(A);
        System.out.println("\nMatriz B:");
        // imprimirMatriz(B);

        long inicio = System.nanoTime();
        int[][] C = strassen(A, B, 0);
        long fin = System.nanoTime();

        System.out.println("\nMatriz resultado C:");
        // imprimirMatriz(C);
        System.out.println("\nTiempo total: " + (fin - inicio) + " ns");
    }

    static int[][] strassen(int[][] A, int[][] B, int depth) {
        int n = A.length;
        if (n == 1) {
            return new int[][] { { A[0][0] * B[0][0] } };
        }


        int[][][] As = dividir(A);
        int[][][] Bs = dividir(B);

        int[][] A11 = As[0], A12 = As[1], A21 = As[2], A22 = As[3];
        int[][] B11 = Bs[0], B12 = Bs[1], B21 = Bs[2], B22 = Bs[3];

        int[][] M1, M2, M3, M4, M5, M6, M7;

        if (depth < MAX_DEPTH) {
            StrassenTask t1 = new StrassenTask(sumar(A11, A22), sumar(B11, B22), depth + 1);
            StrassenTask t2 = new StrassenTask(sumar(A21, A22), B11, depth + 1);
            StrassenTask t3 = new StrassenTask(A11, restar(B12, B22), depth + 1);
            StrassenTask t4 = new StrassenTask(A22, restar(B21, B11), depth + 1);
            StrassenTask t5 = new StrassenTask(sumar(A11, A12), B22, depth + 1);
            StrassenTask t6 = new StrassenTask(restar(A21, A11), sumar(B11, B12), depth + 1);
            StrassenTask t7 = new StrassenTask(restar(A12, A22), sumar(B21, B22), depth + 1);

            t1.start(); t2.start(); t3.start(); t4.start();
            t5.start(); t6.start(); t7.start();

            try {
                t1.join(); t2.join(); t3.join(); t4.join();
                t5.join(); t6.join(); t7.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            M1 = t1.result;
            M2 = t2.result;
            M3 = t3.result;
            M4 = t4.result;
            M5 = t5.result;
            M6 = t6.result;
            M7 = t7.result;
        } else {
            M1 = strassen(sumar(A11, A22), sumar(B11, B22), depth + 1);
            M2 = strassen(sumar(A21, A22), B11, depth + 1);
            M3 = strassen(A11, restar(B12, B22), depth + 1);
            M4 = strassen(A22, restar(B21, B11), depth + 1);
            M5 = strassen(sumar(A11, A12), B22, depth + 1);
            M6 = strassen(restar(A21, A11), sumar(B11, B12), depth + 1);
            M7 = strassen(restar(A12, A22), sumar(B21, B22), depth + 1);
        }

        int[][] C11 = sumar(restar(sumar(M1, M4), M5), M7);
        int[][] C12 = sumar(M3, M5);
        int[][] C21 = sumar(M2, M4);
        int[][] C22 = sumar(restar(sumar(M1, M3), M2), M6);

        return combinar(C11, C12, C21, C22);
    }

    static class StrassenTask extends Thread {
        int[][] A, B, result;
        int depth;

        StrassenTask(int[][] A, int[][] B, int depth) {
            this.A = A;
            this.B = B;
            this.depth = depth;
        }

        public void run() {
            result = strassen(A, B, depth);
        }
    }

    static int[][][] dividir(int[][] M) {
        int n = M.length / 2;
        int[][] A11 = new int[n][n], A12 = new int[n][n], A21 = new int[n][n], A22 = new int[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                A11[i][j] = M[i][j];
                A12[i][j] = M[i][j + n];
                A21[i][j] = M[i + n][j];
                A22[i][j] = M[i + n][j + n];
            }
        return new int[][][] { A11, A12, A21, A22 };
    }

    static int[][] combinar(int[][] C11, int[][] C12, int[][] C21, int[][] C22) {
        int n = C11.length;
        int[][] C = new int[n * 2][n * 2];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                C[i][j] = C11[i][j];
                C[i][j + n] = C12[i][j];
                C[i + n][j] = C21[i][j];
                C[i + n][j + n] = C22[i][j];
            }
        return C;
    }

    static int[][] sumar(int[][] A, int[][] B) {
        int n = A.length;
        int[][] C = new int[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                C[i][j] = A[i][j] + B[i][j];
        return C;
    }

    static int[][] restar(int[][] A, int[][] B) {
        int n = A.length;
        int[][] C = new int[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                C[i][j] = A[i][j] - B[i][j];
        return C;
    }

    static int[][] generarMatrizAleatoria(int n) {
        int[][] M = new int[n][n];
        Random rand = new Random();
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                M[i][j] = rand.nextInt(10);
        return M;
    }

    static void imprimirMatriz(int[][] M) {
        for (int[] fila : M) {
            for (int val : fila)
                System.out.printf("%4d", val);
            System.out.println();
        }
    }
}
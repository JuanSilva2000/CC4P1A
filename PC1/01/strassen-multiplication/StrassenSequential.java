import java.util.Random;

public class StrassenSequential {
  static final int N = 512; // debe ser potencia de 2
  static int[][] A = new int[N][N];
  static int[][] B = new int[N][N];

  public static void main(String[] args) {
      generarMatrices();
      System.out.println("Matriz A:");
    //   imprimirMatriz(A);
      System.out.println("\nMatriz B:");
    //   imprimirMatriz(B);

      long inicio = System.nanoTime();
      int[][] C = strassen(A, B);
      long fin = System.nanoTime();

      System.out.println("\nMatriz resultado C:");
    //   imprimirMatriz(C);
      System.out.println("\nTiempo total: " + (fin - inicio) + " ns");
  }

  static void generarMatrices() {
      Random rand = new Random();
      for (int i = 0; i < N; i++)
          for (int j = 0; j < N; j++) {
              A[i][j] = rand.nextInt(10);
              B[i][j] = rand.nextInt(10);
          }
  }

  static int[][] strassen(int[][] A, int[][] B) {
      int n = A.length;

      if (n == 1) {
          return new int[][] { { A[0][0] * B[0][0] } };
      }

      int[][][] As = dividir(A);
      int[][][] Bs = dividir(B);

      int[][] A11 = As[0], A12 = As[1], A21 = As[2], A22 = As[3];
      int[][] B11 = Bs[0], B12 = Bs[1], B21 = Bs[2], B22 = Bs[3];

      int[][] M1 = strassen(sumar(A11, A22), sumar(B11, B22));
      int[][] M2 = strassen(sumar(A21, A22), B11);
      int[][] M3 = strassen(A11, restar(B12, B22));
      int[][] M4 = strassen(A22, restar(B21, B11));
      int[][] M5 = strassen(sumar(A11, A12), B22);
      int[][] M6 = strassen(restar(A21, A11), sumar(B11, B12));
      int[][] M7 = strassen(restar(A12, A22), sumar(B21, B22));

      int[][] C11 = sumar(restar(sumar(M1, M4), M5), M7);
      int[][] C12 = sumar(M3, M5);
      int[][] C21 = sumar(M2, M4);
      int[][] C22 = sumar(restar(sumar(M1, M3), M2), M6);

      return combinar(C11, C12, C21, C22);
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

  static void imprimirMatriz(int[][] M) {
      for (int[] fila : M) {
          for (int val : fila)
              System.out.printf("%4d", val);
          System.out.println();
      }
  }
}
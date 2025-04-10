public class EquationsSystem {
  static final int L = 3; // numero de incognitas
    static double[][] A = { // esta es la matriz aumentada, no la matriz de coeficientes
        {2, 1, -1, -3},
        {-3, -1, 2, -11},
        {-2, 1, 2, -3}
    };

    public static void main(String[] args) {
        gaussianElimination();
        backSubstitution();
    }

    public static void gaussianElimination() {
        for (int k = 0; k < L; k++) {
            for (int i = k + 1; i < L; i++) {
                double factor = A[i][k] / A[k][k];
                for (int j = k; j <= L; j++) {
                    A[i][j] -= factor * A[k][j];
                }
            }
        }
    }

    public static void backSubstitution() {
        double[] x = new double[L];

        for (int i = L - 1; i >= 0; i--) {
            x[i] = A[i][L];
            for (int j = i + 1; j < L; j++) {
                x[i] -= A[i][j] * x[j];
            }
            x[i] /= A[i][i];
        }

        System.out.println("Solución:");
        for (double xi : x) {
            System.out.printf("%.4f ", xi);
        }
    }
}


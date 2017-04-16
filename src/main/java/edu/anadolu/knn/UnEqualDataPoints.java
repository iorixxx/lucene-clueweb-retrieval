package edu.anadolu.knn;

/**
 * Chi-Square implementation for unequal numbers of data points.
 */
public class UnEqualDataPoints extends ChiBase {

    @Override
    String name() {
        return "UE";
    }

    public UnEqualDataPoints(boolean divide, boolean cdf) {
        super(divide, cdf);
    }

    static <T extends Number> double sum(T[] R) {
        double sum = 0.0;
        for (T t : R)
            sum += t.doubleValue();
        return sum;
    }

    /**
     * Compute chi-square statistics of two binned data sets
     *
     * @param R first data set
     * @param S second data set
     * @return chi-square statistics
     */
    @Override
    public <T extends Number> double chiPDF(T[] R, T[] S) {

        double chi = 0.0;

        int counter = 0;

        double sumR = sum(R);
        double sumS = sum(S);

        double S_R = Math.sqrt(sumS / sumR);
        double R_S = Math.sqrt(sumR / sumS);

        for (int i = 0; i < R.length; i++) {

            if (R[i].doubleValue() == 0 && S[i].doubleValue() == 0) continue;
            counter++;
            chi += Math.pow(S_R * R[i].doubleValue() - R_S * S[i].doubleValue(), 2.0) / (R[i].doubleValue() + S[i].doubleValue());
        }

        if (divide)
            return chi / (double) counter;
        else
            return chi;
    }

    @Override
    public <T extends Number> double chiCDF(T[] R, T[] S) {

        double chi = 0.0;

        double sumR = sum(R);
        double sumS = sum(S);

        double r = 0.0;
        double s = 0.0;

        double S_R = Math.sqrt(sumS / sumR);
        double R_S = Math.sqrt(sumR / sumS);

        for (int i = R.length - 1; i >= 0; i--) {

            r += R[i].doubleValue();
            s += S[i].doubleValue();

            if (r == 0 && s == 0) {
//                System.err.println("i = " + i + " both r and s are zero in chiCDF method of " + toString());
//                System.err.print(Arrays.toString(R));
//                System.err.print(Arrays.toString(S));

                continue;
            }


            chi += Math.pow(S_R * r - R_S * s, 2.0) / (r + s);
        }


        return chi;
    }
}

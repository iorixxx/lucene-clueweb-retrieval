package edu.anadolu.knn;

/**
 * First implementation of mine!
 */
public class ChiSquare extends ChiBase {

    @Override
    String name() {
        return "Ch";
    }

    public ChiSquare(boolean divide, boolean cdf) {
        super(divide, cdf);
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

        for (int i = 0; i < R.length; i++) {

            if (R[i].doubleValue() == 0 && S[i].doubleValue() == 0) continue;
            counter++;
            chi += Math.pow(R[i].doubleValue() - S[i].doubleValue(), 2.0) / (R[i].doubleValue() + S[i].doubleValue());
        }

        if (divide)
            return chi / (double) counter;
        else
            return chi;
    }


    @Override
    public <T extends Number> double chiCDF(T[] R, T[] S) {

        double chi = 0.0;

        double r = 0.0;
        double s = 0.0;

        for (int i = R.length - 1; i >= 0; i--) {

            r += R[i].doubleValue();
            s += S[i].doubleValue();

            if (r == 0 && s == 0) {
                //System.err.println("i = " + i + " both r and s are zero in chiCDF method of " + toString());
//                System.err.print(Arrays.toString(R));
//                System.err.print(Arrays.toString(S));
                continue;
            }


            chi += Math.pow(r - s, 2.0) / (r + s);
        }

        return chi;
    }

    public String toString() {
        return ",Ch"; //+ super.toString();
    }
}

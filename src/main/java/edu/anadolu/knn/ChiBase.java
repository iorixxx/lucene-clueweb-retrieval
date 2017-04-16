package edu.anadolu.knn;

/**
 * Base class for chi-square implementations
 */
public abstract class ChiBase implements Chi {

    abstract String name();

    /**
     * The lower bound, optional protected final int l;
     */


    /**
     * The upper bound, optional protected final int u;
     */


    /**
     * Flag to decide whether divide sum to counter or not
     */

    protected final boolean divide;
    protected final boolean cdf;

    /**
     * @param l      The lower bound, 0 or 1
     * @param u      The upper bound 50 < u < 1001
     * @param divide flag to decide whether divide sum to counter or not
     * @param cdf    flag to decide whether to use CDF or PDF
     */


    public ChiBase(boolean divide, boolean cdf) {

        // if (!(l == 0 || l == 1)) throw new RuntimeException("lower bound must be 0 or 1");

        //  if (u < 50 || u > 1001) throw new RuntimeException("upper bound must be less than 1001 or greater than 50");

        if (cdf && divide)
            throw new IllegalArgumentException("when CDF is used, divide true makes no sense");
        this.divide = divide;
        this.cdf = cdf;
    }

    public final <T extends Number> double chiSquared(T[] R, T[] S) {
        final double returnValue = cdf ? chiCDF(R, S) : chiPDF(R, S);
        return Double.isNaN(returnValue) ? Double.MAX_VALUE : returnValue;

//        if (cdf)
//            return chiCDF(R, S);
//        else
//            return chiPDF(R, S);
    }

    @Override
    public String toString() {
        return "," + (cdf ? "c" : "p") + "," + (divide ? "D" : "N");
    }

}

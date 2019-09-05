package edu.anadolu.spam;

/**
 * Binning Strategy for The log-odds-ratio version of the Fusion spam score set.
 */
public class OddsBinning {

    // The odds ratio [-10.42 .. 15.96]
    public static final double[] intervals = new double[]{
            -10.5
            , -10
            , -9.5
            , -9
            , -8.5
            , -8
            , -7.5
            , -7
            , -6.5
            , -6
            , -5.5
            , -5
            , -4.5
            , -4
            , -3.5
            , -3
            , -2.5
            , -2
            , -1.5
            , -1
            , -0.5
            , 0
            , 0.5
            , 1
            , 1.5
            , 2
            , 2.5
            , 3
            , 3.5
            , 4
            , 4.5
            , 5
            , 5.5
            , 6
            , 6.5
            , 7
            , 7.5
            , 8
            , 8.5
            , 9
            , 9.5
            , 10
            , 10.5
            , 11
            , 11.5
            , 12
            , 12.5
            , 13
            , 13.5
            , 14
            , 14.5
            , 15
            , 15.5
            , 16
    };

    public static int bin(double odds) {

        if (!(odds >= -10.42 && odds <= 15.96))
            throw new RuntimeException("odds ratio is invalid " + odds);

        for (int i = 0; i < intervals.length - 1; i++) {
            final double floor = intervals[i];
            final double ceiling = intervals[i + 1];
            if (odds >= floor && odds < ceiling) {
                return i;
            }
        }

        throw new RuntimeException("cannot find the bin for " + odds);
    }

    public static void main(String[] args) {

        System.out.println(intervals.length);

        System.out.println(bin(-10.42));
        System.out.println(bin(15.96));
    }
}

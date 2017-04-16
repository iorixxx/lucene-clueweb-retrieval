package edu.anadolu.freq;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Random;

/**
 * Unit test for length normalized binning strategy
 */
public class LengthNormalizedTest {

    private class SlowLengthNormalized extends LengthNormalized {

        private final double[] ranges;

        public SlowLengthNormalized(int numOfBins) {
            super(numOfBins);
            ranges = new double[numOfBins + 1];

            for (int i = 0; i < ranges.length; i++)
                ranges[i] = (double) i / numOfBins;

            //   for (int i = 0; i < ranges.length; i++)
            //     System.out.println(i + " " + ranges[i]);

            // System.out.println(Arrays.toString(ranges));
        }

        @Override
        public int bin(final double percentage) {

            double lower;
            double upper = -1d;

            int i;

            for (i = 0; i < ranges.length - 1; i++) {
                lower = ranges[i];
                upper = ranges[i + 1];

                if (lower <= percentage && percentage < upper)
                    return i + 1;
            }

            if (percentage == upper)
                return i;

            throw new RuntimeException("impossible state : percentage does not fit any range = " + percentage);

        }

    }

    private void compare(LengthNormalized strategy, SlowLengthNormalized slow, double relativeFreq) {

        final int result = strategy.calculateBinValue(relativeFreq);
        final int result2 = slow.calculateBinValue(relativeFreq);
        Assert.assertEquals("numOfBins = " + strategy.numOfBins + " relativeFreq = " + relativeFreq, result, result2);
    }

    /**
     * Try with different interval values of a given strategy
     */
    private void smokeIntervals(LengthNormalized strategy, SlowLengthNormalized slow) {

        int n = 800000;
        Random random = new Random(System.currentTimeMillis());
        for (int i = 1; i <= n; i++) {


            double relativeFreq = random.nextDouble();
            compare(strategy, slow, relativeFreq);


            relativeFreq = (double) i / n;
            compare(strategy, slow, relativeFreq);
        }
    }

    /**
     * No problem occurs when numOfBins=1000 is used
     */
    @Test
    public void smoke1000() {
        smokeIntervals(new LengthNormalized(1000), new SlowLengthNormalized(1000));
    }

    @Test(expected = RuntimeException.class)
    public void outOfBounds() {
        new SlowLengthNormalized(1000).bin(1.1);
    }

    @Test(expected = RuntimeException.class)
    public void outOfBound() {
        new LengthNormalized(1000).calculateBinValue(1.1);
    }


    @Ignore
    @Test(expected = RuntimeException.class)
    public void outOfBound1() {
        new SlowLengthNormalized(1000).calculateBinValue(0.0);
    }

    @Ignore
    @Test(expected = RuntimeException.class)
    public void outOfBound2() {
        new LengthNormalized(1000).calculateBinValue(0.0);
    }

    /**
     * Try with different bin values
     */
    @Test
    @Ignore
    public void smokeNumBins() {
        for (int j = 0; j < 1000; j++) {
            int seed = (int) (Math.random() * 1000) + 44;
            smokeIntervals(new LengthNormalized(seed), new SlowLengthNormalized(seed));
        }
    }
}

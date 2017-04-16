package edu.anadolu.knn;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Test ChiSquare Implementations
 */
public class ChiSquareTest {

    Long[] generateArray() {
        final Long[] array = new Long[1001];

        for (int i = 0; i < array.length; i++)
            array[i] = (long) (Math.random() * 48730502L);

        return array;
    }

    List<Long[]> generateList() {
        List<Long[]> list = new ArrayList<>(1);
        list.add(generateArray());

        return list;
    }

    @Test
    public void smokeUnEqualDataPoints() {
        for (int i = 0; i < 1000; i++)
            testUnEqualDataPoints();
    }

    @Test
    public void testUnEqualDataPoints() {

        Double[] array1 = TFDAwareNeed.averageAndDF(generateList());
        Double[] array2 = TFDAwareNeed.averageAndDF(generateList());

        for (boolean divide : new boolean[]{true, false})
            for (boolean cdf : new boolean[]{true, false}) {

                if (cdf && divide) continue;

                double chi1 = new ChiSquare(divide, cdf).chiCDF(array1, array2);
                double chi2 = new UnEqualDataPoints(divide, cdf).chiCDF(array1, array2);

                Assert.assertEquals("divide = " + divide + " cdf = " + cdf, chi1, chi2, 0.000000000001);

            }

    }
}

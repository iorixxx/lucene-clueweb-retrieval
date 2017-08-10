package edu.anadolu;

import edu.anadolu.knn.TFDAwareNeed;
import org.junit.Assert;
import org.junit.Test;

public class DecoratorTest {

    @Test
    public void testInsertZerothPosition() {

        Long[] input = {1L, 2L, 3L, 4L, 5L};

        long df = TFDAwareNeed.df(input);

        Assert.assertEquals(15, df);

        Long[] output = Decorator.insertZerothPosition(input, TFDAwareNeed.df(input));

        Assert.assertArrayEquals(new Long[]{15L, 1L, 2L, 3L, 4L}, output);

    }

    @Test
    public void testInsertZerothPosition2() {

        Long[] input = {1L, 2L, 3L, 4L, 0L};

        long df = TFDAwareNeed.df(input);

        Assert.assertEquals(10, df);

        Long[] output = Decorator.insertZerothPosition(input, TFDAwareNeed.df(input));

        Assert.assertArrayEquals(new Long[]{10L, 1L, 2L, 3L, 4L}, output);

    }

    @Test
    public void testArrayClone() {

        Long[] input = {1L, 2L, 3L, 4L, 0L};
        Long[] clone = input.clone();

        Assert.assertArrayEquals(input, clone);

        input[2] = -1L;

        Assert.assertNotEquals(input[2], clone[2]);
    }

}

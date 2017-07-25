package org.clueweb09.tracks;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test extact tag method
 */
public class TestTrack {

    @Test
    public void test1() {
        Assert.assertEquals(Track.extract("<task id = \"2\">", "id"), "2");
    }

    @Test
    public void test2() {
        Assert.assertEquals(Track.extract("<task id = \"43\">", "id"), "43");
    }


    @Test
    public void test3() {
        Assert.assertEquals(Track.extract("<task id=\"3\">", "id"), "3");
    }

    @Test
    public void test4() {
        Assert.assertEquals(Track.extract("<task id=\"50\">", "id"), "50");
    }

    @Test
    public void test5() {
        Assert.assertEquals(Track.extract("<topic number=\"58\" type=\"ambiguous\">", "number"), "58");
    }

    @Test
    public void test6() {
        Assert.assertEquals(Track.extract("<topic number=\"58\" type=\"ambiguous\">", "type"), "ambiguous");
    }

    @Test
    public void test7() {
        Assert.assertEquals(Track.extract("  <topic number = \"88\" type = \"faceted\">", "number"), "88");
    }

    @Test
    public void test8() {
        Assert.assertEquals(Track.extract("  <topic number = \"88\" type = \"faceted\">", "type"), "faceted");
    }

}

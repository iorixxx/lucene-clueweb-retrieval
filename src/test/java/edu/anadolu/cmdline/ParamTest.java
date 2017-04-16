package edu.anadolu.cmdline;

import edu.anadolu.similarities.BM25c;
import edu.anadolu.similarities.DirichletLM;
import edu.anadolu.similarities.LGDc;
import edu.anadolu.similarities.PL2c;
import org.apache.lucene.search.similarities.ModelBase;
import org.junit.Assert;
import org.junit.Test;

/**
 * testString2Model()
 */
public class ParamTest {


    @Test
    public void testLGD() {
        ModelBase modelBase = ParamTool.string2model("LGDc2.0");
        Assert.assertTrue(modelBase instanceof LGDc);
        Assert.assertEquals(modelBase.toString(), "LGDc2.0");

    }

    @Test
    public void testDirichlet() {
        ModelBase modelBase = ParamTool.string2model("DirichletLMc500.0");
        Assert.assertTrue(modelBase instanceof DirichletLM);
        Assert.assertEquals(modelBase.toString(), "DirichletLMc500.0");

    }

    @Test
    public void testPL2c() {
        ModelBase modelBase = ParamTool.string2model("PL2c10.0");
        Assert.assertTrue(modelBase instanceof PL2c);
        Assert.assertEquals(modelBase.toString(), "PL2c10.0");

    }

    @Test
    public void testBM25() {
        ModelBase modelBase = ParamTool.string2model("BM25k1.6b0.4");
        Assert.assertTrue(modelBase instanceof BM25c);
        Assert.assertEquals(modelBase.toString(), "BM25k1.6b0.4");

    }
}

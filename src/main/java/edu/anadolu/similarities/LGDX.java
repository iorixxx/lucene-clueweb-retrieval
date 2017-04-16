package edu.anadolu.similarities;

import edu.anadolu.exp.Prob;
import edu.anadolu.freq.TFNormalization;
import org.apache.lucene.search.similarities.EModelBase;

/**
 * X version of {@link LGD}
 */
public final class LGDX extends EModelBase {

    final TFNormalization normalization;
    final Prob prob;

    public LGDX(TFNormalization normalization, String tag, String home, Prob prob) throws Exception {
        super(normalization, tag, home);
        this.normalization = normalization;
        this.prob = prob;
    }

    @Override
    protected double score(String term, double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {

        if (!DFMap.containsKey(term)) throw new RuntimeException("DF map does not contain the term:" + term);

        final double tfn = normalization.tfn((int) tf, docLength, averageDocumentLength);

        long cdf = sqlCDF(term, tfn);

        long df = DFMap.get(term);

        if (cdf > df) throw new RuntimeException("CDF > DF " + tag + "{" + term + "}" + tfn);

        double myProb = prob.prob(cdf, df);

        return -1 * log2(myProb);
    }

    @Override
    public String toString() {
        return "LGDX" + normalization.toString() + prob.toString();
    }

}

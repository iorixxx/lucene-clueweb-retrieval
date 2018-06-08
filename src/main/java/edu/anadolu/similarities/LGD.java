package edu.anadolu.similarities;

import edu.anadolu.freq.TFNormalization;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.ModelBase;

/**
 * This class implements the LGD weighting model. For more information about
 * this model, see:
 * <ol>
 * <li>Stephane Clinchant and Eric Gaussier.
 * <a href="http://www.springerlink.com/content/f66q1228432w6605/">Bridging
 * Language Modeling and Divergence From Randomness Approaches: A Log-logistic
 * Model for IR</a>. ICTIR 2009, London, UK.</li>
 * <li>Stephane Clinchant and Eric Gaussier. Information-Based Models for Ad Hoc
 * Information Retrieval. SIGIR 2010, Geneva, Switzerland.</li>
 * </ol>
 */
public final class LGD extends ModelBase {

    protected float score(BasicStats stats, float freq, float docLen) {

        double TF = freq * log2(1.0d + (stats.getAvgFieldLength()) / docLen);

        double local_freq = (1.0D * stats.getDocFreq()) / (1.0D * stats.getNumberOfDocuments());

        double returnValue = log2((freq + TF) / local_freq);

        return (float) returnValue;

    }

    final TFNormalization normalization;

    public LGD(TFNormalization normalization) {
        this.normalization = normalization;
    }

    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {
        final double tfn = normalization.tfn((int) tf, docLength, averageDocumentLength);
        double lambda = (1.0D * documentFrequency) / (1.0D * numberOfDocuments);
        return keyFrequency * log2((lambda + tfn) / lambda);
    }

    @Override
    public String toString() {
        return "LGD" + normalization.toString();
    }
}

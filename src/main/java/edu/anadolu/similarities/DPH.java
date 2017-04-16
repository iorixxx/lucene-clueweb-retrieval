package edu.anadolu.similarities;

import org.apache.lucene.search.similarities.ModelBase;

/**
 * This class implements the DPH hypergeometric weighting model. P
 * stands for Popper's normalization. This is a parameter-free
 * weighting model. Even if the user specifies a parameter value, it will <b>NOT</b>
 * affect the results. It is highly recommended to use the model with query expansion.
 * <p><b>References</b>
 * <ol>
 * <li>FUB, IASI-CNR and University of Tor Vergata at TREC 2007 Blog Track. G. Amati
 * and E. Ambrosi and M. Bianchi and C. Gaibisso and G. Gambosi. Proceedings of
 * the 16th Text REtrieval Conference (TREC-2007), 2008.</li>
 * <li>Frequentist and Bayesian approach to  Information Retrieval. G. Amati. In
 * Proceedings of the 28th European Conference on IR Research (ECIR 2006).
 * LNCS vol 3936, pages 13--24.</li>
 * </ol>
 */
public class DPH extends ModelBase {

    protected float score(BasicStats stats, float freq, float docLen) {

        double f = freq / docLen;
        double norm = (1d - f) * (1d - f) / (freq + 1d);

        // averageDocumentLength => stats.getAvgFieldLength()
        // numberOfDocuments => stats.getNumberOfDocuments()
        // termFrequency => stats.getTotalTermFreq()

        double returnValue = norm
                * (freq * log2((freq *
                stats.getAvgFieldLength() / docLen) *
                stats.getNumberOfDocuments() / stats.getTotalTermFreq())
                + 0.5d * log2(2d * Math.PI * freq * (1d - f))
        );

        return (float) returnValue;
    }

    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {
        double f = relativeFrequency(tf, docLength);
        double norm = (1d - f) * (1d - f) / (tf + 1d);

        return keyFrequency * norm
                * (tf * log2((tf *
                averageDocumentLength / docLength) *
                (numberOfDocuments / termFrequency))
                + 0.5d * log2(2d * Math.PI * tf * (1d - f))
        );
    }

    @Override
    public String toString() {
        return "DPH";
    }
}

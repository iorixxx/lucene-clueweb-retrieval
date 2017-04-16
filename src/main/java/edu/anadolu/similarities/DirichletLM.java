package edu.anadolu.similarities;

import org.apache.lucene.search.similarities.ModelBase;

/**
 * Bayesian smoothing with Dirichlet Prior. This has one parameter, mu &gt; 0. This class sets mu to 2500 by default.
 * <p>
 * The retrieval performance of this weighting model has been empirically verified to be similar to that reported
 * below. This model is formulated such that all scores are &gt; 0.
 * <p>A Study of Smoothing Methods for Language Models Applied to Information Retrieval.
 * Zhai & Lafferty, ACM Transactions on Information Systems, Vol. 22, No. 2, April 2004, Pages 179--214.
 * <p>
 */
public final class DirichletLM extends ModelBase {

    final double c;

    public DirichletLM(double c) {
        this.c = c;
    }

    public DirichletLM() {
        this(2500);
    }

    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {
        return log2(1 + (tf / (c * (termFrequency / numberOfTokens)))) + log2(c / (docLength + c));
    }

    @Override
    public String toString() {
        return "DirichletLMc" + c;
    }
}

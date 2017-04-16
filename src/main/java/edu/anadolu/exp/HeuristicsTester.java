package edu.anadolu.exp;

import edu.anadolu.freq.L2;
import edu.anadolu.similarities.DFIC;
import edu.anadolu.similarities.DFRee;
import edu.anadolu.similarities.DPH;
import edu.anadolu.similarities.LGD;
import org.apache.lucene.search.similarities.ModelBase;

/**
 * A Formal Study of Information Retrieval Heuristics
 *
 * @see @link{http://www.eecis.udel.edu/~hfang/pubs/sigir04-formal.pdf}
 */
public class HeuristicsTester {
    /**
     * Term statistics
     */
    double documentFrequency = 1078702;
    /**
     * The document frequency of the term in the collection.
     */
    double termFrequency = 2330772; /** The term frequency in the collection.*/

    /**
     * Collection wide statistics
     */
    double numberOfDocuments = 48735852d;
    /**
     * The number of documents in the collection.
     */
    double numberOfTokens = 37407079119d; /** The number of tokens in the collections. */


    /**
     * Statistics of document being scored
     */
    long docLength = 250;

    /**
     * Term Frequency Constraint One (TFC1)
     *
     * @param model term-weighting model
     * @return false if constraint is not satisfied by the term-weighting model
     */
    boolean TFC1(ModelBase model) {

        for (int tf = 1; tf <= docLength - 1; tf++) {

            double f1 = model.f(tf, docLength, documentFrequency, termFrequency, numberOfDocuments, numberOfTokens);
            double f2 = model.f(tf + 1, docLength, documentFrequency, termFrequency, numberOfDocuments, numberOfTokens);

            if (f2 <= f1) {
                System.out.println("TFC1 false tf@" + tf);
                //return false;
            }
        }
        return true;
    }

    void _TFC2(ModelBase model) {

        System.out.println(model.toString() + ": TFC2=" + TFC2(model));
    }

    void _TFC1(ModelBase model) {

        System.out.println(model.toString() + ": TFC1=" + TFC1(model));
    }


    /**
     * Term Frequency Constraint Two (TFC2)
     *
     * @param model term-weighting model
     * @return false if constraint is not satisfied by the term-weighting model
     */
    boolean TFC2(ModelBase model) {

        for (int tf = 1; tf <= docLength - 3; tf++) {

            double f1 = model.f(tf, docLength, documentFrequency, termFrequency, numberOfDocuments, numberOfTokens);
            double f2 = model.f(tf + 1, docLength, documentFrequency, termFrequency, numberOfDocuments, numberOfTokens);

            double f3 = model.f(tf + 2, docLength, documentFrequency, termFrequency, numberOfDocuments, numberOfTokens);
            double f4 = model.f(tf + 3, docLength, documentFrequency, termFrequency, numberOfDocuments, numberOfTokens);

            if (f4 - f3 >= f2 - f1) {
                System.out.println("TFC2 false tf@" + tf);
                // return false;
            }
        }
        return true;
    }

    /**
     * Term Discrimination Constraint (TDC)
     *
     * @param model term-weighting model
     * @return false if constraint is not satisfied by the term-weighting model
     */
    boolean TDC(ModelBase model) {

        int queryFreq = 10;

        int w1d1 = 7;
        int w2d1 = queryFreq - w1d1;


        int w1d2 = w1d1 - 2; // w1d2 <= w1d1

        int w2d2 = w1d1 + w2d1 - w1d2; // w1d1 + w2d1 = w1d2 + w2d2

        System.out.println("w1d1(" + w1d1 + ") + w2d1(" + w2d1 + ") = w1d2(" + w1d2 + ") + w2d2(" + w2d2 + ")");


        double f1 =
                model.f(w1d1, docLength, documentFrequency, termFrequency, numberOfDocuments, numberOfTokens) +
                        model.f(w2d1, docLength, documentFrequency * 2, termFrequency * 2, numberOfDocuments, numberOfTokens);

        double f2 =
                model.f(w1d2, docLength, documentFrequency, termFrequency, numberOfDocuments, numberOfTokens) +
                        model.f(w2d2, docLength, documentFrequency * 2, termFrequency * 2, numberOfDocuments, numberOfTokens);

        return f1 >= f2;

    }

    /**
     * Length Normalization Constraint One (LNC1)
     *
     * @param model term-weighting model
     * @return false if constraint is not satisfied by the term-weighting model
     */
    boolean LNC1(ModelBase model) {

        for (int tf = 1; tf <= docLength; tf++) {

            double f1 = model.f(tf, docLength, documentFrequency, termFrequency, numberOfDocuments, numberOfTokens);
            double f2 = model.f(tf, docLength + 1, documentFrequency, termFrequency, numberOfDocuments, numberOfTokens);

            if (f2 >= f1) return false;
        }
        return true;
    }

    /**
     * Length Normalization Constraint Two (LNC2)
     *
     * @param model term-weighting model
     * @return false if constraint is not satisfied by the term-weighting model
     */
    boolean LNC2(ModelBase model) {

        for (int tf = 1; tf < docLength / 2; tf++) {

            double f1 = model.f(tf, docLength, documentFrequency, termFrequency, numberOfDocuments, numberOfTokens);
            // we concatenate a document with itself to form a new document
            double f2 = model.f(tf * 2, docLength * 2, documentFrequency, termFrequency, numberOfDocuments, numberOfTokens);

            if (f2 < f1) return false;
        }
        return true;
    }

    /**
     * Interaction between TF and document length
     *
     * @param model term-weighting model
     * @return false if constraint is not satisfied by the term-weighting model
     */
    boolean TF_LNC(ModelBase model) {

        for (int tf = 1; tf < docLength / 2; tf++) {

            double f1 = model.f(tf, docLength, documentFrequency, termFrequency, numberOfDocuments, numberOfTokens);
            // we add more occurrences of the query term
            double f2 = model.f(tf + 2, docLength + 2, documentFrequency, termFrequency, numberOfDocuments, numberOfTokens);

            // f2 should be higher
            if (f2 <= f1) return false;
        }
        return true;
    }

    /**
     * Score produced when tf increased from 1 to docLength
     *
     * @param model term-weighting model
     */
    void scoreDist(ModelBase model) {

        for (int tf = 1; tf <= docLength; tf++) {

            double score = model.f(tf, docLength, documentFrequency, termFrequency, numberOfDocuments, numberOfTokens);

            System.out.println("tf: " + tf + " " + model.toString() + " score:" + score);
        }

    }


    public static void main(String[] args) {
        HeuristicsTester tester = new HeuristicsTester();

        tester._TFC1(new DPH());
        tester._TFC2(new DPH());

        ModelBase model = new DFIC(); //LGD(new L2());

        System.out.println(tester.TFC1(model));
        System.out.println(tester.TFC2(model));

        System.out.println(tester.TDC(model));

        System.out.println(tester.LNC1(model));
        System.out.println(tester.LNC2(model));
        System.out.println(tester.TF_LNC(model));


        tester.scoreDist(new DPH());
        tester.scoreDist(new LGD(new L2()));
        tester.scoreDist(new DFIC());
        tester.scoreDist(new DFRee());
    }
}

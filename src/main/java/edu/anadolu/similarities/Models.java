package edu.anadolu.similarities;


import edu.anadolu.freq.L0;
import org.apache.lucene.search.similarities.*;

import java.util.*;

/**
 * {@link Similarity} implementations available in Lucene.
 */
public final class Models {

    /**
     * The DFR basic models to test.
     */
    static final BasicModel[] BASIC_MODELS = {
            new BasicModelBE(), new BasicModelD(), new BasicModelG(),
            new BasicModelIF(), new BasicModelIn(), new BasicModelIne(),
            new BasicModelP()
    };
    /**
     * The DFR aftereffects to test.
     */
    static final AfterEffect[] AFTER_EFFECTS = {
            new AfterEffectB(), new AfterEffectL(), new AfterEffect.NoAfterEffect()
    };
    /**
     * The DFR normalizations to test.
     */
    static final Normalization[] NORMALIZATIONS = {
            new NormalizationH1(), new NormalizationH2(), new NormalizationH3(),
            new NormalizationZ(), new Normalization.NoNormalization()
    };
    /**
     * The distributions for IB.
     */
    static final Distribution[] DISTRIBUTIONS = {
            new DistributionLL(), new DistributionSPL()
    };
    /**
     * Lambdas for IB.
     */
    static final Lambda[] LAMBDAS = {
            new LambdaDF(), new LambdaTTF()
    };

    private static final List<Similarity> sims;

    public static List<String> stringValues() {
        List<String> list = new ArrayList<>();
        for (ModelBase model : values())
            list.add(model.toString());
        return list;

    }

    /**
     * Create DFI extension of given collection of models
     *
     * @param sims collection of models
     * @return delegates
     */
    public static Collection<ModelBase> delegates(Collection<ModelBase> sims) {
        List<ModelBase> delegates = new ArrayList<>();

        for (ModelBase model : sims)
            delegates.add(new Delegate(model));

        return delegates;
    }

    public static Collection<ModelBase> values() {
        List<ModelBase> sims = new ArrayList<>();

        sims.add(new BM25c(2.425, 0.2));
        sims.add(new LGDc(16.25));
        sims.add(new PL2c(18.5));

        sims.add(new RawTF());
        sims.add(new LogTFN(new L0(), 0));
        sims.add(new DFIC());
        sims.add(new DPH());

        sims.add(new DLH13());

        sims.add(new DFRee());

        /**
         TFNormalization[] normalizations = {new L0(), new L1(), new L2()};
         for (TFNormalization normalization : normalizations) {
         sims.add(new LogTFN(normalization, 0));
         sims.add(new LogTFN(normalization, 1));
         sims.add(new SqrtTFN(normalization));
         }
         **/

        // sims.addAll(delegates(sims));

        Collections.sort(sims, (o1, o2) -> o1.toString().compareTo(o2.toString()));
        return Collections.unmodifiableCollection(sims);
    }

    static {

        sims = new ArrayList<>();
        sims.add(new ClassicSimilarity());
        sims.add(new BM25Similarity());
        for (BasicModel basicModel : BASIC_MODELS) {
            for (AfterEffect afterEffect : AFTER_EFFECTS) {
                for (Normalization normalization : NORMALIZATIONS) {
                    sims.add(new DFRSimilarity(basicModel, afterEffect, normalization));
                }
            }
        }
        for (Distribution distribution : DISTRIBUTIONS) {
            for (Lambda lambda : LAMBDAS) {
                for (Normalization normalization : NORMALIZATIONS) {
                    sims.add(new IBSimilarity(distribution, lambda, normalization));
                }
            }
        }
        sims.add(new LMDirichletSimilarity());
        sims.add(new LMJelinekMercerSimilarity(0.1f));
        sims.add(new LMJelinekMercerSimilarity(0.7f));

    }

    public static void main(String[] args) {

        for (Similarity similarity : sims)
            System.out.println(similarity.toString());

        for (Similarity similarity : Models.values())
            System.out.println(similarity.toString());
    }
}

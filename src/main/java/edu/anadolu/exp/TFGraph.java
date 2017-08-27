package edu.anadolu.exp;

import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.similarities.DPH;
import org.apache.lucene.search.similarities.ModelBase;

import java.util.List;

/**
 * Draws Term Frequency Graph: concave convex etc.
 */
public final class TFGraph extends ScorpioExperiment {

    public TFGraph(ModelBase model, String tag, DataSet dataSet) {
        super(model, tag, dataSet);
    }

    public void graph(String... term) {

        for (String t : term) {
            System.out.print("\t" + t);
        }

        System.out.println();
        for (int tf = 1; tf <= 200; tf++) {

            System.out.print(tf + "\t");

            for (String t : term) {

                double score = model.f(tf, docLength, termStatsMap.get(t).docFreq(), termStatsMap.get(t).totalTermFreq(), numberOfDocuments, numberOfTokens);
                System.out.print(String.format("%.7f", score));
                System.out.print("\t");

            }

            System.out.println();
        }
    }

    public void graph(String t, List<ModelBase> models) {

        for (ModelBase model : models) {
            System.out.print("\t" + model.toString());
        }

        System.out.println();

        for (int tf = 1; tf <= 200; tf++) {

            System.out.print(tf + "\t");

            for (ModelBase model : models) {

                double score = model.f(tf, docLength, termStatsMap.get(t).docFreq(), termStatsMap.get(t).totalTermFreq(), numberOfDocuments, numberOfTokens);
                System.out.print(String.format("%.7f", score));
                System.out.print("\t");

            }

            System.out.println();
        }
    }
}

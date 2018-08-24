package edu.anadolu.exp;

import edu.anadolu.QueryBank;
import edu.anadolu.QuerySelector;
import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.freq.L0;
import edu.anadolu.similarities.*;
import edu.anadolu.stats.TermStats;
import org.apache.commons.math3.util.Precision;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.ModelBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.clueweb09.tracks.Track.whiteSpaceSplitter;

/**
 * Tabula rasa
 */
public class ScorpioExperiment {

    final ModelBase model;
    int iterations;
    final DataSet dataSet;
    final protected String tag;
    protected final Map<String, TermStats> termStatsMap;


    final long numberOfDocuments;
    final long numberOfTokens;

    final protected Analyzer analyzer;

    /**
     * Statistics of document being scored
     */
    final long docLength;

    public ScorpioExperiment(ModelBase model, String tag, DataSet dataSet) {
        this.model = model;
        this.dataSet = dataSet;
        this.tag = tag;
        this.analyzer = Analyzers.analyzer(Tag.tag(tag));
        this.termStatsMap = new QuerySelector(dataSet, tag).loadTermStatsMap();

        String line = Evaluator.loadCorpusStats(dataSet.collectionPath(), "contents", tag);
        String[] parts =  whiteSpaceSplitter.split(line);

        if (parts.length != 4)
            throw new RuntimeException("line from field_stats.csv does not have four parts " + line);

        numberOfDocuments = Long.parseLong(parts[2]);
        numberOfTokens = Long.parseLong(parts[1]);

        docLength = numberOfTokens / numberOfDocuments;

        // System.out.println(numberOfDocuments + " " + numberOfTokens + " " + docLength);
    }

    public void expected(String... terms) throws IOException {
        for (String term : terms)
            System.out.println("expected(" + term + ") = " + expectedUnderDBar(Analyzers.getAnalyzedToken(term, analyzer)));
    }

    public double f(int tf, String term) {
        return model.f(tf, docLength, termStatsMap.get(Analyzers.getAnalyzedToken(term, analyzer)).docFreq(), termStatsMap.get(Analyzers.getAnalyzedToken(term, analyzer)).totalTermFreq(), numberOfDocuments, numberOfTokens);
    }

    public double expectedUnderDBar(String term) {
        return Precision.round((double) termStatsMap.get(term).totalTermFreq() / numberOfDocuments, 2);
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public void scorpio2(String t1, String t2) {


        List<PairScore> list = new ArrayList<>();

        for (int count1 = 1; count1 < iterations; count1++)

            for (int count2 = 1; count2 < iterations; count2++) {

                double score = f(count1, t1) + f(count2, t2);

                list.add(new PairScore(count1, count2, score));


            }

        Collections.sort(list);
        System.out.print(model.toString() + " : ");
        printList(list, t1, t2);

    }

    private void printList(List<PairScore> list, String t1, String t2) {
        for (int i = 0; i < 5; i++) {
            System.out.print(list.get(i).toString(t1, t2));
            System.out.print(" | ");
        }
        System.out.println();
    }

    public void scorpio(String t1, String t2) {


        int count1 = 1, count2 = 1;

        int i;
        for (i = 0; i < iterations; i++) {

            double score0 = f(count1, t1) + f(count2, t2);

            double score1 = f(count1 + 1, t1) + f(count2, t2);

            double score2 = f(count1, t1) + f(count2 + 1, t2);

            if (score1 <= score0 && score2 <= score0)
                break;

            if (score1 > score2)
                count1++;
            else
                count2++;
        }
        System.out.println(model.toString() + ": after " + i + " iterations " + t1 + "(" + count1 + ")," + t2 + "(" + count2 + ")");

    }

    public static void main(String[] args) throws IOException {

        final String tfd_home = "/Users/iorixxx/clueweb09";

        Collection collection = Collection.CW09A;

        DataSet dataSet = CollectionFactory.dataset(collection, tfd_home);


        List<ModelBase> sims = new ArrayList<>();

        sims.add(new DFIC());
        sims.add(new DFRee());
        sims.add(new DPH());

        sims.add(new BM25c(2.425, 0.2));
        sims.add(new LGDc(16.25));
        sims.add(new PL2c(18.5));

        sims.add(new DLH13());

        for (ModelBase modelBase : sims) {
            ScorpioExperiment experiment = new ScorpioExperiment(modelBase, "KStemAnalyzer", dataSet);
            experiment.setIterations(400);

            experiment.scorpio("sore", "throat");

            // topic 30
            experiment.scorpio("diabetes", "education");

            //topic 39
            experiment.scorpio("disneyland", "hotel");

            experiment.scorpio("the", "sun");

            experiment.scorpio("pork", "tenderloin");
        }

        sims.add(new LogTFN(new L0(), 0));
        for (ModelBase modelBase : sims) {
            ScorpioExperiment experiment = new ScorpioExperiment(modelBase, "KStemAnalyzer", dataSet);
            experiment.setIterations(200);

            experiment.scorpio2("sore", "throat");

            // topic 30
            experiment.scorpio2("diabetes", "education");

            //topic 39
            experiment.scorpio2("disneyland", "hotel");

            experiment.scorpio2("the", "sun");

            experiment.scorpio2("pork", "tenderloin");

            experiment.scorpio2("getting", "organized");

            experiment.scorpio2("quit", "smoking");

            experiment.scorpio2("south", "africa");
        }

/*
        experiment = new ScorpioExperiment(new LogTFN(new L0(), 0), 1000, analyzer, home);

        // topic 30
        experiment.scorpio("diabetes", "education");

        //topic 39
        experiment.scorpio("disneyland", "hotel");

        experiment = new ScorpioExperiment(new LGD(new L2()), 1000, analyzer, home);

        // topic 30
        experiment.scorpio("diabetes", "education");

        //topic 39
        experiment.scorpio("disneyland", "hotel");

        */


        ScorpioExperiment experiment = new ScorpioExperiment(new DPH(), "KStemAnalyzer", dataSet);

        experiment.expected("diabetes", "education", "disneyland", "hotel", "the", "sun", "poker", "tournaments", "ron", "howard", "getting", "organized", "elliptical", "trainer", "espn", "sports");

        List<StringDoublePair> list = new ArrayList<>();

        for (String s : new QueryBank(dataSet).distinctTerms(experiment.analyzer)) {

            String string = Analyzers.getAnalyzedToken(s, experiment.analyzer);
            double d = experiment.expectedUnderDBar(string);

            if (d > 1.0)
                list.add(new StringDoublePair(string, d));


        }

        Collections.sort(list);

        Collections.reverse(list);

        System.out.println(list);

    }

}

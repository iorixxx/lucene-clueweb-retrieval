package edu.anadolu.cmdline;

import edu.anadolu.Decorator;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.freq.Freq;
import edu.anadolu.knn.CartesianQueryTermSimilarity;
import edu.anadolu.knn.ChiSquare;
import edu.anadolu.knn.DiscountCartesianSimilarity;
import edu.anadolu.knn.TFDAwareNeed;
import org.apache.poi.ss.usermodel.Workbook;
import org.kohsuke.args4j.Option;

import java.util.Properties;

/**
 * Generates examples for PhD thesis
 */
class ExampleTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "Collection")
    private edu.anadolu.datasets.Collection collection;

    @Override
    public String getShortDescription() {
        return "Example Utility";
    }

    @Option(name = "-tag", metaVar = "[KStemAnalyzer|KStemAnalyzerAnchor]", required = false, usage = "Index Tag")
    protected String tag = "KStemAnalyzer";

    private Workbook workbook;

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        DataSet dataSet = CollectionFactory.dataset(collection, tfd_home);

        Decorator decorator = new Decorator(dataSet, tag, Freq.Rel);

        DiscountCartesianSimilarity similarity = new DiscountCartesianSimilarity(new ChiSquare(false, false), true, CartesianQueryTermSimilarity.Aggregation.Euclid, CartesianQueryTermSimilarity.Way.s);

        TFDAwareNeed R = decorator.tfdAwareNeed(188);
        TFDAwareNeed S = decorator.tfdAwareNeed(7);

        System.out.println(similarity.score(R, S));
    }

    @Override
    public String getHelp() {
        return "Generates examples for PhD thesis";
    }
}

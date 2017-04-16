package edu.anadolu.cmdline;

import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.exp.TFGraph;
import edu.anadolu.similarities.*;
import org.apache.lucene.search.similarities.ModelBase;
import org.apache.poi.ss.usermodel.Workbook;
import org.kohsuke.args4j.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Generates data for TFGraph
 */
public class TFTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "Collection")
    private edu.anadolu.datasets.Collection collection;

    @Override
    public String getShortDescription() {
        return "TFGraph Utility";
    }

    @Override
    public String getHelp() {
        return "Generates data for TFGraph";
    }

    @Option(name = "-tag", metaVar = "[KStemAnalyzer|KStemAnalyzerAnchor]", required = false, usage = "Index Tag")
    protected String tag = "KStemAnalyzer";

    @Option(name = "-model", metaVar = "[DPH|DFRee|DLH13]", required = false, usage = "TW Model")
    protected String model = "DPH";

    @Option(name = "-task", metaVar = "[term]", required = false, usage = "task")
    protected String task;

    @Option(name = "-term", metaVar = "[term]", required = false, usage = "word")
    protected String term;

    private Workbook workbook;

    private static ModelBase string2model(String model) {
        if ("DPH".equals(model))
            return new DPH();

        if ("DFRee".equals(model))
            return new DFRee();

        if ("DLH13".equals(model))
            return new DLH13();

        if ("DLH".equals(model))
            return new DLH();

        if ("DFIC".equals(model))
            return new DFIC();


        return ParamTool.string2model(model);
    }

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        DataSet dataSet = CollectionFactory.dataset(collection, tfd_home);

        if ("term".equals(task)) {

            String[] models = {"DFIC", "PL2c3.0", "DPH", "BM25k1.0b0.4", "DirichletLMc500.0", "LGDc2.0", "DFRee", "DLH13"};

            List<ModelBase> list = new ArrayList<>();

            for (String s : models)
                list.add(string2model(s));

            TFGraph tfGraph = new TFGraph(null, tag, dataSet);

            tfGraph.graph(term, list);

            return;
        }

        TFGraph tfGraph = new TFGraph(string2model(model), tag, dataSet);
        tfGraph.graph("pork", "tenderloin", "disneyland", "hotel", "the", "for");


    }
}

package edu.anadolu.cmdline;

import edu.anadolu.LearningToSelect;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.eval.ModelScore;

import java.util.Properties;

/**
 * Tool for Learning to Select (LTS) framework
 */
public final class LTSTool extends EvaluatorTool {

    @Override
    public String getShortDescription() {
        return "Tool for Learning to Select (LTS) framework";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    @Override
    public void run(Properties props) throws Exception {

        this.models = "BM25k1.2b0.75_DirichletLMc2500.0_LGDc1.0_PL2c1.0_DPH_DFIC_DFRee_DLH13";
        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);

        Evaluator evaluator = new Evaluator(dataset, tag, measure, models, "evals", op);

        LearningToSelect lts = new LearningToSelect(dataset, tag, op);
        ModelScore modelScore = lts.evaluate(evaluator.evaluatorFromResidualNeeds());
        System.out.println(modelScore);
    }
}

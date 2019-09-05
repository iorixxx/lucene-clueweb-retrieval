package edu.anadolu.cmdline;

import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.exp.Word2VecTraverser;
import org.kohsuke.args4j.Option;

import java.nio.file.Paths;
import java.util.Properties;

public class DeepTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "Collection")
    private Collection collection;

    @Option(name = "-base", required = true, usage = "output file")
    private String base;

    @Override
    public String getShortDescription() {
        return "Word2Vec Tool for ClueWeb09 ClueWeb12 Gov2 collections";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " paths.docs paths.indexes paths.csv";
    }

    @Override
    public void run(Properties props) throws Exception {

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        if (parseArguments(props) == -1) return;


        final String docsPath = props.getProperty("paths.docs." + collection.toString());


        if (docsPath == null) {
            System.out.println(getHelp());
            return;
        }


        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);
        long start = System.nanoTime();


        Word2VecTraverser traverser = new Word2VecTraverser(dataset, docsPath);

        final int numThreads = props.containsKey("numThreads") ? Integer.parseInt(props.getProperty("numThreads")) : Runtime.getRuntime().availableProcessors();
        traverser.traverseParallel(Paths.get(base), numThreads);
        System.out.println("Document features are extracted in " + execution(start));
    }
}
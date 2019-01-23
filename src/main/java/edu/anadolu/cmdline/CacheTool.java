package edu.anadolu.cmdline;

import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.knn.Measure;
import org.apache.lucene.search.similarities.ModelBase;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static edu.anadolu.cmdline.ParamTool.train;

/**
 * Save best parameters in cache.properties file
 */
class CacheTool extends CmdLineTool {

    private String tfd_home;

    @Option(name = "-task", required = false, usage = "task to be executed")
    private String task;


    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println("tfd.home is mandatory for query statistics!");
            return;
        }

        if ("param".equals(task)) {
            param();
            return;
        }

        if ("spam".equals(task)) {
            spam();
            return;
        }

        if ("clear".equals(task)) {
            clear();
            return;
        }

    }

    private void clear() {

        Path cacheFile = Paths.get(tfd_home, "cache.properties");

        try {
            if (Files.deleteIfExists(cacheFile))
                System.out.println("deleted : " + cacheFile.toString());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        Collection[] collections = {Collection.CW09A, Collection.CW09B, Collection.CW12B, Collection.MQ09, Collection.GOV2, Collection.ROB04, Collection.MQE2};

        for (final Collection collection : collections) {

            cacheFile = Paths.get(tfd_home, collection.toString()).resolve("cache.properties");

            try {
                if (Files.deleteIfExists(cacheFile))
                    System.out.println("deleted : " + cacheFile.toString());
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }

        }
    }

    private void spam() {

        Collection[] collections = {Collection.CW09A, Collection.CW09B, Collection.CW12B, Collection.MQ09, Collection.MQE2};

        for (final Collection collection : collections) {

            DataSet dataSet = CollectionFactory.dataset(collection, tfd_home);

            for (final String tag : tags)
                for (final Measure measure : Measure.values()) {
                    {
                        int spam = SpamEvalTool.bestSpamThreshold(dataSet, tag, measure, "OR");
                        String key = SpamEvalTool.cacheKey(tag, measure, "OR");
                        System.out.println(dataSet + " " + key + " = " + spam);
                    }

                }

        }
    }

    private void param() {

        Collection[] collections = {Collection.CW09A, Collection.CW09B, Collection.CW12B, Collection.GOV2, Collection.ROB04, Collection.MQ09, Collection.MQE2};

        for (final Collection collection : collections) {

            DataSet dataset = CollectionFactory.dataset(collection, tfd_home);

            for (final String tag : tags)
                for (final Measure measure : Measure.values()) {
                    for (String parametricModel : parametricModels) {
                        if ((Collection.GOV2.equals(collection) || Collection.ROB04.equals(collection)) && tags[1].equals(tag))
                            continue;
                        ModelBase modelBase = train(parametricModel, dataset, tag, measure, "OR");
                        String key = ParamTool.cacheKey(parametricModel, tag, measure, "OR");
                        System.out.println(dataset + " " + key + " = " + modelBase);
                    }

                }

        }
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    @Override
    public String getShortDescription() {
        return "Cache tool for determining best parameters of models";
    }
}

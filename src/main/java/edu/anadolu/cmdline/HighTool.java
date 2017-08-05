package edu.anadolu.cmdline;


import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.misc.HighFreqTerms;
import org.apache.lucene.misc.TermStats;
import org.apache.lucene.store.FSDirectory;
import org.kohsuke.args4j.Option;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.Properties;

import static org.apache.lucene.misc.HighFreqTerms.DEFAULT_NUMTERMS;
import static org.apache.lucene.misc.HighFreqTerms.getHighFreqTerms;

/**
 * HighFreqTerms class extracts the top n most frequent terms
 */
public class HighTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "underscore separated collection values", metaVar = "CW09A_CW12B")
    protected Collection collection;

    @Override
    public String getShortDescription() {
        return "Extracts the top n most frequent terms";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    @Option(name = "-field", metaVar = "[url|email|contents]", required = false, usage = "field that you want to search on")
    protected String field = "url";

    @Option(name = "-tag", usage = "If you want to search use specific tag, e.g. UAX or Script")
    private String tag = null;

    @Option(name = "-numTerms", required = false, usage = "number of terms")
    private int numTerms = DEFAULT_NUMTERMS;

    @Option(name = "-t", required = false, usage = "order by totalTermFreq")
    private boolean t = false;


    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        DataSet dataSet = CollectionFactory.dataset(collection, tfd_home);

        final long start = System.nanoTime();

        for (final Path path : discoverIndexes(dataSet)) {

            final String tag = path.getFileName().toString();

            // search for a specific tag, skip the rest
            if (this.tag != null && !tag.equals(this.tag)) continue;


            try (IndexReader reader = DirectoryReader.open(FSDirectory.open(path))) {

                Comparator<TermStats> comparator = t ? new HighFreqTerms.TotalTermFreqComparator() : new HighFreqTerms.DocFreqComparator();

                TermStats[] terms = getHighFreqTerms(reader, numTerms, field, comparator);

                System.out.println("term \t totalTF \t docFreq");

                for (TermStats termStats : terms) {
                    System.out.printf(Locale.ROOT, "%s \t %d \t %d \n",
                            termStats.termtext.utf8ToString(), termStats.totalTermFreq, termStats.docFreq);
                }

            }

        }

        System.out.println("High Frequency terms extracted in " + execution(start));

    }
}

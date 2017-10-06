package edu.anadolu.cmdline;


import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.misc.HighFreqTerms;
import org.apache.lucene.misc.TermStats;
import org.apache.lucene.store.FSDirectory;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;
import org.kohsuke.args4j.Option;
import org.xml.sax.DocumentHandler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.lucene.misc.HighFreqTerms.DEFAULT_NUMTERMS;
import static org.apache.lucene.misc.HighFreqTerms.getHighFreqTerms;

/**
 * QueryPerTFTool class extracts term frequencies per query from relevant documents of the query
 */
public class QueryPerTFTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "underscore separated collection values", metaVar = "CW09A_CW12B")
    protected Collection collection;

    @Override
    public String getShortDescription() {
        return "QueryPerTFTool class extracts term frequencies per query from relevant documents of the query";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    @Option(name = "-field", metaVar = "[url|email|contents]", required = false, usage = "field that you want to search on")
    protected String field = "contents";

    @Option(name = "-tag", required = true, usage = "If you want to search use specific tag, e.g. UAX or Script")
    private String tag = null;

    @Option(name = "-length", required = true, usage = "length of topic")
    private int length = 2;



    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        DataSet dataSet = CollectionFactory.dataset(collection, tfd_home);
        List<InfoNeed> infoneeds = dataSet.getTopics();
        //List<Track> tracks = Arrays.asList(dataSet.tracks());


        final long start = System.nanoTime();

        for(InfoNeed need: infoneeds){
            if(need.wordCount()!=length) continue;
            Set<String> relevantDocs = need.getJudgeMap().entrySet().stream().filter(e -> e.getValue() > 0)
                    .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue())).keySet();

            Set<String> fields = new TreeSet<String>();
            fields.add(field);

            for (final Path path : discoverIndexes(dataSet)) {

                final String tag = path.getFileName().toString();

                // search for a specific tag, skip the rest
                if (this.tag != null && !tag.equals(this.tag)) continue;


                try (IndexReader reader = DirectoryReader.open(FSDirectory.open(path))) {
                    HashMap<String, Integer> termFreqMap = termFreq4MultipleDocs(field, reader, relevantDocs);
                    for (Map.Entry<String, Integer> termStats : termFreqMap.entrySet()) {
                        System.out.printf(Locale.ROOT, "%s \t %d \n",
                                termStats.getKey(), termStats.getValue());
                    }

                }
            }
        }

        System.out.println("High Frequency terms extracted in " + execution(start));

    }

    private static HashMap<String,Integer> termFreq4MultipleDocs(String field,IndexReader reader,Set<String> relevantDocIds) throws IOException {
        HashMap<String,Integer> termFreqMap = new HashMap<>();

        for(String docid:relevantDocIds) {
            Terms terms = reader.getTermVector(Integer.valueOf(docid), field);

            TermsEnum termsEnum = terms.iterator();
            PostingsEnum postings = null;
            while (termsEnum.next() != null) {
                String term = termsEnum.term().utf8ToString();
                postings = termsEnum.postings(postings, PostingsEnum.FREQS);
                int freq = postings.freq();

                if (termFreqMap.containsKey(term)) termFreqMap.merge(term, freq, Integer::sum);
                else termFreqMap.put(term, freq);
            }
        }

        return termFreqMap;
    }


}

package edu.anadolu.cmdline;


import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.clueweb09.InfoNeed;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import static edu.anadolu.Indexer.FIELD_ID;

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

    @Option(name = "-length", required = false, usage = "length of topic")
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
            String query = need.query();
            Analyzer analyzer = Analyzers.analyzer(Tag.tag(tag));
            List<String> tokens = Analyzers.getAnalyzedTokens(query, analyzer);
            if(tokens.size()!=length) continue;
            System.out.println(query);


            Set<String> relevantDocs = need.getJudgeMap().entrySet().stream().filter(e -> e.getValue() > 0)
                    .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue())).keySet();

           // Set<String> nonRelevantDocs = need.getJudgeMap().entrySet().stream().filter(e -> e.getValue() <= 0)
            //        .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue())).keySet();


            Set<String> fields = new TreeSet<String>();
            fields.add(field);

            for (final Path path : discoverIndexes(dataSet)) {

                final String tag = path.getFileName().toString();

                // search for a specific tag, skip the rest
                if (this.tag != null && !tag.equals(this.tag)) continue;


                try (IndexReader reader = DirectoryReader.open(FSDirectory.open(path))) {
                    termFreq4MultipleDocs(tokens, field, reader, relevantDocs);
                   // System.out.println("NONRELEVANTS");
                   // termFreq4MultipleDocs(tokens,field, reader, nonRelevantDocs);
                }
            }
        }

        System.out.println("Term frequencies according to relevant and nonrelevant docs is completed  " + execution(start));

    }

    private static void termFreq4MultipleDocs(List<String> queryTokens, String field,IndexReader reader,Set<String> relevantDocIds) throws IOException {
        HashMap<String,ArrayList<TermTFStats>> termStats4RelevantDocs = new HashMap<>(); //DocId and Term stats
        for(String s:relevantDocIds) termStats4RelevantDocs.put(s,new ArrayList<>());

        for(String token:queryTokens) {
            Term term = new Term(field, token);
            PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader, field, term.bytes());

            if (postingsEnum == null){
                System.out.println(token + "(stopword)"+" Skipping...");
                return;
            }


            while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {

                final long freq = postingsEnum.freq();

                Document doc = reader.document(postingsEnum.docID());

                String docID = doc.get(FIELD_ID);

                if (relevantDocIds.contains(docID)){
                    //Relevant doc is detected
                    ArrayList<TermTFStats> entry = termStats4RelevantDocs.get(docID);
                    entry.add(new TermTFStats(term.text(),reader.totalTermFreq(term),freq,queryTokens.indexOf(token)));
                }
            }
        }
        printResults(termStats4RelevantDocs);

    }

    private static void printResults(HashMap<String,ArrayList<TermTFStats>> termStats4RelevantDocs) {
        boolean first2second=true;
        long TF1=0;
        long TF2=0;
        for (Map.Entry<String, ArrayList<TermTFStats>> entry : termStats4RelevantDocs.entrySet()){
            if(entry.getValue().size()!=2)continue;
            TF1=entry.getValue().get(0).totalTermFreq();
            TF2=entry.getValue().get(1).totalTermFreq();
            if(TF1 > TF2) first2second=false;
            break;
        }

        System.out.print("DocId:\t\t");
        for (Map.Entry<String, ArrayList<TermTFStats>> entry : termStats4RelevantDocs.entrySet())
            System.out.printf("%s\t", entry.getKey());

        System.out.printf("\nW1 tf:\t%d\t",TF1);
        for (Map.Entry<String, ArrayList<TermTFStats>> entry : termStats4RelevantDocs.entrySet()) {
            if (entry.getValue().size() > 0 && entry.getValue().get(0).termPositionInQuery() == 0)
                System.out.printf("%s %d\t", entry.getValue().get(0).term().utf8ToString(), entry.getValue().get(0).termFreq());
            else System.out.printf("%d\t", 0);
        }
        System.out.printf("\nW2 tf:\t%d\t",TF2);
        for (Map.Entry<String, ArrayList<TermTFStats>> entry : termStats4RelevantDocs.entrySet()) {
            if (entry.getValue().size() > 1 && entry.getValue().get(0).termPositionInQuery() == 1)
                System.out.printf("%s %d\t", entry.getValue().get(1).term().utf8ToString(), entry.getValue().get(1).termFreq());
            else System.out.printf("%d\t", 0);
        }

        System.out.print("\nRatio:\t\t");
        for (Map.Entry<String, ArrayList<TermTFStats>> entry : termStats4RelevantDocs.entrySet()) {
            if(entry.getValue().size()==0){
                System.out.println(entry.getKey()+" is skipping...(No term in the document ");

            }
            else if(entry.getValue().size()==1){
                if(first2second) {
                    if (entry.getValue().get(0).termPositionInQuery() == 0) System.out.printf("%s\t", "INFINITY");
                    else System.out.printf("%.4f\t", 0.0);
                }else{
                    if (entry.getValue().get(0).termPositionInQuery() == 0) System.out.printf("%.4f\t", 0.0);
                    else System.out.printf("%s\t", "INFINITY");
                }
            }
            else if(entry.getValue().size()==2){
                long w1 = entry.getValue().get(0).termFreq();
                long w2 = entry.getValue().get(1).termFreq();
                double ratio;
                if (first2second) ratio = w1 / (double)w2;
                else ratio = w2 / (double)w1;
                System.out.printf("%.4f\t", ratio);
            }else System.out.println("More than 2 !!! " + entry.getKey());
        }
        System.out.println("\n=========================================");
    }

    private static final class TermTFStats extends TermStatistics {

        private final long termFreq;
        private final int termPositionInQuery;
        public TermTFStats(String term, long totalTermFreq, long termFreq, int termPositionInQuery) {
            super(new BytesRef(term), 0l, totalTermFreq);
            this.termFreq = termFreq;
            this.termPositionInQuery = termPositionInQuery;
        }
        public final long termFreq() {
            return termFreq;
        }

        public final int termPositionInQuery() {
            return termPositionInQuery;
        }

    }


}

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

    @Option(name = "-orderFactor", metaVar = "[TF|avgTF(TF/docFreq)|DF]", required = true)
    protected String orderFactor = "avgTF";

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
                    termFreq4MultipleDocs(tokens, field, reader, relevantDocs,orderFactor);
                   // System.out.println("NONRELEVANTS");
                   // termFreq4MultipleDocs(tokens,field, reader, nonRelevantDocs);
                }
            }
        }

        System.out.println("Term frequencies according to relevant and nonrelevant docs is completed  " + execution(start));

    }

    private static void termFreq4MultipleDocs(List<String> queryTokens, String field,IndexReader reader,Set<String> relevantDocIds,String orderFactor) throws IOException {
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
                    entry.add(new TermTFStats(term.text(),reader.totalTermFreq(term),freq,queryTokens.indexOf(token),reader.docFreq(term)));
                }
            }
        }
        printResults(termStats4RelevantDocs,orderFactor);

    }

    private static void printResults(HashMap<String,ArrayList<TermTFStats>> termStats4RelevantDocs,String orderFactor) {
        boolean first2second=true;
        double orderVal1=0;
        double orderVal2=0;
        double totalRatio=0;
        if(orderFactor.equals("TF")){
            for (Map.Entry<String, ArrayList<TermTFStats>> entry : termStats4RelevantDocs.entrySet()) {
                if (entry.getValue().size() != 2) continue;
                orderVal1 = entry.getValue().get(0).totalTermFreq();
                orderVal2 = entry.getValue().get(1).totalTermFreq();
                if (orderVal1 > orderVal2) {
                    first2second = false;
                    totalRatio = orderVal2 / (double) orderVal1;
                } else totalRatio = orderVal1 / (double) orderVal2;
                break;
            }
        }else if(orderFactor.equals("DF")){
            for (Map.Entry<String, ArrayList<TermTFStats>> entry : termStats4RelevantDocs.entrySet()){
                if(entry.getValue().size()!=2)continue;
                orderVal1=entry.getValue().get(0).docFreq();
                orderVal2=entry.getValue().get(1).docFreq();
                if(orderVal1 > orderVal2){
                    first2second=false;
                    totalRatio=orderVal2/(double)orderVal1;
                } else totalRatio=orderVal1/(double)orderVal2;
                break;
            }
        }else if(orderFactor.equals("avgTF")) {
            for (Map.Entry<String, ArrayList<TermTFStats>> entry : termStats4RelevantDocs.entrySet()) {
                if (entry.getValue().size() != 2) continue;
                long df1 = entry.getValue().get(0).docFreq();
                long df2 = entry.getValue().get(1).docFreq();
                long tf1 = entry.getValue().get(0).totalTermFreq();
                long tf2 = entry.getValue().get(1).totalTermFreq();
                orderVal1=tf1/(double)df1;
                orderVal2=tf2/(double)df2;
                if (orderVal1 > orderVal2) {
                    first2second = false;
                    totalRatio = orderVal2 / orderVal1;
                } else totalRatio = orderVal1 / orderVal2;
                break;
            }
        }else return;

        System.out.print("DocId:\t\t");
        for (Map.Entry<String, ArrayList<TermTFStats>> entry : termStats4RelevantDocs.entrySet())
            System.out.printf("%s\t", entry.getKey());

        System.out.printf("\nW1 tf:\t%.4f\t",orderVal1);
        for (Map.Entry<String, ArrayList<TermTFStats>> entry : termStats4RelevantDocs.entrySet()) {
            if (entry.getValue().size() > 0 ){
                boolean isFound=false;
                for (TermTFStats t : entry.getValue()) {
                    if (t.termPositionInQuery() == 0) {
                        System.out.printf("%s %d\t", t.term().utf8ToString(), t.termFreq());
                        isFound=true;
                        break;
                    }
                }
                if(!isFound) System.out.printf("%d\t", 0);
            }
            else System.out.printf("%d\t", 0);
        }

        System.out.printf("\nW2 tf:\t%.4f\t",orderVal2);
        for (Map.Entry<String, ArrayList<TermTFStats>> entry : termStats4RelevantDocs.entrySet()) {
            if (entry.getValue().size() > 0 ) {
                boolean isFound=false;
                for (TermTFStats t : entry.getValue()) {
                    if (t.termPositionInQuery() == 1) {
                        System.out.printf("%s %d\t", t.term().utf8ToString(), t.termFreq());
                        isFound=true;
                        break;
                    }
                }
                if(!isFound) System.out.printf("%d\t", 0);
            }
            else System.out.printf("%d\t", 0);
        }

        System.out.print("\nRatio:\t"+totalRatio+"\t");
        for (Map.Entry<String, ArrayList<TermTFStats>> entry : termStats4RelevantDocs.entrySet()) {
            if(entry.getValue().size()==0){
                System.out.printf("%d\t",-2);  //No term freq for both tokens even if their document is relevant

            }
            else if(entry.getValue().size()==1){
                if(first2second) {
                    if (entry.getValue().get(0).termPositionInQuery() == 0) System.out.printf("%d\t", -1); //INFINITY
                    else System.out.printf("%.4f\t", 0.0);
                }else{
                    if (entry.getValue().get(0).termPositionInQuery() == 0) System.out.printf("%.4f\t", 0.0);
                    else System.out.printf("%d\t", -1);
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

        for (Map.Entry<String, ArrayList<TermTFStats>> entry : termStats4RelevantDocs.entrySet()) {
            if (entry.getValue().size() != 2) continue;
            double TF,DF,avgTF;
            long df1 = entry.getValue().get(0).docFreq();
            long df2 = entry.getValue().get(1).docFreq();
            long tf1 = entry.getValue().get(0).totalTermFreq();
            long tf2 = entry.getValue().get(1).totalTermFreq();
            if(first2second){
                TF=tf1 / (double) tf2;
                DF=df1 / (double) df2;
                avgTF = TF/DF;
            }else{
                TF=tf2 / (double) tf1;
                DF=df2 / (double) df1;
                avgTF = TF/DF;
            }

            System.out.printf("\nTFRatio\t%.4f\tDFRatio\t%.4f\tavgTF\t%.4f",TF,DF,avgTF);
            break;
        }
        System.out.println("\n=========================================");
    }

    private static final class TermTFStats extends TermStatistics {

        private final long termFreq;
        private final int termPositionInQuery;
        private final long documentFreq;

        public TermTFStats(String term, long totalTermFreq, long termFreq, int termPositionInQuery,long documentFreq) {
            super(new BytesRef(term), documentFreq, totalTermFreq);
            this.termFreq = termFreq;
            this.termPositionInQuery = termPositionInQuery;
            this.documentFreq=documentFreq;
        }
        public final long termFreq() {
            return termFreq;
        }

        public final int termPositionInQuery() {
            return termPositionInQuery;
        }

    }


}

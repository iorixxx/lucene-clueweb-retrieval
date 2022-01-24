package edu.anadolu.ltr;

import com.robrua.nlp.bert.Bert;
import edu.anadolu.Indexer;
import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.cmdline.CLI;
import edu.anadolu.cmdline.CmdLineTool;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.store.FSDirectory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.apache.solr.common.params.CommonParams.*;

/**
 * Tool that computes SEO-based document features.
 */
public class SEOTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "Collection")
    private Collection collection;

    @Argument
    private List<String> files = new ArrayList<>();

    @Option(name = "-out", required = true, usage = "output file")
    private String out;

    @Option(name = "-tag", usage = "If you want to search specific tag, e.g. KStemField")
    private String tag = null;

    @Option(name = "-type", usage = "seo or doc")
    private String type = null;

    @Option(name = "-resultsettype", usage = "resultset or featureset or all")
    private String resultsettype = null;

    @Option(name = "-seopart", required = false, usage = "meta,content,link,url,simcos,simbert (seperated by -) to divide seo features for fast computing")
    private String seopart = null;

    @Override
    public String getShortDescription() {
        return "SEO Tool for ClueWeb09 ClueWeb12 Gov2 collections";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " paths.docs paths.indexes paths.csv";
    }

    private Tag analyzerTag;
    private IndexReader reader;
    private String indexTag;

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


        String[] spamWiki = new String[]
                {
                        "clueweb09-enwp01-95-02016",
                        "clueweb09-enwp01-90-17134",
                        "clueweb09-enwp02-04-15021",
                        "clueweb09-enwp01-35-03270",
                        "clueweb09-enwp01-15-24594",
                        "clueweb09-enwp03-36-01635",
                        "clueweb09-enwp00-54-16573",
                        "clueweb09-enwp01-76-17822",
                        "clueweb09-enwp01-81-20329",
                        "clueweb09-enwp03-37-21416",
                        "clueweb09-enwp03-15-15563",
                        "clueweb09-enwp01-92-08869",
                        "clueweb09-enwp01-86-03020",
                        "clueweb09-enwp01-84-21637",
                        "clueweb09-enwp01-92-17846"
                };

        Set<String> docIdSet = new HashSet<>();
//        docIdSet.addAll(Arrays.asList(spamWiki));


        for (String file : files) {
            System.out.println(file);
            Path path = Paths.get(file);
            if (!(Files.exists(path) && Files.isRegularFile(path))) {
                System.out.println(getHelp());
                return;
            }
            if(resultsettype.equals("resultset")){
                docIdSet.addAll((Collection.GOV2.equals(collection)||Collection.MQ07.equals(collection)||Collection.MQ08.equals(collection)) ? retrieveDocIdSetForLetor(path) : retrieveDocIdSetFromResultset(path));
            }
            if(resultsettype.equals("featureset")){
                throw new RuntimeException("Reading from featureset is not ready yet!");
//                docIdSet.addAll((Collection.GOV2.equals(collection)||Collection.MQ07.equals(collection)||Collection.MQ08.equals(collection)) ? retrieveDocIdSetForLetor(path) : retrieveDocIdSet(path));
            }
        }

//        docIdSet.removeAll(retrieveDocIdSetFromExisting(Paths.get("Seo12B.txt")));


        System.out.println(docIdSet.size() + " docs will be processed.");
        
        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);
        long start = System.nanoTime();


        ///////////////////////////// Index Reading for stats ///////////////////////////////////////////
        Path indexPath=null;
//        if(this.tag == null)
//            indexPath = Files.newDirectoryStream(dataset.indexesPath(), Files::isDirectory).iterator().next();
//        else {
//            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataset.indexesPath(), Files::isDirectory)) {
//                for (Path path : stream) {
//                    if(!tag.equals(path.getFileName().toString())) continue;
//                    indexPath = path;
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        if(indexPath == null)
//            throw new RuntimeException(tag + " index not found");
//
//
//        this.indexTag = indexPath.getFileName().toString();
//        this.analyzerTag = Tag.tag(indexTag);
        this.analyzerTag = Tag.tag(tag);

//        this.reader = DirectoryReader.open(FSDirectory.open(indexPath));

        IndexSearcher searcher = null;
//        IndexSearcher searcher = new IndexSearcher(reader);
        CollectionStatistics collectionStatistics = null;
//        CollectionStatistics collectionStatistics = searcher.collectionStatistics(Indexer.FIELD_CONTENTS);

        Bert bert = null;


        List<IDocFeature> features = new ArrayList<>();
        if(type.equals("seo")){
            String type="cos";
            if(seopart==null) {
                features.add(new Contact());
                features.add(new ContentLengthOver1800());
                features.add(new Copyright());
                features.add(new Description());
                features.add(new Favicon());
                features.add(new Https());
                features.add(new Keyword());
                features.add(new KeywordInDomain());
                features.add(new KeywordInFirst100Words());
                features.add(new KeywordInImgAltTag());
                features.add(new KeywordInTitle());
                features.add(new Robots());
                features.add(new SocialMediaShare());
                features.add(new Viewport());
                features.add(new AlttagToImg());
                features.add(new ContentLengthToMax());
                features.add(new HdensityToMax());
                features.add(new ImgToMax());
                features.add(new IndexOfKeywordInTitle());
                features.add(new InOutlinkToAll());
                features.add(new UrlLength());
                features.add(new MetaTagToMax());
                features.add(new NoFollowToAll());
                features.add(new SimDescriptionH(type));
                features.add(new SimContentDescription(type));
                features.add(new SimKeywordDescription(type));
                features.add(new SimContentH(type));
                features.add(new SimKeywordH(type));
                features.add(new SimContentKeyword(type));
                features.add(new SimTitleDescription(type));
                features.add(new SimContentTitle(type));
                features.add(new SimTitleH(type));
                features.add(new SimTitleKeyword(type));
            }else{
                String[] parts = seopart.split("-");
                for(String part : parts){
                    if(part==null) continue;
                    if(part.isEmpty()) continue;
                    if("meta".equals(part)){
                        features.add(new MetaTagToMax());
                        features.add(new Copyright());
                        features.add(new Viewport());
                        features.add(new Robots());
                        features.add(new Description());
                        features.add(new Keyword());
                        features.add(new KeywordInDomain());
                        features.add(new KeywordInFirst100Words());
                        features.add(new KeywordInImgAltTag());
                        features.add(new KeywordInTitle());
                        features.add(new IndexOfKeywordInTitle());
                    }
                    if("content".equals(part)){
                        features.add(new ContentLengthToMax());
                        features.add(new ContentLengthOver1800());
                        features.add(new HdensityToMax());
                    }
                    if("link".equals(part)){
                        features.add(new Contact());
                        features.add(new Favicon());
                        features.add(new SocialMediaShare());
                        features.add(new ImgToMax());
                        features.add(new AlttagToImg());
                        features.add(new InOutlinkToAll());
                        features.add(new NoFollowToAll());
                    }
                    if("url".equals(part)){
                        features.add(new Https());
                    }
                    if("simcos".equals(part)){
                        features.add(new SimDescriptionH(type));
                        features.add(new SimContentDescription(type));
                        features.add(new SimKeywordDescription(type));
                        features.add(new SimContentH(type));
                        features.add(new SimKeywordH(type));
                        features.add(new SimContentKeyword(type));
                        features.add(new SimTitleDescription(type));
                        features.add(new SimContentTitle(type));
                        features.add(new SimTitleH(type));
                        features.add(new SimTitleKeyword(type));
                    }
                    if("simbert".equals(part)){
                        type="bert";
                        features.add(new SimDescriptionH(type));
                        features.add(new SimContentDescription(type));
                        features.add(new SimKeywordDescription(type));
                        features.add(new SimContentH(type));
                        features.add(new SimKeywordH(type));
                        features.add(new SimContentKeyword(type));
                        features.add(new SimTitleDescription(type));
                        features.add(new SimContentTitle(type));
                        features.add(new SimTitleH(type));
                        features.add(new SimTitleKeyword(type));
                        bert = Bert.load("com/robrua/nlp/easy-bert/bert-uncased-L-12-H-768-A-12");
                    }
                }
            }
        }else if(type.equals("doc")){
            features.add(new NumberOfChildPages(collection));
            features.add(new InLinkCount(collection));
            features.add(new PageRank(collection));
            features.add(new SpamScore(collection));

            features.add(new Entropy());
            features.add(new NumberOfSlashesInURL());
            features.add(new OutLinkCount());

            features.add(new AvgTermLength());
            features.add(new FracAnchorText());
            features.add(new FracTableText());
            features.add(new NoOfTitleTerms());
            features.add(new StopCover());
            features.add(new StopWordRatio());
            features.add(new TextToDocRatio());

            features.add(new URLWiki());
            features.add(new CDD());
        }

        Traverser traverser = new Traverser(dataset, docsPath, docIdSet, features, collectionStatistics, analyzerTag, searcher, reader, resultsettype, bert);
//        System.out.println("Average Doc Len = "+(double)collectionStatistics.sumTotalTermFreq()/collectionStatistics.docCount());

        final int numThreads = props.containsKey("numThreads") ? Integer.parseInt(props.getProperty("numThreads")) : Runtime.getRuntime().availableProcessors();
        System.out.println(numThreads + " threads are running.");
        traverser.traverseParallel(Paths.get(out), numThreads);
        //traverser.traverseWithThreads(Paths.get(out), numThreads);
        System.out.println("Document features are extracted in " + execution(start));

        for (IDocFeature feature : features)
            if (feature instanceof Closeable)
                ((Closeable) feature).close();
    }

    private Set<String> retrieveDocIdSet(Path file) throws IOException {

        Set<String> docIdSet = new HashSet<>();
        List<String> lines = Files.readAllLines(file);

        for (String line : lines) {

            if (line.startsWith("#")) continue;

            int i = line.indexOf("#");

            if (i == -1) {
                throw new RuntimeException("cannot find # in " + line);
            }

            String docId = line.substring(i + 1).trim();

            docIdSet.add(docId);
        }

        lines.clear();

        return docIdSet;
    }

    private Set<String> retrieveDocIdSetFromResultset(Path file) throws IOException {

        Set<String> docIdSet = new HashSet<>();
        List<String> lines = Files.readAllLines(file);

        for (String line : lines) {

            if (line.startsWith("#")) continue;


            String docId = Track.whiteSpaceSplitter.split(line)[2];

            docIdSet.add(docId);
        }

        lines.clear();

        return docIdSet;
    }
    private Set<String> retrieveDocIdSetFromExisting(Path file) throws IOException {

        Set<String> docIdSet = new HashSet<>();
        List<String> lines = Files.readAllLines(file);

        for (int i=0;i<lines.size();i++) {

            String line = lines.get(i);

            if (line.startsWith("#")) continue;


            String docId = Track.whiteSpaceSplitter.split(line)[0];

            docIdSet.add(docId);
        }

        System.out.println("Existing docs "+docIdSet.size());

        lines.clear();

        return docIdSet;
    }


    private Set<String> retrieveDocIdSetForLetor(Path file) throws IOException {

        Set<String> docIdSet = new HashSet<>();
        List<String> lines = Files.readAllLines(file);

        for (String line : lines) {

            if (line.startsWith("#")) continue;

            String docId = Track.whiteSpaceSplitter.split(line)[2];

            docIdSet.add(docId);
        }

        lines.clear();

        return docIdSet;
    }

    /**
     * Factory method for the instances of SolrAwareFeatureBase
     *
     * @param collection the collection in use
     * @param clazz      class of SolrAwareFeatureBase instances
     * @return HttpSolrClient
     */
    static HttpSolrClient solrClientFactory(Collection collection, Class<? extends SolrAwareFeatureBase> clazz) {

        if (Collection.CW09A.equals(collection) || Collection.CW09B.equals(collection) || Collection.MQ09.equals(collection) || Collection.MQE2.equals(collection)) {

            if (NumberOfChildPages.class.equals(clazz))
                return new HttpSolrClient.Builder().withBaseSolrUrl("http://irra-micro:8983/solr/url09").build();
            else if (PageRank.class.equals(clazz))
                return new HttpSolrClient.Builder().withBaseSolrUrl("http://irra-micro:8983/solr/rank09A").build();
            else if (InLinkCount.class.equals(clazz))
                return new HttpSolrClient.Builder().withBaseSolrUrl("http://irra-micro:8983/solr/anchor09A").build();
            else if (SpamScore.class.equals(clazz))
                return new HttpSolrClient.Builder().withBaseSolrUrl("http://irra-micro:8983/solr/spam09A").build();

        } else if (Collection.CW12A.equals(collection) || Collection.CW12B.equals(collection) || Collection.NTCIR.equals(collection)) {

            if (NumberOfChildPages.class.equals(clazz))
                return new HttpSolrClient.Builder().withBaseSolrUrl("http://irra-micro:8983/solr/url12").build();
            else if (PageRank.class.equals(clazz))
                return new HttpSolrClient.Builder().withBaseSolrUrl("http://irra-micro:8983/solr/rank12A").build();
            else if (InLinkCount.class.equals(clazz))
                return new HttpSolrClient.Builder().withBaseSolrUrl("http://irra-micro:8983/solr/anchor12A").build();
            else if (SpamScore.class.equals(clazz))
                return new HttpSolrClient.Builder().withBaseSolrUrl("http://irra-micro:8983/solr/spam12A").build();
        }

        throw new RuntimeException("The factory cannot find appropriate SolrClient for " + collection + " and " + clazz);
    }
}
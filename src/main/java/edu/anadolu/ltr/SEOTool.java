package edu.anadolu.ltr;

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
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.clueweb09.InfoNeed;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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
            docIdSet.addAll(Collection.GOV2.equals(collection) ? retrieveDocIdSetForLetor(path) : retrieveDocIdSet(path));
        }


        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);
        long start = System.nanoTime();


        ///////////////////////////// Index Reading for stats ///////////////////////////////////////////
        Path indexPath=null;
        if(this.tag == null)
            indexPath = Files.newDirectoryStream(dataset.indexesPath(), Files::isDirectory).iterator().next();
        else {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataset.indexesPath(), Files::isDirectory)) {
                for (Path path : stream) {
                    if(!tag.equals(path.getFileName().toString())) continue;
                    indexPath = path;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(indexPath == null)
            throw new RuntimeException(tag + " index not found");


        this.indexTag = indexPath.getFileName().toString();
        this.analyzerTag = Tag.tag(indexTag);

        this.reader = DirectoryReader.open(FSDirectory.open(indexPath));

        IndexSearcher searcher = new IndexSearcher(reader);
        CollectionStatistics collectionStatistics = searcher.collectionStatistics(Indexer.FIELD_CONTENTS);



        List<IDocFeature> features = new ArrayList<>();
        if(type.equals("seo")){
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
            features.add(new SimDescriptionH());
            features.add(new SimKeywordDescription());
            features.add(new SimKeywordH());
            features.add(new SimTitleDescription());
            features.add(new SimTitleH());
            features.add(new SimTitleKeyword());
            features.add(new SimContentDescription());
            features.add(new SimContentH());
            features.add(new SimContentKeyword());
            features.add(new SimContentTitle());
        }else if(type.equals("doc")){
            features.add(new NumberOfChildPages(collection));
            features.add(new InLinkCount(collection));
            features.add(new PageRank(collection));

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

        Traverser traverser = new Traverser(dataset, docsPath, docIdSet, features, collectionStatistics, analyzerTag, searcher, reader);

        final int numThreads = props.containsKey("numThreads") ? Integer.parseInt(props.getProperty("numThreads")) : Runtime.getRuntime().availableProcessors();
        traverser.traverseParallel(Paths.get(out), numThreads);
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


    private Set<String> retrieveDocIdSetForLetor(Path file) throws IOException {

        Set<String> docIdSet = new HashSet<>();
        List<String> lines = Files.readAllLines(file);

        for (String line : lines) {

            if (line.startsWith("#")) continue;

            int i = line.indexOf("GX");

            if (i == -1) {
                throw new RuntimeException("cannot find # in " + line);
            }

            String docId = line.substring(i, line.indexOf(" ", i)).trim();

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

        } else if (Collection.CW12A.equals(collection) || Collection.CW12B.equals(collection) || Collection.NTCIR.equals(collection)) {

            if (NumberOfChildPages.class.equals(clazz))
                return new HttpSolrClient.Builder().withBaseSolrUrl("http://irra-micro:8983/solr/url12").build();
            else if (PageRank.class.equals(clazz))
                return new HttpSolrClient.Builder().withBaseSolrUrl("http://irra-micro:8983/solr/rank12A").build();
            else if (InLinkCount.class.equals(clazz))
                return new HttpSolrClient.Builder().withBaseSolrUrl("http://irra-micro:8983/solr/anchor12A").build();
        }

        throw new RuntimeException("The factory cannot find appropriate SolrClient for " + collection + " and " + clazz);
    }
}
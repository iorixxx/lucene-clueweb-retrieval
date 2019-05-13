package edu.anadolu.ltr;

import edu.anadolu.cmdline.CLI;
import edu.anadolu.cmdline.CmdLineTool;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.clueweb09.Gov2Record;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Tool that compute SEO-based document features.
 */
public class SEOTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "Collection")
    private Collection collection;

    @Option(name = "-file", required = true, usage = "input file")
    private String file;

    @Option(name = "-out", required = true, usage = "output file")
    private String out;

    @Override
    public String getShortDescription() {
        return "Indexer Tool for ClueWeb09 ClueWeb12 Gov2 collections";
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

        final HttpSolrClient solr;

        if (Collection.CW09A.equals(collection) || Collection.CW09B.equals(collection) || Collection.MQ09.equals(collection) || Collection.MQE1.equals(collection)) {
            solr = new HttpSolrClient.Builder().withBaseSolrUrl("http://irra-micro.nas.ceng.local:8983/solr/anchor09A").build();
        } else if (Collection.CW12A.equals(collection) || Collection.CW12B.equals(collection))
            solr = new HttpSolrClient.Builder().withBaseSolrUrl("http://irra-micro.nas.ceng.local:8983/solr/anchor12A").build();
        else {
            System.out.println("anchor text is only available to ClueWeb09 and ClueWeb12 collections!");
            solr = null;
        }


        Set<String> docIdSet = new HashSet<String>();
        String[] tokens = this.file.split(",");
        for(String token : tokens){
            Path file = Paths.get(token.replaceAll("\\s+",""));
            if (!(Files.exists(file) && Files.isRegularFile(file))) {
                System.out.println(getHelp());
                return;
            }

             docIdSet.addAll(Collection.GOV2.equals(collection)?retrieveDocIdSetForLetor(file):retrieveDocIdSet(file));

        }


        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);
        long start = System.nanoTime();



        List<IDocFeature> features = new ArrayList<>();

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
        features.add(new InversedUrlLength());
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
        features.add(new StopWordRatio());
        features.add(new TextToDocRatio());

        Traverser traverser = new Traverser(dataset, docsPath, solr, docIdSet, features);

//        int numThread = Integer.parseInt(props.getProperty("numThreads", "2"));
        traverser.traverseParallel(Paths.get(out));
//        traverser.traverse(Paths.get(out),numThread);
        System.out.println("Document features are extracted in " + execution(start));
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

            String docId = line.substring(i,line.indexOf(" ",i)).trim();

            docIdSet.add(docId);
        }

        lines.clear();

        return docIdSet;
    }
}
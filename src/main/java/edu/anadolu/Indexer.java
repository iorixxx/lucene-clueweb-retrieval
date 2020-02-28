package edu.anadolu;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.field.MetaTag;
import edu.anadolu.field.SemanticElements;
import edu.anadolu.similarities.MetaTerm;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilter;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.clueweb09.ClueWeb09WarcRecord;
import org.clueweb09.ClueWeb12WarcRecord;
import org.clueweb09.Gov2Record;
import org.clueweb09.WarcRecord;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Element;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static edu.anadolu.analysis.Analyzers.scripts;
import static edu.anadolu.field.MetaTag.notEmpty;
import static org.apache.solr.common.params.CommonParams.HEADER_ECHO_PARAMS;
import static org.apache.solr.common.params.CommonParams.OMIT_HEADER;

/**
 * Indexer for ClueWeb{09|12} plus GOV2
 */
public class Indexer {

    public static final class NoPositionsTextField extends Field {

        static final FieldType TYPE_NOT_STORED = new FieldType();

        static {
            TYPE_NOT_STORED.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
            TYPE_NOT_STORED.setTokenized(true);
            TYPE_NOT_STORED.freeze();
        }

        public NoPositionsTextField(String name, String value) {
            super(name, value, TYPE_NOT_STORED);
        }
    }

    public static final String FIELD_CONTENTS = "contents";
    public static final String FIELD_ID = "id";
    public static final String RESPONSE = "response";

    public static final int BUFFER_SIZE = 1 << 16; // 64K

    private final class IndexerThread extends Thread {

        final private Path inputWarcFile;

        final private IndexWriter writer;

        IndexerThread(IndexWriter writer, Path inputWarcFile) {
            // super(inputWarcFile.getFileName().toString());
            this.writer = writer;
            this.inputWarcFile = inputWarcFile;
        }

        private int index(String id, String contents) throws IOException {

            // make a new, empty document
            Document document = new Document();

            // document ID
            document.add(new StringField(FIELD_ID, id, Field.Store.YES));

            // entire document
            document.add(new NoPositionsTextField(FIELD_CONTENTS, contents));

            // handle script flag
            if (!config.field && config.script) {

                for (String script : scripts)
                    document.add(new NoPositionsTextField(script, contents));

                document.add(new NoPositionsTextField("ascii", contents));
            }


            // URLs only
            // document.add(new NoPositionsTextField("url", contents));

            // EMails only
            // document.add(new NoPositionsTextField("email", contents));

            writer.addDocument(document);
            return 1;

        }

        private int indexJDoc(org.jsoup.nodes.Document jDoc, String id) throws IOException {

            String contents = jDoc.text();

            // don't index empty documents
            if (contents.length() == 0) {
                if (!config.silent)
                    System.err.println(id);
                return 1;
            }

            return index(id, contents);
        }

        private int indexJDocWithAnchor(org.jsoup.nodes.Document jDoc, String id) throws IOException {

            StringBuilder contents = new StringBuilder(jDoc.text()).append(" ");

            if (config.anchor && solr != null) {
                String anchor = anchor(id, solr);
                if (anchor != null)
                    stripHTMLAndAppend(anchor, contents);
            }

            // don't index empty documents
            if (contents.length() < 2) {
                if (!config.silent)
                    System.err.println(id);
                return 1;
            }
            return index(id, contents.toString().trim());
        }

        private int indexWarcRecord(WarcRecord warcRecord) throws IOException {
            // see if it's a response record
            if (!RESPONSE.equals(warcRecord.type()))
                return 0;

            String id = warcRecord.id();
            if (skip(id)) return 0;

            if (config.field) {
                Document document = warc2LuceneDocument(warcRecord);
                if (document != null)
                    writer.addDocument(document);

                return 1;
            } else if (config.semantic) {
                Document document = SemanticElements.warc2LuceneDocument(warcRecord);
                if (document == null)
                    return 1;

                writer.addDocument(document);

                return 1;
            }

            org.jsoup.nodes.Document jDoc;

            try {
                jDoc = Jsoup.parse(warcRecord.content());
            } catch (Exception exception) {
                if (!config.silent)
                    System.err.println("jdoc exception " + id);
                return 1;
            }

            if (config.anchor)
                return indexJDocWithAnchor(jDoc, id);
            else
                return indexJDoc(jDoc, id);

        }

        private int indexClueWeb12WarcFile() throws IOException {

            int i = 0;

            try (DataInputStream inStream = new DataInputStream(new GZIPInputStream(Files.newInputStream(inputWarcFile, StandardOpenOption.READ), BUFFER_SIZE))) {
                // iterate through our stream
                ClueWeb12WarcRecord wDoc;
                while ((wDoc = ClueWeb12WarcRecord.readNextWarcRecord(inStream)) != null) {
                    i += indexWarcRecord(wDoc);
                }
            }
            return i;
        }

        private int indexClueWeb09WarcFile() throws IOException {

            int i = 0;

            try (DataInputStream inStream = new DataInputStream(new GZIPInputStream(Files.newInputStream(inputWarcFile, StandardOpenOption.READ), BUFFER_SIZE))) {
                // iterate through our stream
                ClueWeb09WarcRecord wDoc;
                while ((wDoc = ClueWeb09WarcRecord.readNextWarcRecord(inStream)) != null) {
                    i += indexWarcRecord(wDoc);
                }
            }
            return i;
        }

        private int indexGov2File() throws IOException {

            int i = 0;

            StringBuilder builder = new StringBuilder();

            boolean found = false;

            try (
                    InputStream stream = new GZIPInputStream(Files.newInputStream(inputWarcFile, StandardOpenOption.READ), BUFFER_SIZE);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {


                for (; ; ) {
                    String line = reader.readLine();
                    if (line == null)
                        break;

                    line = line.trim();

                    if (line.startsWith(Gov2Record.DOC)) {
                        found = true;
                        continue;
                    }

                    if (line.startsWith(Gov2Record.TERMINATING_DOC)) {
                        found = false;
                        WarcRecord gov2 = Gov2Record.parseGov2Record(builder);
                        i += indexWarcRecord(gov2);
                        builder.setLength(0);
                    }

                    if (found)
                        builder.append(line).append(" ");
                }
            }

            return i;
        }

        @Override
        public void run() {
            try {

                Thread.currentThread().setName(inputWarcFile.toAbsolutePath().toString());
                //setName(inputWarcFile.getFileName().toString());

                if (Collection.CW09A.equals(collection) || Collection.CW09B.equals(collection)) {
                    int addCount = indexClueWeb09WarcFile();
                    //System.out.println("*./" + inputWarcFile.getParent().getFileName().toString() + File.separator + inputWarcFile.getFileName().toString() + "  " + addCount);
                } else if (Collection.CW12A.equals(collection) || Collection.CW12B.equals(collection)) {
                    int addCount = indexClueWeb12WarcFile();
                    //System.out.println("./" + inputWarcFile.getParent().getFileName().toString() + File.separator + inputWarcFile.getFileName().toString() + "\t" + addCount);
                } else if (Collection.GOV2.equals(collection)) {
                    int addCount = indexGov2File();
                    //System.out.println("./" + inputWarcFile.getParent().getFileName().toString() + File.separator + inputWarcFile.getFileName().toString() + "\t" + addCount);
                }

            } catch (IOException ioe) {
                System.out.println(Thread.currentThread().getName() + ": ERROR: unexpected IOException:");
                ioe.printStackTrace(System.out);
            }
        }
    }

    /**
     * Skip certain documents that hang JSoup.parse method.
     * Query relevance judgments of the ClueWeb12 dataset does not contain these documents, so skipping them is safe.
     * See open ticket : https://github.com/jhy/jsoup/issues/1192
     * Here are some binary files that hang JSoup.
     * <p>
     * <p>
     * clueweb12-1100wb-15-21376 http://csr.bu.edu/colortracking/data/test-sequences/sequence15.mv
     * clueweb12-1100wb-15-21381 http://csr.bu.edu/colortracking/data/test-sequences/sequence4.mv
     * clueweb12-1013wb-14-21356 http://www.geowall.org/data/3Dgeology/dem/helens.pfb
     * clueweb12-0200wb-38-08218 http://www.innovative-dsp.com/ftp/MIT/x6_400m_lx240t-ff1156.bit 9229029
     * clueweb12-0200wb-38-08219 http://www.innovative-dsp.com/ftp/MIT/x6_400m_sx315t-ff1156.bit 9975121
     *
     * @param docId document identifier
     * @return true if the document should be skipped
     */
    protected boolean skip(String docId) {
        return "clueweb12-1100wb-15-21376".equals(docId) || "clueweb12-1100wb-15-21381".equals(docId) || "clueweb12-1013wb-14-21356".equals(docId) || "clueweb12-0200wb-38-08218".equals(docId) || "clueweb12-0200wb-38-08219".equals(docId);
    }

    protected Path indexPath;
    private final Path docsPath;

    private final IndexerConfig config;
    private final Collection collection;

    public static String anchor(String id, SolrClient solr) {
        SolrQuery query = new SolrQuery();
        query.setQuery(id);
        query.setFields("anchor");
        query.set(HEADER_ECHO_PARAMS, CommonParams.EchoParamStyle.NONE.toString());
        query.set(OMIT_HEADER, true);
        QueryResponse response;

        try {
            response = solr.query(query);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        SolrDocumentList list = response.getResults();

        query.clear();
        if (list.size() == 0) return null;

        if (list.size() == 1)
            return (String) list.get(0).getFieldValue("anchor");
        else throw new RuntimeException(id + " returned " + list.size() + " many anchor texts");
    }

    private final SolrClient solr;

    private final Tag tag;
    private final DataSet dataset;

    public Indexer(DataSet dataset, String docsDir, String indexPath, HttpSolrClient solr, Tag tag, IndexerConfig config) throws IOException {

        this.dataset = dataset;
        this.collection = dataset.collection();
        this.config = config;
        boolean anchor = config.field || config.anchor;

        docsPath = Paths.get(docsDir);
        if (!Files.exists(docsPath) || !Files.isReadable(docsPath) || !Files.isDirectory(docsPath)) {
            System.out.println("Document directory '" + docsPath.toString() + "' does not exist or is not readable, please check the path");
            System.exit(1);
        }

        this.solr = solr;
        this.tag = tag;

        if (this.config.field)
            this.indexPath = Paths.get(indexPath, tag + "Field");
        else if (this.config.semantic)
            this.indexPath = Paths.get(indexPath, "Semantic");
        else
            this.indexPath = Paths.get(indexPath, tag + (anchor ? "Anchor" : ""));
        if (!Files.exists(this.indexPath))
            Files.createDirectories(this.indexPath);

    }

    private static StringBuilder stripHTMLAndAppend(String value, StringBuilder out) {

        try
                (
                        StringReader strReader = new StringReader(value);
                        HTMLStripCharFilter html = new HTMLStripCharFilter(strReader.markSupported() ? strReader : new BufferedReader(strReader))
                ) {

            char[] cbuf = new char[1024 * 10];
            while (true) {
                int count = html.read(cbuf);
                if (count == -1)
                    break; // end of stream mark is -1
                if (count > 0)
                    out.append(cbuf, 0, count);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed stripping HTML", e);
        }
        return out;
    }

    /**
     * Indexes different document representations (keywords, body, title, description, URL) into separate fields.
     *
     * @param wDoc ClueWeb09WarcRecord
     * @return Lucene Document having different fields (keywords, body, title, description)
     */
    protected Document warc2LuceneDocument(WarcRecord wDoc) {

        org.jsoup.nodes.Document jDoc;
        try {
            jDoc = Jsoup.parse(wDoc.content());
        } catch (Exception exception) {
            if (!config.silent)
                System.err.println(wDoc.id());
            return null;
        }


        // make a new, empty document
        Document document = new Document();

        String title = null;
        String body = null;


        document.add(new StringField("id", wDoc.id(), Field.Store.YES));


        // HTML <title> Tag
        Element titleEl = jDoc.getElementsByTag("title").first();
        if (titleEl != null) {
            title = StringUtil.normaliseWhitespace(titleEl.text()).trim();
            document.add(new NoPositionsTextField("title", title));
        }

        String keywords = MetaTag.enrich2(jDoc, "keywords");
        String description = MetaTag.enrich2(jDoc, "description");

        if (notEmpty.test(keywords))
            document.add(new NoPositionsTextField("keywords", keywords));

        if (notEmpty.test(description))
            document.add(new NoPositionsTextField("description", description));


        // HTML <body> Tag
        Element bodyEl = jDoc.body();
        if (bodyEl != null) {
            body = bodyEl.text();
            document.add(new NoPositionsTextField("body", body));
        }


        if (config.anchor && solr != null) {
            String anchor = anchor(wDoc.id(), solr);
            if (anchor != null) {
                StringBuilder builder = new StringBuilder();
                stripHTMLAndAppend(anchor, builder);
                document.add(new NoPositionsTextField("anchor", builder.toString().trim()));
            }
        }

        String metaNames = MetaTag.metaTagsWithNameAttribute(jDoc);

        if (notEmpty.test(metaNames))
            document.add(new NoPositionsTextField("meta", metaNames));

        document.add(new NoPositionsTextField("bt", body + " " + title));
        document.add(new NoPositionsTextField("btd", body + " " + title + " " + description));
        document.add(new NoPositionsTextField("btk", body + " " + title + " " + keywords));
        document.add(new NoPositionsTextField("btdk", body + " " + title + " " + description + " " + keywords));


        /*
         * Try to get useful parts of the URL
         * https://docs.oracle.com/javase/tutorial/networking/urls/urlInfo.html
         */

        final String URLString = Collection.GOV2.equals(collection) ? jDoc.baseUri() : wDoc.url();

        if (URLString != null && URLString.length() > 5) {

            String url;

            try {

                final URL aURL = new URL(URLString);

                url = (aURL.getHost() + " " + aURL.getFile()).trim();

                if (aURL.getRef() != null)
                    url += " " + aURL.getRef();

                document.add(new NoPositionsTextField("url", url));

                if (notEmpty.test(aURL.getHost()))
                    document.add(new NoPositionsTextField("host", aURL.getHost()));

            } catch (MalformedURLException me) {
                System.out.println("Malformed URL = " + URLString);
            }

        }

        return document;
    }


    public static Deque<Path> discoverWarcFiles(Path p, final String suffix) {

        final Deque<Path> stack = new ArrayDeque<>();

        FileVisitor<Path> fv = new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

                Path name = file.getFileName();
                if (name != null && name.toString().endsWith(suffix))
                    stack.add(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if ("OtherData".equals(dir.getFileName().toString())) {
                    System.out.printf("Skipping %s\n", dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException ioe) {
                System.out.printf("Visiting failed for %s\n", file);
                return FileVisitResult.SKIP_SUBTREE;
            }
        };

        try {
            Files.walkFileTree(p, fv);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stack;
    }

    private Analyzer analyzer() throws IOException {
        if (config.field) {
            Map<String, Analyzer> analyzerPerField = new HashMap<>();
            analyzerPerField.put("url", new SimpleAnalyzer());
            analyzerPerField.put("meta", MetaTag.whitespaceAnalyzer());
            analyzerPerField.put("host", MetaTag.whitespaceAnalyzer());
            return new PerFieldAnalyzerWrapper(Analyzers.analyzer(tag), analyzerPerField);
        } else if (config.semantic) {
            return MetaTag.whitespaceAnalyzer();
        } else
            return Analyzers.analyzer(tag);
    }

    public int indexWithThreads(int numThreads) throws IOException, InterruptedException {

        System.out.println("Indexing with " + numThreads + " threads to directory '" + indexPath.toAbsolutePath() + "'...");

        final Directory dir = FSDirectory.open(indexPath);

        final IndexWriterConfig iwc = new IndexWriterConfig(analyzer());

        iwc.setSimilarity(new MetaTerm());
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        iwc.setRAMBufferSizeMB(512.0);
        iwc.setUseCompoundFile(false);
        iwc.setMergeScheduler(new ConcurrentMergeScheduler());

        final IndexWriter writer = new IndexWriter(dir, iwc);

        final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);


        final String suffix = Collection.GOV2.equals(collection) ? ".gz" : ".warc.gz";
        final Deque<Path> warcFiles = discoverWarcFiles(docsPath, suffix);

        long totalWarcFiles = warcFiles.size();
        System.out.println(totalWarcFiles + " many " + suffix + " files found under the docs path : " + docsPath.toString());

        for (int i = 0; i < 2000; i++) {
            if (!warcFiles.isEmpty())
                executor.execute(new IndexerThread(writer, warcFiles.removeFirst()));
            else {
                if (!executor.isShutdown()) {
                    Thread.sleep(30000);
                    executor.shutdown();
                }
                break;
            }
        }


        long previous = 0;
        //add some delay to let some threads spawn by scheduler
        Thread.sleep(30000);


        try {
            // Wait for existing tasks to terminate
            while (!executor.awaitTermination(5, TimeUnit.MINUTES)) {

                System.out.print(String.format("%.2f percentage completed ", ((double) executor.getCompletedTaskCount() / totalWarcFiles) * 100.0d));
                System.out.println("activeCount = " + executor.getActiveCount() + " completed task = " + executor.getCompletedTaskCount() + " task count = " + executor.getTaskCount());

                final long completedTaskCount = executor.getCompletedTaskCount();

                if (!warcFiles.isEmpty())
                    for (long i = previous; i < completedTaskCount; i++) {
                        if (!warcFiles.isEmpty())
                            executor.execute(new IndexerThread(writer, warcFiles.removeFirst()));
                        else {
                            if (!executor.isShutdown())
                                executor.shutdown();
                        }
                    }

                previous = completedTaskCount;
                Thread.sleep(1000);
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            executor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }

        if (totalWarcFiles != executor.getCompletedTaskCount())
            throw new RuntimeException("totalWarcFiles = " + totalWarcFiles + " is not equal to completedTaskCount =  " + executor.getCompletedTaskCount());

        System.out.println("outside while pool size = " + executor.getPoolSize() + " activeCount = " + executor.getActiveCount() + " completed task = " + executor.getCompletedTaskCount() + " task count = " + executor.getTaskCount());

        int numIndexed = writer.getDocStats().maxDoc;

        try {
            writer.commit();
        } finally {
            writer.close();
            dir.close();
        }

        return numIndexed;
    }

    /**
     * Indexer based on Java8's parallel streams
     *
     * @return number of indexed documents
     * @throws IOException if IO exception occurs
     */
    public int indexParallel(int numThreads) throws IOException {

        System.out.println("Parallel Indexing to directory '" + indexPath.toAbsolutePath() + "'...");

        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "" + numThreads);

        final Directory dir = FSDirectory.open(indexPath);

        final IndexWriterConfig iwc = new IndexWriterConfig(analyzer());

        iwc.setSimilarity(new MetaTerm());
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        iwc.setRAMBufferSizeMB(512.0);
        iwc.setUseCompoundFile(false);
        iwc.setMergeScheduler(new ConcurrentMergeScheduler());

        final IndexWriter writer = new IndexWriter(dir, iwc);

        final String suffix = Collection.GOV2.equals(collection) ? ".gz" : ".warc.gz";

        try (Stream<Path> stream = Files.find(docsPath, 4, new WarcMatcher(suffix))) {

            stream.parallel().forEach(p -> {
                new IndexerThread(writer, p).run();
            });

        }

        int numIndexed = writer.getDocStats().maxDoc;

        try {
            writer.commit();
        } finally {
            writer.close();
            dir.close();
        }

        return numIndexed;
    }

    static final class WarcMatcher implements BiPredicate<Path, BasicFileAttributes> {

        private final String suffix;

        WarcMatcher(String suffix) {
            this.suffix = suffix;
        }

        @Override
        public boolean test(Path path, BasicFileAttributes basicFileAttributes) {

            if (path.toString().contains("OtherData")) return false;

            if (!basicFileAttributes.isRegularFile()) return false;

            Path name = path.getFileName();

            return (name != null && name.toString().endsWith(suffix));

        }
    }

    public static final class IndexerConfig {

        boolean anchor = false;
        boolean field = false;
        boolean script = false;
        boolean semantic = false;
        boolean silent = false;

        public IndexerConfig useAnchorText(boolean anchor) {
            this.anchor = anchor;
            return this;
        }

        public IndexerConfig useScripts(boolean script) {
            this.script = script;
            return this;
        }

        public IndexerConfig useMetaFields(boolean field) {
            this.field = field;
            return this;
        }

        public IndexerConfig useSemanticElements(boolean semantic) {
            this.semantic = semantic;
            return this;
        }

        public IndexerConfig useSilent(boolean silent) {
            this.silent = silent;
            return this;
        }
    }
}

package edu.anadolu;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.Collection;
import edu.anadolu.similarities.MetaTerm;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilter;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.*;
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
import org.clueweb09.ClueWeb09WarcRecord;
import org.clueweb09.ClueWeb12WarcRecord;
import org.clueweb09.Gov2Record;
import org.clueweb09.WarcRecord;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
import java.util.zip.GZIPInputStream;

/**
 * Indexer for ClueWeb{09|12} plus GOV2
 */
public final class Indexer {

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

        IndexerThread(IndexWriter writer, Path inputWarcFile) throws IOException {
            this.writer = writer;
            this.inputWarcFile = inputWarcFile;
            setName(inputWarcFile.getFileName().toString());
        }

        private int index(String id, String contents) throws IOException {

            // make a new, empty document
            Document document = new Document();

            // document ID
            document.add(new StringField(FIELD_ID, id, Field.Store.YES));

            // entire document
            document.add(new NoPositionsTextField(FIELD_CONTENTS, contents));

            writer.addDocument(document);
            return 1;

        }

        private int indexJDoc(org.jsoup.nodes.Document jDoc, String id) throws IOException {

            String contents = jDoc.text();

            // don't index empty documents
            if (contents.length() == 0) {
                System.err.println(id);
                return 1;
            }

            return index(id, contents);
        }

        private int indexJDocWithAnchor(org.jsoup.nodes.Document jDoc, String id) throws IOException {

            StringBuilder contents = new StringBuilder(jDoc.text()).append(" ");

            if (anchor && solr != null) {
                String anchor = anchor(id);
                if (anchor != null)
                    stripHTMLAndAppend(anchor, contents);
            }

            // don't index empty documents
            if (contents.length() < 2) {
                System.err.println(id);
                return 1;
            }
            return index(id, contents.toString().trim());
        }

        private int indexWarcRecord(WarcRecord warcRecord) throws IOException {
            // see if it's a response record
            if (!RESPONSE.equals(warcRecord.type()))
                return 0;

            if (field) {
                Document document = warc2LuceneDocument(warcRecord);
                if (document != null)
                    writer.addDocument(document);

                return 1;
            }

            String id = warcRecord.id();

            org.jsoup.nodes.Document jDoc;
            try {
                jDoc = Jsoup.parse(warcRecord.content());
            } catch (Exception exception) {
                // exception.printStackTrace();
                System.err.println(id);
                // System.out.println(warcRecord.content());
                return 1;
            }

            if (anchor)
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

                if (Collection.CW09A.equals(collection) || Collection.CW09B.equals(collection)) {
                    int addCount = indexClueWeb09WarcFile();
                    //System.out.println("*./" + inputWarcFile.getParent().getFileName().toString() + File.separator + inputWarcFile.getFileName().toString() + "  " + addCount);
                } else if (Collection.CW12B.equals(collection)) {
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

    private final Path indexPath;
    private final Path docsPath;

    private final boolean anchor;
    private final boolean field;
    private final Collection collection;

    private String anchor(String id) {
        SolrQuery query = new SolrQuery();
        query.setQuery(id);
        query.setFields("anchor");
        QueryResponse response;

        try {
            response = solr.query(query);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        SolrDocumentList list = response.getResults();

        if (list.size() == 0) return null;

        if (list.size() == 1)
            return (String) list.get(0).getFieldValue("anchor");
        else throw new RuntimeException(id + " returned " + list.size() + " many anchor texts");
    }

    private final SolrClient solr;

    private final Tag tag;

    public Indexer(Collection collection, String docsDir, String indexPath, HttpSolrClient solr, boolean anchor, Tag tag, boolean field) throws IOException {

        this.collection = collection;
        this.field = field;
        this.anchor = this.field || anchor;

        docsPath = Paths.get(docsDir);
        if (!Files.exists(docsPath) || !Files.isReadable(docsPath) || !Files.isDirectory(docsPath)) {
            System.out.println("Document directory '" + docsPath.toString() + "' does not exist or is not readable, please check the path");
            System.exit(1);
        }

        this.solr = solr;
        this.tag = tag;

        if (this.field)
            this.indexPath = Paths.get(indexPath, tag + "Field");
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

    private static void append(String content, StringBuilder builder) {
        if (content != null && content.trim().length() > 1)
            builder.append(content.trim()).append(" ");
    }

    /**
     * Enrich Lucene document with metadata extracted from JSoup document.
     * <p>
     * <head>
     * <meta charset="UTF-8">
     * <meta name="description" content="Free Web tutorials">
     * <meta name="keywords" content="HTML,CSS,XML,JavaScript">
     * <meta name="author" content="Hege Refsnes">
     * </head>
     *
     * @param meta      name of the metadata
     * @param jDoc      JSoup document
     * @param fieldName name of the field
     * @param document  Lucene document
     */
    private static void enrich(String meta, org.jsoup.nodes.Document jDoc, String fieldName, Document document) {

        Elements elements = jDoc.select("meta[name=" + meta + "]");
        elements.addAll(jDoc.select("meta[name=" + meta + "s]"));

        if (elements.isEmpty()) return;

        StringBuilder builder = new StringBuilder();

        for (Element e : elements) {
            append(e.attr("contents"), builder);
            append(e.attr("content"), builder);
        }

        if (builder.length() > 1)
            document.add(new TextField(fieldName, builder.toString().trim(), Field.Store.NO));

        elements.empty();
    }

    /**
     * Indexes different document representations (keywords, body, title, description, URL) into separate fields.
     *
     * @param wDoc ClueWeb09WarcRecord
     * @return Lucene Document having different fields (keywords, body, title, description)
     */
    private Document warc2LuceneDocument(WarcRecord wDoc) {

        org.jsoup.nodes.Document jDoc;
        try {
            jDoc = Jsoup.parse(wDoc.content());
        } catch (Exception exception) {
            System.err.println(wDoc.id());
            return null;
        }


        // make a new, empty document
        Document document = new Document();

        document.add(new StringField("id", wDoc.id(), Field.Store.YES));


        // HTML <title> Tag
        Element titleEl = jDoc.getElementsByTag("title").first();
        if (titleEl != null)
            document.add(new NoPositionsTextField("title", StringUtil.normaliseWhitespace(titleEl.text()).trim()));


        enrich("description", jDoc, "description", document);
        enrich("keyword", jDoc, "keywords", document);

        // HTML <body> Tag
        Element bodyEl = jDoc.body();
        if (bodyEl != null)
            document.add(new NoPositionsTextField("body", bodyEl.text()));


        if (anchor && solr != null) {
            String anchor = anchor(wDoc.id());
            if (anchor != null)
                document.add(new NoPositionsTextField("anchor", anchor));
        }


        /*
         * Try to get useful parts of the URL
         * https://docs.oracle.com/javase/tutorial/networking/urls/urlInfo.html
         */

        if (wDoc.url() != null && wDoc.url().length() > 5) {

            String url;

            try {
                URL aURL = new URL(wDoc.url());

                url = aURL.getHost() + " " + aURL.getFile();

                if (aURL.getRef() != null)
                    url += " " + aURL.getRef();

                document.add(new NoPositionsTextField("url", url));

            } catch (MalformedURLException me) {
                System.out.println("Malformed URL = " + wDoc.url());
            }

        }

        return document;
    }


    static Deque<Path> discoverWarcFiles(Path p, final String suffix) {

        final Deque<Path> stack = new ArrayDeque<>();

        FileVisitor<Path> fv = new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

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


    public int indexWithThreads(int numThreads) throws IOException, InterruptedException {

        System.out.println("Indexing with " + numThreads + " threads to directory '" + indexPath.toAbsolutePath() + "'...");

        final Directory dir = FSDirectory.open(indexPath);

        Map<String, Analyzer> analyzerPerField = new HashMap<>();
        analyzerPerField.put("url", new SimpleAnalyzer());

        PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(
                Analyzers.analyzer(tag), analyzerPerField);


        final IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

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

        int numIndexed = writer.maxDoc();

        try {
            writer.commit();
        } finally {
            writer.close();
        }

        dir.close();
        return numIndexed;
    }
}

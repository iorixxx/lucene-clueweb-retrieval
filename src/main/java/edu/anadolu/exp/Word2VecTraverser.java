package edu.anadolu.exp;

import edu.anadolu.Indexer;
import edu.anadolu.analysis.Analyzers;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.DataSet;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.clueweb09.ClueWeb09WarcRecord;
import org.clueweb09.ClueWeb12WarcRecord;
import org.clueweb09.Gov2Record;
import org.clueweb09.WarcRecord;
import org.jsoup.Jsoup;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static edu.anadolu.Indexer.BUFFER_SIZE;
import static edu.anadolu.analysis.Analyzers.FIELD;
import static edu.anadolu.analysis.Tag.KStem;

/**
 * Traverses for ClueWeb{09|12} plus GOV2
 */
public class Word2VecTraverser {

    private final class WorkerThread {

        private final Path inputWarcFile;
        private final AtomicReference<PrintWriter> out;

        WorkerThread(Path inputWarcFile, AtomicReference<PrintWriter> out) {
            this.inputWarcFile = inputWarcFile;
            this.out = out;
        }

        /**
         * This is the entry point for processing files in the data sets.
         *
         * @param wDoc input file
         * @return 1 in success
         */
        private int processWarcRecord(WarcRecord wDoc) {
            // see if it's a response record
            if (!Indexer.RESPONSE.equals(wDoc.type()))
                return 0;

            String id = wDoc.id();

            if (skip(id)) return 0;

            org.jsoup.nodes.Document jDoc;
            try {
                jDoc = Jsoup.parse(wDoc.content());
            } catch (Exception exception) {
                System.err.println(wDoc.id());
                return 0;
            }

            String contents = jDoc.text();

            // don't index empty documents
            if (contents.length() == 0) {
                System.err.println(wDoc.id());
                return 1;
            }

            // TODO do something with the content
            out.get().println(getAnalyzedTokens(contents, Analyzers.analyzer(KStem)));

            return 1;
        }

        private int processClueWeb12WarcFile() throws IOException {

            int i = 0;

            try (DataInputStream inStream = new DataInputStream(new GZIPInputStream(Files.newInputStream(inputWarcFile, StandardOpenOption.READ), BUFFER_SIZE))) {
                // iterate through our stream
                ClueWeb12WarcRecord wDoc;
                while ((wDoc = ClueWeb12WarcRecord.readNextWarcRecord(inStream)) != null) {
                    i += processWarcRecord(wDoc);
                }
            }
            return i;
        }

        private int processClueWeb09WarcFile() throws IOException {

            int i = 0;

            try (DataInputStream inStream = new DataInputStream(new GZIPInputStream(Files.newInputStream(inputWarcFile, StandardOpenOption.READ), BUFFER_SIZE))) {
                // iterate through our stream
                ClueWeb09WarcRecord wDoc;
                while ((wDoc = ClueWeb09WarcRecord.readNextWarcRecord(inStream)) != null) {
                    i += processWarcRecord(wDoc);
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
                        i += processWarcRecord(gov2);
                        builder.setLength(0);
                    }

                    if (found)
                        builder.append(line).append(" ");
                }
            }

            return i;
        }

        public void run() {
            try {

                Thread.currentThread().setName(inputWarcFile.toAbsolutePath().toString());

                if (Collection.CW09A.equals(collection) || Collection.CW09B.equals(collection)) {
                    int addCount = processClueWeb09WarcFile();
                    //System.out.println("*./" + inputWarcFile.getParent().getFileName().toString() + File.separator + inputWarcFile.getFileName().toString() + "  " + addCount);
                } else if (Collection.CW12A.equals(collection) || Collection.CW12B.equals(collection)) {
                    int addCount = processClueWeb12WarcFile();
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
     * Skip certain documents that hang Jsoup.parse method
     *
     * @param docId document identifier
     * @return true if the document should be skipped
     */
    protected boolean skip(String docId) {
        return "clueweb12-1100wb-15-21376".equals(docId) || "clueweb12-1100wb-15-21381".equals(docId) || "clueweb12-1013wb-14-21356".equals(docId) || "clueweb12-0200wb-38-08218".equals(docId) || "clueweb12-0200wb-38-08219".equals(docId);
    }

    private final Path docsPath;
    private final Collection collection;


    public Word2VecTraverser(DataSet dataset, String docsDir) {
        this.collection = dataset.collection();


        docsPath = Paths.get(docsDir);
        if (!Files.exists(docsPath) || !Files.isReadable(docsPath) || !Files.isDirectory(docsPath)) {
            System.out.println("Document directory '" + docsPath.toString() + "' does not exist or is not readable, please check the path");
            System.exit(1);
        }
    }


    /**
     * Traverse based on Java8's parallel streams
     */
    public void traverseParallel(Path resultPath, int numThreads) throws IOException {

        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "" + numThreads);

        final String suffix = Collection.GOV2.equals(collection) ? ".gz" : ".warc.gz";

        final AtomicReference<PrintWriter> out = new AtomicReference<>(new PrintWriter(Files.newBufferedWriter(resultPath, StandardCharsets.UTF_8)));

        try (Stream<Path> stream = Files.find(docsPath, 4, new WarcMatcher(suffix))) {

            stream.parallel().forEach(p -> new WorkerThread(p, out).run());
        }

        out.get().flush();
        out.get().close();
    }

    private final class WarcMatcher implements BiPredicate<Path, BasicFileAttributes> {

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

    public static String getAnalyzedTokens(String text, Analyzer analyzer) {

        final StringBuilder builder = new StringBuilder();
        try (TokenStream ts = analyzer.tokenStream(FIELD, new StringReader(text))) {

            final CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
            ts.reset(); // Resets this stream to the beginning. (Required)
            while (ts.incrementToken()) {
                builder.append(termAtt.toString());
                builder.append(' ');
            }

            ts.end();   // Perform end-of-stream operations, e.g. set the final offset.
        } catch (IOException ioe) {
            throw new RuntimeException("happened during string analysis", ioe);
        }
        return builder.toString().trim();
    }

}

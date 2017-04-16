package edu.anadolu;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.clueweb09.ClueWeb09WarcRecord;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import static edu.anadolu.Indexer.discoverWarcFiles;

/**
 * Extracts anchor text from the ClueWeb09 category B corpus
 */
public final class AnchorTextExtractor {

    private static void processAnchorWarcFile(Path anchorFile, HttpSolrClient solr) throws IOException, SolrServerException {

        //System.out.println("processing AnchorText warc file : " + anchorFile + " writing it to csv file : " + csvFile.toString());

        try (DataInputStream inStream = new DataInputStream(new GZIPInputStream(Files.newInputStream(anchorFile, StandardOpenOption.READ)))) {

            String line;

            while ((line = ClueWeb09WarcRecord.readLineFromInputStream(inStream)) != null) {

                if (!line.startsWith("DOCNO=")) continue;

                String docNo = line.substring(6);

                SolrInputDocument doc = new SolrInputDocument();
                doc.setField("id", docNo);

                String http = ClueWeb09WarcRecord.readLineFromInputStream(inStream);
                if (!http.startsWith("http")) {
                    System.out.println("http line does not start with http : " + http);
                    continue;
                }

                String linksLine = ClueWeb09WarcRecord.readLineFromInputStream(inStream).trim();
                if (!linksLine.startsWith("LINKS=")) {
                    System.out.println("LINKS line does not start with LINKS : " + linksLine);
                    continue;
                }

                int links = Integer.parseInt(linksLine.substring(6));

                StringBuilder builder = new StringBuilder();
                int count = 0;
                for (int i = 0; i < links; i++) {

                    String linkDocNo = ClueWeb09WarcRecord.readLineFromInputStream(inStream);
                    if (!linkDocNo.startsWith("LINKDOCNO=")) {
                        System.out.println("LINKDOCNO line does not start with LINKDOCNO= : " + linkDocNo);
                        continue;
                    }

                    String linkFromLine = ClueWeb09WarcRecord.readLineFromInputStream(inStream);
                    if (!linkFromLine.startsWith("LINKFROM=")) {
                        System.out.println("LINKFROM line does not start with LINKFROM= : " + linkFromLine);
                        continue;
                    }

                    String textLine = ClueWeb09WarcRecord.readLineFromInputStream(inStream);
                    if (!textLine.startsWith("TEXT=")) {
                        System.out.println("TEXT line does not start with TEXT= : " + textLine);
                        continue;
                    }

                    textLine = textLine.substring(5).trim();

                    if (textLine.isEmpty()) System.out.println(docNo);

                    // string leading and trailing quote from anchor text entry
                    if (textLine.startsWith("\"")) textLine = textLine.substring(1).trim();
                    if (textLine.endsWith("\"")) textLine = textLine.substring(0, textLine.length() - 1).trim();

                    if (textLine.isEmpty()) {
                        System.out.println(docNo);
                        continue;
                    }

                    builder.append(textLine).append("\t");
                    count++;
                }


                doc.setField("count", count);
                doc.setField("anchor", builder.toString().trim());
                solr.add(doc);
                builder.setLength(0);
            }

        }
    }

    public static void extractWithThreads(String anchorPath, int numThreads, HttpSolrClient solr) throws InterruptedException {

        Path docDir = Paths.get(anchorPath);
        if (!Files.exists(docDir) || !Files.isReadable(docDir) || !Files.isDirectory(docDir)) {
            System.out.println("Document directory '" + docDir.toString() + "' does not exist or is not readable, please check the path");
            System.exit(1);
        }

        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (final Path f : discoverWarcFiles(docDir, ".warc.gz"))
            executor.execute(new Thread(f.toString()) {
                @Override
                public void run() {
                    try {
                        processAnchorWarcFile(f, solr);
                    } catch (IOException ioe) {
                        System.out.println(Thread.currentThread().getName() + ": ERROR: unexpected IOException:");
                        ioe.printStackTrace();
                    } catch (SolrServerException sse) {
                        System.out.println(Thread.currentThread().getName() + ": ERROR: unexpected SolrServerException:");
                        sse.printStackTrace();
                    }

                }
            });


        //add some delay to let some threads spawn by scheduler
        Thread.sleep(30000);
        executor.shutdown(); // Disable new tasks from being submitted

        try {
            // Wait for existing tasks to terminate
            while (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            executor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
}

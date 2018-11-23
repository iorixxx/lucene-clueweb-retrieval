package org.clueweb09;

import edu.anadolu.Indexer;
import edu.anadolu.cmdline.CLI;
import edu.anadolu.cmdline.CmdLineTool;
import edu.anadolu.datasets.Collection;
import org.jsoup.Jsoup;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

/**
 * Test WarcRecord: Shows how to read, ID, URL, and Title.
 */
public class WarcTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "Collection")
    private edu.anadolu.datasets.Collection collection;

    @Option(name = "-doc", required = true, usage = "43.warc.gz")
    private String doc;

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    @Override
    public String getShortDescription() {
        return "Computes Document Length Statistics";
    }


    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        if (Collection.CW09A.equals(collection)) {
            if (this.doc.endsWith(".txt")) {
                for (String line : Files.readAllLines(Paths.get(doc)))
                    cw09(line);
            } else
                cw09(doc);

        } else if (Collection.CW12A.equals(collection))
            cw12();
        else if (Collection.GOV2.equals(collection))
            gov2();
        else
            System.out.println("this warc record test tool is not defined for the input collection: " + collection);


    }


    public void gov2() throws IOException {


        Path file = Paths.get(doc);
        StringBuilder builder = new StringBuilder();

        boolean found = false;

        try (
                InputStream stream = new GZIPInputStream(Files.newInputStream(file, StandardOpenOption.READ), Indexer.BUFFER_SIZE);
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
                    System.out.println(gov2.id());
                    System.out.println(gov2.content());

                    org.jsoup.nodes.Document jDoc;
                    try {
                        jDoc = Jsoup.parse(gov2.content());
                    } catch (java.lang.IllegalArgumentException iae) {
                        iae.printStackTrace();
                        System.err.println(gov2.id());
                        continue;
                    }

                    System.out.println(jDoc.text());

                    builder.setLength(0);
                }

                if (found)
                    builder.append(line).append(" ");
            }
        }
    }

    private void cw12() throws IOException {

        Path inputWarcFile = Paths.get(doc);

        int i = 0;

        try (DataInputStream inStream = new DataInputStream(new GZIPInputStream(Files.newInputStream(inputWarcFile, StandardOpenOption.READ)))) {

            // iterate through our stream
            ClueWeb12WarcRecord wDoc;
            while ((wDoc = ClueWeb12WarcRecord.readNextWarcRecord(inStream)) != null) {

                if ("response".equals(wDoc.getWARCType())) {


                    i++;
                    String id = wDoc.getDocid();

                    if ("clueweb12-1100wb-15-21381".equals(id) || "clueweb12-1013wb-14-21356".equals(id)) continue;

                    String url = wDoc.getURL();

                    String c = wDoc.getContent();

                    //  System.out.println(id + " " + url + " " + c.length());

                    System.out.println(id);


                    try {
                        Jsoup.parse(c);
                    } catch (Exception e) {
                        e.printStackTrace();

                    }

                }

            }
            //clueweb12-1100wb-15-21376 http://csr.bu.edu/colortracking/data/test-sequences/sequence15.mv
        }

        System.out.println(i + " many record found.");
    }

    private void cw09(String doc) throws IOException {


        try (DataInputStream inStream = new DataInputStream(new GZIPInputStream(Files.newInputStream(Paths.get(doc), StandardOpenOption.READ)))) {

            // iterate through our stream
            ClueWeb09WarcRecord wDoc;
            while ((wDoc = ClueWeb09WarcRecord.readNextWarcRecord(inStream)) != null) {
                // see if it's a response record
                if ("response".equals(wDoc.getHeaderRecordType())) {

                    String id = wDoc.getDocid();

                    String url = wDoc.getURL();

                    String c = wDoc.getContent();

                    // System.out.println(id + " " + url + " " + c.length());

                    System.out.println(id);

                    try {
                        Jsoup.parse(c);
                    } catch (Exception e) {
                        e.printStackTrace();

                    }

                }
            }
        }
    }

}

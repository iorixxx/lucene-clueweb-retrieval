package org.clueweb09;

import edu.anadolu.Indexer;
import org.jsoup.Jsoup;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.zip.GZIPInputStream;

/**
 * Test WarcRecord: Shows how to read, ID, URL, and Title.
 */
public class WarcRecordTest {


    public static void main(String[] args) throws IOException {

        //   gov2();
        cw12();
        //cw09();
    }


    public static void gov2() throws IOException {
        Path file = Paths.get("/Users/iorixxx/Desktop/00.gz");

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

    public static void cw12() throws IOException {

        Path inputWarcFile = Paths.get("/Users/iorixxx/1100wb-15.warc.gz");

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

                    System.out.println(id + " " + url + " " + c.length());


                    try {
                        Jsoup.parse(c);
                    } catch (Exception e) {

                    }

                }

            }
            //clueweb12-1100wb-15-21376 http://csr.bu.edu/colortracking/data/test-sequences/sequence15.mv
        }

        System.out.println(i + " many record found.");
    }

    public static void cw09() throws IOException {

        Path inputWarcFile = Paths.get("/Users/iorixxx/ClueWeb09_English_1/en0001/" + new Random().nextInt(100) + ".warc.gz");

        try (DataInputStream inStream = new DataInputStream(new GZIPInputStream(Files.newInputStream(inputWarcFile, StandardOpenOption.READ)))) {

            // iterate through our stream
            ClueWeb09WarcRecord wDoc;
            while ((wDoc = ClueWeb09WarcRecord.readNextWarcRecord(inStream)) != null) {
                System.out.println(wDoc.getHeaderRecordType());
                System.out.println(wDoc.getHeaderString());
                // see if it's a response record
                if ("response".equals(wDoc.getHeaderRecordType())) {

                    String id = wDoc.getDocid();

                    String url = wDoc.getURL();


                    String title = Jsoup.parse(wDoc.getContent()).title();

                    System.out.println(id + " " + url + " " + title);

                }
            }
        }
    }

}

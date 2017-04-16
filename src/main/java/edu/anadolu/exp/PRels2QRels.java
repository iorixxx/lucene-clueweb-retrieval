package edu.anadolu.exp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


/**
 * Relevance judgements prepared for catA runs are not compatible for catB runs
 */
final class PRels2QRels {

    private static final int CW09_DOC_ID_LENGTH = "clueweb09-en0003-55-31884".length();

    public static void main(String[] args) throws IOException {


        Path prels = Paths.get(args[0]);

        if (!Files.exists(prels) || !Files.isRegularFile(prels)) {
            System.out.println("cannot find prels file " + prels.toString());

            return;
        }

        convert(prels, args[1]);
        //trim2catB();
        //emptyIdList();
        System.out.println("END!");
    }

    /**
     * HTML documents are parsed with JSoup, empty documents' IDs are saved.
     * This class checks whether we are missing judged documents.
     *
     * @throws IOException
     */
    static void emptyIdList() throws IOException {


        List<String> emptyIds = Files.readAllLines(Paths.get("/Users/iorixxx/clueweb09/emptyIds.txt"), StandardCharsets.US_ASCII);

        String files[] = {"prels.catA.1-50", "prels-2.1-50", "prels.20001-60000"};

        for (String qRel : files)

            for (String line : Files.readAllLines(Paths.get("/Users/iorixxx/Downloads", qRel), StandardCharsets.US_ASCII)) {

                String[] parts = line.split("\\s+");

                for (String part : parts)
                    if (CW09_DOC_ID_LENGTH == part.length() && emptyIds.contains(part))
                        System.out.println(qRel + " " + line);


            }

        final String[] urls = {
                "http://trec.nist.gov/data/web/10/10.adhoc-qrels.final",
                "http://trec.nist.gov/data/web/11/qrels.adhoc",
                "http://trec.nist.gov/data/web/12/qrels.adhoc"
        };


        for (String urlStr : urls) {

            URL url = new URL(urlStr);

            System.out.println();

            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(url.openStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    String[] parts = inputLine.split("\\s+");

                    for (String part : parts)
                        if (CW09_DOC_ID_LENGTH == part.length() && emptyIds.contains(part))
                            System.out.println(url + " " + inputLine);

                }
            }

        }

    }


    /**
     * WT09: Convert prels.1-50 to 09.qrels.adhoc so that it can be used with gdeval.pl
     *
     * @throws IOException
     */
    public static void convert(Path prels, String qrels) throws IOException {

        PrintWriter output = new PrintWriter(Files.newBufferedWriter(prels.getParent().resolve(qrels), StandardCharsets.US_ASCII));

        for (String line : Files.readAllLines(prels, StandardCharsets.US_ASCII)) {

            String[] parts = line.split("\\s+");

            String queryID = parts[0];
            String docID = parts[1];
            int judge = Integer.parseInt(parts[2]);

            if (judge < 0 || judge > 2)
                throw new IllegalArgumentException("unexpected judge level for WT09[0, 1, 2] " + judge);

            output.println(queryID + " 0 " + docID + " " + judge);
        }

        output.flush();
        output.close();

    }
}

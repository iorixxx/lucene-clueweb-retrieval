package edu.anadolu.cmdline;

import org.kohsuke.args4j.Option;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;

import static org.clueweb09.tracks.Track.whiteSpaceSplitter;

public class WikiTool extends CmdLineTool {

    /**
     * http://www.lemurproject.org/clueweb12/ClueWeb12_All_edocid2url.txt.bz2
     * ClueWeb12 dataset (733,019,372 documents) Mapping of External Document Ids to URLs.
     * Each line represents one document in the format: <edocid> <document url> (delimited by one space).
     */
    @Option(name = "-f", required = false, usage = "Mapping of External Document Ids to URLs.")
    private String file = "/home/iorixxx/ClueWeb12_All_edocid2url.txt";

    @Option(name = "-o", required = false, usage = "Output file")
    private String output = "cw12_wiki.txt";

    @Override
    public String getShortDescription() {
        return "Prints document identifier of URL=wikipedia.org pages of the ClueWeb12 dataset";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home docCount termCount";
    }

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println("tfd.home is mandatory for query statistics!");
            return;
        }

        Path path = Paths.get(file);
        process(path, Paths.get(output));

    }

    private static void process(Path path, Path output) throws Exception {

        if (!(Files.isRegularFile(path) && Files.exists(path))) {
            System.out.println("Cannot read file " + path.toAbsolutePath().toString());
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path);
             PrintWriter out = new PrintWriter(Files.newBufferedWriter((output)))) {

            for (; ; ) {
                String line = reader.readLine();
                if (line == null)
                    break;
                String[] parts = whiteSpaceSplitter.split(line);

                String docId = parts[0];
                String url = parts[1];
                URL aURL = new URL(url);

                String host = aURL.getHost().toLowerCase(Locale.US).trim();
                if (host.contains("wikipedia.org")) {

                    if ("britainloveswikipedia.org".equals(host)) {
                        continue;
                    }

                    if (host.contains("-wiki")) {
                        //System.out.println(host); 
                        continue;
                    }
                    out.print(docId);
                    out.print(" ");
                    out.println(url);
                }
            }

        }
    }

    public static void main(String[] args) throws Exception {

        Path path = Paths.get("/Users/iorixxx/Downloads/ClueWeb12_All_edocid2url.txt");
        process(path, Paths.get("cw12_wiki.txt"));
    }

}

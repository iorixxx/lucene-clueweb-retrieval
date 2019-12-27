package edu.anadolu.qpp;

import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSetIterator;
import org.clueweb09.InfoNeed;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static edu.anadolu.analysis.Analyzers.getAnalyzedTokens;
import static java.nio.file.Files.readAllLines;

/**
 * Variability Score (VAR)
 */
public class VAR extends Base {

    public VAR (Path indexPath, String field) throws IOException {
        super(indexPath, field);
    }

    @Override
    public double value (String word) {
        throw new UnsupportedOperationException();
    }

    /**
     * The implementation is borrowed from the paper:
     * Zhao, Y., Scholer, F., & Tsegay, Y. (2008). Effective pre-retrieval query performance prediction using similarity and variability evidence.
     *
     * @return Variability score
     * @throws IOException
     */
    public double value (InfoNeed need) throws IOException {

        double varScore = 0.0;
        double wdt, fdt;

        // store required query stats in file
        String statsPath = indexPath.toString().substring(0, indexPath.toString().indexOf("indexes"));
        Path termStats = Paths.get(statsPath, "stats", "term_stats_for_typeq.csv");
        PrintWriter output;

        List<String> terms = getAnalyzedTokens(need.query(), analyzer);
        int validTerms = terms.size();

        if (!(Files.exists(termStats))) {
            output = new PrintWriter(Files.newBufferedWriter(termStats, StandardCharsets.US_ASCII));
            output.println("term\twdtSum\twdtSquareSum");
            output.flush();
            output.close();
        }

        for (String term : terms) {
            double wdtSum = 0.0, wdtSquareSum = 0.0;
            double variance;

            if (df(field, term) == 0) {
                System.out.println("Term " + term + " is missing in vocabulary");
                validTerms--;
                continue;
            }

            Term t = new Term(field, term);
            PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(searcher.getIndexReader(), field, t.bytes());

            if (postingsEnum == null) {
                System.out.println("Cannot find the term " + term + " in the field " + field);
                continue;
            }

            List<String> lines = readAllLines(termStats);
            boolean termFound = false;
            for (String line : lines) {
                if ("term\twdtSum\twdtSquareSum".equals(line)) continue;
                String[] parts = line.split("\\s+");
                if (parts.length != 3)
                    throw new RuntimeException("term_stats_for_typeq.csv does not have 3 parts: " + line);

                if (term.equals(parts[0])) {
                    wdtSum = Double.parseDouble(parts[1]);
                    wdtSquareSum = Double.parseDouble(parts[2]);
                    termFound = true;
                    break;
                }
            }

            if (!termFound) {
                while (postingsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                    fdt = postingsEnum.freq();
                    wdt = 1 + Math.log(fdt) * Math.log(1 + docCount / df(field, term));
                    wdtSum += wdt;
                    wdtSquareSum += Math.pow(wdt, 2);
                }

                output = new PrintWriter(Files.newBufferedWriter(termStats, StandardCharsets.US_ASCII, StandardOpenOption.APPEND));
                output.println(term + "\t" + wdtSum + "\t" + wdtSquareSum);
                output.flush();
                output.close();
            }

            // simplified version of variance
            variance = wdtSquareSum - ((Math.pow(wdtSum, 2)) / df(field, term));
            varScore += Math.sqrt((1.0 / df(field, term)) * variance);
        }

        // normalize score by the number of valid query terms
        return validTerms == 0 ? 0 : varScore / validTerms;
    }

    public String toString() {
        return "VAR";
    }
}

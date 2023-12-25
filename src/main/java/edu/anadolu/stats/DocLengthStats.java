package edu.anadolu.stats;

import org.apache.lucene.index.*;
import org.apache.lucene.store.FSDirectory;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Computes Document Length Statistics
 */
public final class DocLengthStats implements Closeable {

    private final String tag;
    private final IndexReader reader;
    private final Path collectionPath;
    private final NumericDocValues norms;
    private final String field;

    public DocLengthStats(Path collectionPath, Path indexPath, String field) throws IOException {
        this.reader = DirectoryReader.open(FSDirectory.open(indexPath));
        this.collectionPath = collectionPath;
        this.tag = indexPath.getFileName().toString();
        this.norms = MultiDocValues.getNormValues(reader, field);
        this.field = field;
    }

    public void process(Set<String> words) throws IOException {

        PrintWriter out = new PrintWriter(
                Files.newBufferedWriter(Paths.get(collectionPath.toString(), "stats", tag, field + "_document_length_stats.csv"))
        );

        out.println("Term\tN\tdocLenAcc\tdocLenSquareAcc");

        for (String word : words) {
            out.println(process(word));
            out.flush();

        }
        out.close();
    }

    public String process(String word) throws IOException {

        Term term = new Term(field, word);
        PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader, field, term.bytes());

        if (postingsEnum == null)
            return (word + "(stopword)");

        long docLenAcc = 0;
        long docLenSquareAcc = 0;
        long counter = 0;

        while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {
            final long docLen;

            if (norms.advanceExact(postingsEnum.docID())) {
                docLen = norms.longValue();
            } else {
                docLen = 0;
            }

            docLenAcc += docLen;
            docLenSquareAcc += (docLen * docLen);
            counter++;
        }

        return word + "\t" + counter + "\t" + docLenAcc + "\t" + docLenSquareAcc;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}

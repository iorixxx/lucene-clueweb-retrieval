package edu.anadolu.mc;

import edu.anadolu.cmdline.CmdLineTool;
import edu.anadolu.similarities.MetaTerm;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Indexer for Milliyet Collection
 */
public class MCIndexer {


    final Path indexPath;

    public MCIndexer(Path indexPath) throws IOException {

//        this.mcCSV = Paths.get(mcCSV);
//        if (!Files.exists(this.mcCSV) || !Files.isReadable(this.mcCSV) || !Files.isRegularFile(this.mcCSV)) {
//            System.out.println("MC CSV file '" + this.mcCSV.toString() + "' does not exist or is not readable, please check the path");
//            System.exit(1);
//        }

        this.indexPath = indexPath;
        if (!Files.exists(this.indexPath))
            Files.createDirectories(this.indexPath);
    }

    /**
     * TurkishAnalyzer: Filters {@link org.apache.lucene.analysis.standard.ClassicTokenizer} with {@link org.apache.lucene.analysis.standard.ClassicFilter},
     * {@link org.apache.lucene.analysis.tr.ApostropheFilter} and {@link org.apache.lucene.analysis.tr.TurkishLowerCaseFilter}.
     *
     * @return KStemAnalyzer
     * @throws IOException
     */
    public static Analyzer analyzer() throws IOException {
        return CustomAnalyzer.builder()
                .withTokenizer("classic")
                .addTokenFilter("classic")
                .addTokenFilter("apostrophe")
                .addTokenFilter("turkishlowercase")
                .build();
    }

    public int index() throws Exception {

        System.out.println("Indexing to directory '" + indexPath.toAbsolutePath() + "'...");

        final Directory dir = FSDirectory.open(indexPath);

        final IndexWriterConfig iwc = new IndexWriterConfig(analyzer());

        iwc.setSimilarity(new MetaTerm());
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        iwc.setRAMBufferSizeMB(256.0);
        iwc.setUseCompoundFile(false);
        iwc.setMergeScheduler(new ConcurrentMergeScheduler());

        final IndexWriter writer = new IndexWriter(dir, iwc);

        Class.forName("com.mysql.jdbc.Driver");
        final Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/milliyetcollection", "root", "");

        Statement statement = conn.createStatement();

        ResultSet rs = statement.executeQuery("SELECT docno, headline, text from documents");

        while (rs.next()) {

            int id = rs.getInt(1);

            // System.out.println(id + "\t" + rs.getString(2));
            Document document = new Document();

            document.add(new NumericDocValuesField("id", id));
            document.add(new StringField("id", Integer.toString(id), Field.Store.YES));

            document.add(new TextField("contents", rs.getString(2), Field.Store.NO));
            document.add(new TextField("contents", rs.getString(3), Field.Store.NO));

            writer.addDocument(document);
        }

        rs.close();
        statement.close();

        int numIndexed = writer.maxDoc();
        try {
            writer.commit();
            writer.forceMerge(1);
        } finally {
            writer.close();
        }

        dir.close();
        conn.close();
        return numIndexed;
    }

    public static void main(String[] args) throws Exception {

        String tfd_home = "/Users/iorixxx/TFD_HOME";
        String data_set = "MC";
        String tag = "NS";

        long start = System.nanoTime();
        MCIndexer indexer = new MCIndexer(Paths.get(tfd_home, data_set, "indexes", tag));
        int numIndexed = indexer.index();
        System.out.println("Total " + numIndexed + " documents indexed in " + CmdLineTool.execution(start));
    }
}

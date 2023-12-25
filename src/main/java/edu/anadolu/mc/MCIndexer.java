package edu.anadolu.mc;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.similarities.BM25c;
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
import java.sql.*;

/**
 * Indexer for Milliyet Collection
 */
public class MCIndexer {

    public static int index(String dataDir, final String indexPath, Tag tag) throws IOException, ClassNotFoundException, SQLException {

        Class.forName("com.mysql.cj.jdbc.Driver");
        Path iPath = Paths.get(indexPath, tag.toString());

        if (!Files.exists(iPath))
            Files.createDirectories(iPath);

        System.out.println("Indexing to directory '" + iPath.toAbsolutePath() + "'...");

        final Directory dir = FSDirectory.open(iPath);

        final IndexWriterConfig iwc = new IndexWriterConfig(Analyzers.analyzer(tag));
        iwc.setSimilarity(new BM25c(1.2, 0.75));
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        iwc.setRAMBufferSizeMB(256.0);
        iwc.setUseCompoundFile(false);
        iwc.setMergeScheduler(new ConcurrentMergeScheduler());

        final IndexWriter writer = new IndexWriter(dir, iwc);
        final Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/mc?serverTimezone=UTC", "root", "");
        Statement statement = conn.createStatement();
        ResultSet rs = statement.executeQuery("SELECT docno, headline, text from documents");

        while (rs.next()) {
            int id = rs.getInt(1);
            Document document = new Document();

            document.add(new StringField("id", "Milliyet_0105_v00_" + id, Field.Store.YES));
            document.add(new TextField("contents", rs.getString(2) + " " + rs.getString(3), Field.Store.NO));

            writer.addDocument(document);
        }

        rs.close();
        statement.close();

        int numIndexed = writer.getDocStats().maxDoc;
        try {
            writer.commit();
            writer.forceMerge(1);
        } finally {
            writer.close();
            dir.close();
            conn.close();
        }
        return numIndexed;
    }
}

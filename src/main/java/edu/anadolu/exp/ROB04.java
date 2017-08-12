package edu.anadolu.exp;

import edu.anadolu.Indexer;
import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.similarities.MetaTerm;
import org.apache.lucene.benchmark.byTask.feeds.DocData;
import org.apache.lucene.benchmark.byTask.feeds.NoMoreDataException;
import org.apache.lucene.benchmark.byTask.feeds.TrecContentSource;
import org.apache.lucene.benchmark.byTask.feeds.TrecParserByPath;
import org.apache.lucene.benchmark.byTask.utils.Config;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Helper class for Robust Track 2004
 * http://trec.nist.gov/data/robust/04.guidelines.html
 */
public class ROB04 {

    public static int index(String dataDir, final String indexPath, Tag tag) throws IOException {

        Path iPath = Paths.get(indexPath, tag.toString());

        if (!Files.exists(iPath))
            Files.createDirectories(iPath);

        System.out.println("Indexing to directory '" + iPath.toAbsolutePath() + "'...");

        final Directory dir = FSDirectory.open(iPath);

        final IndexWriterConfig iwc = new IndexWriterConfig(Analyzers.analyzer(tag));

        iwc.setSimilarity(new MetaTerm());
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        iwc.setRAMBufferSizeMB(512.0);
        iwc.setUseCompoundFile(false);
        iwc.setMergeScheduler(new ConcurrentMergeScheduler());

        final IndexWriter writer = new IndexWriter(dir, iwc);

        TrecContentSource tcs = new TrecContentSource();
        Properties props = new Properties();
        props.setProperty("print.props", "false");
        props.setProperty("content.source.verbose", "false");
        props.setProperty("content.source.excludeIteration", "true");
        props.setProperty("docs.dir", dataDir);
        props.setProperty("trec.doc.parser", TrecParserByPath.class.getName());
        props.setProperty("content.source.forever", "false");
        tcs.setConfig(new Config(props));

        tcs.resetInputs();

        DocData dd = new DocData();
        while (true) {

            try {
                dd = tcs.getNextDocData(dd);
            } catch (NoMoreDataException no) {
                break;
            }


            String id = dd.getName();
            String body = dd.getBody();
            String title = dd.getTitle();

            StringBuilder contents = new StringBuilder();

            if (title != null && title.trim().length() > 0)
                contents.append(title.trim()).append(' ');

            if (body != null && body.trim().length() > 0) {
                contents.append(body.trim());
            }

            // don't index empty documents
            if (contents.length() == 0) {
                System.err.println(id + " " + dd.getTitle() + " " + dd.getBody());
                continue;
            }

            // make a new, empty document
            Document document = new Document();

            // document ID
            document.add(new StringField(Indexer.FIELD_ID, id, Field.Store.YES));

            // entire document
            document.add(new Indexer.NoPositionsTextField(Indexer.FIELD_CONTENTS, contents.toString().trim()));

            // add artificial: every document should have this
            document.add(Indexer.ARTIFICIAL);

            writer.addDocument(document);

        }

        tcs.close();

        final int numIndexed = writer.maxDoc();

        try {
            writer.commit();
            writer.forceMerge(1);
        } finally {
            writer.close();
        }

        dir.close();

        return numIndexed;
    }
}

package edu.anadolu;

import edu.anadolu.analysis.Tag;
import edu.anadolu.cmdline.CLI;
import edu.anadolu.cmdline.SearcherTool;
import edu.anadolu.datasets.Collection;
import org.clueweb09.tracks.MC;
import org.junit.Test;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * Created by GKHN on 14.07.2017.
 */
public class SearcherTest {



    @Test
    public void SearcherToolMCTest() throws Exception {
        String[] args ={"Searcher","-collection","MC","-task","param"};
        CLI.main(args);
    }
}

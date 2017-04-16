package edu.anadolu.cmdline;

import edu.anadolu.exp.MySQL;
import edu.anadolu.freq.L2;
import edu.anadolu.freq.TFNormalization;

import java.util.Properties;

/**
 * MySQL Tool
 */
public final class MySQLTool extends CmdLineTool {

    @Override
    public String getShortDescription() {
        return "MySQL Tool";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    @Override
    public void run(Properties props) throws Exception {


        final String home = props.getProperty("tfd.home");

        if (home == null) {
            System.out.println(getHelp());
            return;
        }

        TFNormalization[] normalizations = {new L2()};

        for (TFNormalization normalization : normalizations)
            MySQL.dump(home, "KStemAnalyzer", normalization);
    }
}

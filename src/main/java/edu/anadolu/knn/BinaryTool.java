package edu.anadolu.knn;

import edu.anadolu.datasets.Collection;
import org.kohsuke.args4j.Option;
import org.paukov.combinatorics3.Generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;

/**
 * Binary Tool
 */
public class BinaryTool extends XTool {

    @Option(name = "-task", required = false, usage = "task to be executed")
    private String task;

    @Override
    public void run(Properties props) throws Exception {

        if (null == task) {
            super.run(props);
            return;
        }

        if (parseArguments(props) == -1) return;

        tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }
    }

    @Override
    protected Path excelFile() throws IOException {

        Path excelPath = Paths.get(tfd_home, test.toString()).resolve("binary");

        if (!Files.exists(excelPath))
            Files.createDirectories(excelPath);

        return excelPath.resolve(models + "X" + test.toString() + report.toString() + tag + train.toString() + optimize.toString() + op.toUpperCase(Locale.ENGLISH) + ".xlsx");
    }

    @Override
    public String getShortDescription() {
        return "Binary Tool";
    }


    public static void main(String[] args) {

        Generator.combination(ColorUtil.colorMap().keySet())
                .simple(2)
                .stream()
                .filter(l -> !l.get(0).equals(l.get(1)))
                .forEach(combination -> {

                    String m1 = combination.get(0);
                    String m2 = combination.get(1);
                    System.out.print(m1 + "_" + m2 + " ");

                });


        System.out.println();
        for (Measure measure : Measure.values())
            System.out.print(measure + " ");

    }
}



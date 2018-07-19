package edu.anadolu.cmdline;

import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.exp.FullFactorial;
import edu.anadolu.knn.XTool;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.clueweb09.InfoNeed;
import org.paukov.combinatorics3.Generator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * multi-winner tally
 */
public class TallyTool extends XTool {

    @Override
    public String getShortDescription() {
        return "TallyTool";
    }


    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        Path path = Paths.get(tfd_home, "Tally.xlsx");

        XSSFWorkbook workbook = new XSSFWorkbook();


        for (FullFactorial.EXP exp : FullFactorial.experiments("Anchor")) {

            Map<String, Integer> affairs = new HashMap<>();

            DataSet dataset = CollectionFactory.dataset(exp.collection, tfd_home);

            final String evalDirectory = evalDirectory(dataset, report);

            System.out.println(exp.collection + " Instantiating Evaluator with evaluation directory of " + evalDirectory);

            Evaluator evaluator = new Evaluator(dataset, tag, report, "all", evalDirectory, op);

            Set<String> modelSet = evaluator.getModelSet();

            Map<InfoNeed, Set<String>> map = evaluator.multiLabelMap(1.0);

            for (Set<String> set : map.values()) {

                if (set.size() < 2) continue;

                Generator.combination(set)
                        .simple(2)
                        .stream()
                        .forEach(combination -> {

                            String m1 = combination.get(0);
                            String m2 = combination.get(1);

                            incrementAffairs(affairs, m1 + "_" + m2);
                            incrementAffairs(affairs, m2 + "_" + m1);

                        });
            }

            Sheet sheet = workbook.createSheet(exp.collection.toString());

            writeModel2ModelMatrixHeaders(sheet, modelSet, affairs);

        }

        workbook.write(Files.newOutputStream(path));
        workbook.close();

    }

}

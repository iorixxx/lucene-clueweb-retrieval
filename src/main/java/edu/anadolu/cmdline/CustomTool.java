package edu.anadolu.cmdline;

import edu.anadolu.Searcher;
import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.knn.Measure;
import edu.anadolu.similarities.DFIC;
import edu.anadolu.similarities.DFRee;
import edu.anadolu.similarities.DLH13;
import edu.anadolu.similarities.DPH;
import org.apache.lucene.search.similarities.ModelBase;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.clueweb09.InfoNeed;
import org.kohsuke.args4j.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class CustomTool extends CmdLineTool {

    @Option(name = "-tag", metaVar = "[KStem|KStemAnchor]", required = false, usage = "Index Tag")
    protected Tag tag;

    /**
     * Terrier's default values
     */
    @Option(name = "-models", required = false, usage = "term-weighting models")
    protected String models = "BM25k1.2b0.75_DirichletLMc2500.0_LGDc1.0_PL2c1.0";

    @Option(name = "-metric", required = false, usage = "Effectiveness measure")
    protected Measure measure = Measure.NDCG20;

    @Option(name = "-collection", required = true, usage = "Collection")
    protected edu.anadolu.datasets.Collection collection;

    @Option(name = "-task", required = false, usage = "task to be executed: search or eval")
    private String task;

    @Override
    public String getShortDescription() {
        return "Searcher Tool in which you supply values of the hyper parameters";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " paths.indexes tfd.home";
    }


    @Override
    public void run(Properties props) throws Exception {

        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);

        final int numThreads = Integer.parseInt(props.getProperty("numThreads", "2"));

        if ("search".equals(task)) {

            final long start = System.nanoTime();

            final Set<ModelBase> modelBaseSet = Arrays.stream(models.split("_"))
                    .map(ParamTool::string2model)
                    .collect(Collectors.toSet());

            modelBaseSet.add(new DFIC());
            modelBaseSet.add(new DPH());
            modelBaseSet.add(new DLH13());
            modelBaseSet.add(new DFRee());

            if (!props.containsKey(collection.toString() + ".fields"))
                throw new RuntimeException("cannot find " + collection.toString() + ".fields property!");

            final String[] fieldsArr = props.getProperty(collection.toString() + ".fields").split(",");

            List<String> fields = Arrays.asList(fieldsArr);
            for (final Path path : discoverIndexes(dataset)) {

                final String tag = path.getFileName().toString();

                // search for a specific tag, skip the rest
                if (this.tag != null && !tag.equals(this.tag)) continue;

                try (Searcher searcher = new Searcher(path, dataset, 1000)) {
                    searcher.searchWithThreads(numThreads, modelBaseSet, /*Collections.singletonList("contents")*/ fields, "runs");
                }
            }
            System.out.println("Search completed in " + execution(start));
        } else if ("eval".equals(task)) {
            if (tag == null) tag = Tag.KStem;
            Evaluator evaluator = new Evaluator(dataset, tag.toString(), measure, models + "_DFIC_DPH_DFRee_DLH13", "evals", "OR");
            evaluator.printMeanWT();
            evaluator.printMean();
            System.out.println("=======================");
        } else if ("export".equals(task)) {

            Path excelPath = Paths.get(tfd_home, collection.toString()).resolve("excels");
            if (!Files.exists(excelPath))
                Files.createDirectories(excelPath);

            Path excelFile = excelPath.resolve(collection + ".xlsx");

            Workbook workbook = new XSSFWorkbook();

            for (final Path path : discoverIndexes(dataset)) {

                final String tag = path.getFileName().toString();

                for (Measure measure : new Measure[]{Measure.NDCG20, Measure.NDCG100, Measure.MAP}) {

                    Evaluator evaluator = new Evaluator(dataset, tag, measure, models + "_DFIC_DPH_DFRee_DLH13", "evals", "OR");


                    Sheet statSheet = workbook.createSheet(tag + measure);

                    Row r0 = statSheet.createRow(0);

                    r0.createCell(0).setCellValue("qID");
                    int i = 1;
                    for (String model : new TreeSet<>(evaluator.getModelSet()))
                        r0.createCell(i++).setCellValue(Evaluator.prettyModel(model));

                    int row = 1;
                    for (InfoNeed need : evaluator.getNeeds()) {

                        Row r = statSheet.createRow(row);
                        r.createCell(0).setCellValue(need.id());

                        i = 1;
                        for (String model : new TreeSet<>(evaluator.getModelSet()))
                            r.createCell(i++).setCellValue(evaluator.score(need, model));

                        row++;
                    }
                }
            }

            workbook.write(Files.newOutputStream(excelFile));
            workbook.close();
            System.out.println(collection + "'s run-topic matrix is saved into : " + excelFile.toAbsolutePath());

        } else if ("spam".equals(task)) {

            final long start = System.nanoTime();

            final Set<ModelBase> modelBaseSet = Arrays.stream(models.split("_"))
                    .map(ParamTool::string2model)
                    .collect(Collectors.toSet());

            modelBaseSet.add(new DFIC());
            modelBaseSet.add(new DPH());
            modelBaseSet.add(new DLH13());
            modelBaseSet.add(new DFRee());

            if (!props.containsKey(collection.toString() + ".fields"))
                throw new RuntimeException("cannot find " + collection.toString() + ".fields property!");

            final String[] fieldsArr = props.getProperty(collection.toString() + ".fields").split(",");

            List<String> fields = Arrays.asList(fieldsArr);
            if (fields.size() == 0) fields.add(props.getProperty(collection.toString() + ".fields"));

            if (dataset.spamAvailable()) {
                for (final Path path : discoverIndexes(dataset)) {

                    final String tag = path.getFileName().toString();

                    // search for a specific tag, skip the rest
                    if (this.tag != null && !tag.equals(this.tag)) continue;

                    try (Searcher searcher = new Searcher(path, dataset, 10000)) {
                        searcher.searchWithThreads(numThreads, modelBaseSet, fields, "base_spam_runs");
                    }
                    modelBaseSet.clear();
                }

                System.out.println("Base search for spam filtering 10,000 documents per query completed in " + execution(start));
            }
        }

    }
}

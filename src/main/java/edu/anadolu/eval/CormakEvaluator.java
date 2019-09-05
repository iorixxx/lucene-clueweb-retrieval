package edu.anadolu.eval;

import edu.anadolu.cmdline.CormakTool;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.knn.Measure;
import org.clueweb09.InfoNeed;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CormakEvaluator extends Evaluator {

    private String[] catARuns = new String[]{
            "input.MSRAAF",
            "input.MSRAC",
            "input.MSRANORM",
            "input.MS1",
            "input.MS2",
            "input.pkuLink",
            "input.pkuSewmTp",
            "input.pkuStruct",
            "input.Sab9wtBase",
            "input.Sab9wtBf1",
            "input.Sab9wtBf2",
            "input.muadanchor",
            "input.muadibm5",
            "input.muadimp",
            "input.THUIR09An",
            "input.THUIR09LuTA",
            "input.THUIR09TxAn",
            "input.uvaee",
            "input.uvamrf",
            "input.uvamrftop",
            "input.uogTrdphP",
            "input.UMHOObm25GS",
            "input.UMHOObm25IF",
            "input.UMHOOqlGS",
            "input.UMHOOqlIF",
            "input.twCSrs9N",
            "input.twCSrsR",
            "input.twJ48rsU",
            "input.watprf",
            "input.watrrfw",
            "input.watwp",
            "input.WatSdmrm3",
            "input.WatSdmrm3we",
            "input.WatSql",
            "input.yhooumd09BFM",
            "input.yhooumd09BGC",
            "input.yhooumd09BGM"
    };

    private String[] catBRuns = new String[]{
            "input.arsc09web",
            "input.IE09",
            "input.ICTNETADRun3",
            "input.ICTNETADRun4",
            "input.ICTNETADRun5",
            "input.SIEL09",
            "input.irra1a",
            "input.irra2a",
            "input.irra3a",
            "input.NeuLMWeb300",
            "input.NeuLMWeb600",
            "input.NeuLMWebBase",
            "input.RmitLm",
            "input.RmitOkapi",
            "input.UCDSIFTinter",
            "input.UCDSIFTprob",
            "input.UCDSIFTslide",
            "input.scutrun1",
            "input.scutrun2",
            "input.scutrun3",
            "input.UamsAw7an3",
            "input.UamsAwebQE10",
            "input.udelIndDMRM",
            "input.udelIndDRPR",
            "input.udelIndDRSP",
            "input.UDWAxBL",
            "input.UDWAxQE",
            "input.UDWAxQEWeb",
            "input.uogTrdphA",
            "input.uogTrdphCEwP",
            "input.UMHOObm25B",
            "input.UMHOOqlB",
            "input.UMHOOsd",
            "input.UMHOOsdp"
    };


    public CormakEvaluator(boolean catA, DataSet dataSet, String indexTag, Measure measure, String models, String evalDirectory, String op) {
        super(dataSet, indexTag, measure, models, evalDirectory, op);
        if (catA)
            this.modelSet.retainAll(new HashSet<>(Arrays.asList(catARuns)));

    }

    @Override
    public List<Path> getPathList(InfoNeed need, String k) throws IOException {

        Path thePath = Paths.get(need.dataSet().collectionPath().toString(), evalDirectoryMap.get(need.dataSet()), indexTag, "MQ09", k);

        if (!Files.exists(thePath) || !Files.isDirectory(thePath) || !Files.isReadable(thePath))
            throw new IllegalArgumentException(thePath + " does not exist or is not a directory.");

        List<Path> paths = CormakTool.discoverSubmissions(thePath);

        if (paths.size() == 0)
            throw new IllegalArgumentException(thePath + " does not contain any submission files.");

        return paths;
    }

    @Override
    protected String getRunTag(Path path) {
        return path.getFileName().toString();
    }
}

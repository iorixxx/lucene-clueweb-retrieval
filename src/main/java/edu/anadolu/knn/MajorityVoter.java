package edu.anadolu.knn;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.eval.ModelScore;
import org.clueweb09.InfoNeed;

import java.util.List;
import java.util.Map;

/**
 * First implementation of mine!
 */
public class MajorityVoter implements Voter {

    private final Map<InfoNeed, List<ModelScore>> sortedPerformanceMap;

    public MajorityVoter(Map<InfoNeed, List<ModelScore>> sortedPerformanceMap) {
        this.sortedPerformanceMap = sortedPerformanceMap;
    }


    @Override
    public String vote(List<Pair> neighbors) {
        Multiset<String> multiset = HashMultiset.create();

        for (Pair neighbor : neighbors) {

            List<ModelScore> modelScores = sortedPerformanceMap.get(neighbor.infoNeed);

            final double standardError = Math.sqrt(Evaluator.variance(modelScores) / modelScores.size());

            final double best = modelScores.get(0).score;


            for (ModelScore modelScore : modelScores) {


                // never recommend a model that has zero effectiveness!


                if (modelScore.score > (best - standardError)) {

                    if (modelScore.score == 0.0) throw new RuntimeException(modelScore.toString());

                    multiset.add(modelScore.model);

                } else
                    break;
            }
        }
        //System.out.println();
        //System.out.println(multiset);

        if (multiset.size() == 0) {

            throw new RuntimeException("k=" + neighbors.size());

        } else
            return Multisets.copyHighestCountFirst(multiset).asList().get(0);
    }

    @Override
    public String toString() {
        return "Mj";
    }
}

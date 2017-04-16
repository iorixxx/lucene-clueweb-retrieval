package edu.anadolu.knn;

import edu.anadolu.eval.Evaluator;
import org.clueweb09.InfoNeed;

import java.util.List;

/**
 * Selects the term weighting model that has a higher average mean effectiveness among the neighbour set.
 */
public class MeanVoter implements Voter {

    private final Evaluator evaluator;

    public MeanVoter(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public String vote(List<Pair> neighbors) {

        String bestModel = null;
        double max = 0.0;

        for (String model : evaluator.getModelSet()) {

            double avg = 0.0;
            for (Pair neighbor : neighbors) {
                InfoNeed need = neighbor.infoNeed;
                avg += evaluator.score(need, model);
            }
            avg /= neighbors.size();

            if (avg > max) {
                max = avg;
                bestModel = model;
            }
        }

        return bestModel;
    }

    @Override
    public String toString() {
        return "Me";
    }
}

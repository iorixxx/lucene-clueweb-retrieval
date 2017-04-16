package edu.anadolu.eval;

import org.clueweb09.InfoNeed;

/**
 * Interface for evaluation tools like gdeval, trec_eval etc.
 */
public interface EvalTool {
    String getMetric(InfoNeed need, Metric metric);
}

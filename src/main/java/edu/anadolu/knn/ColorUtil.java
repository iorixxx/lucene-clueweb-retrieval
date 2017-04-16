package edu.anadolu.knn;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Colorful models
 */
public final class ColorUtil {

    private final static int[] yellow = {1, 1, 0};

    private final static int[] magenta = {1, 0, 1};

    private final static int[] cyan = {0, 1, 1};

    private final static int[] red = {1, 0, 0};

    private final static int[] green = {0, 1, 0};

    private final static int[] blue = {0, 0, 1};

    private final static int[] white = {1, 1, 1};

    private final static int[] black = {0, 0, 0};

    private final static Map<String, int[]> colMap;

    public static Map<String, int[]> colorMap() {
        return colMap;
    }

    static {

        Map<String, int[]> colorMap = new HashMap<>();
        colorMap.put("BM25", yellow);
        colorMap.put("DFIC", magenta);
        colorMap.put("DFRee", cyan);
        colorMap.put("DPH", red);
        colorMap.put("Dirichlet", green);
        colorMap.put("PL2", blue);
        colorMap.put("LGD", white);
        colorMap.put("DLH13", black);
        colMap = Collections.unmodifiableMap(colorMap);
    }

    private ColorUtil() {
    }
}

package edu.anadolu.knn;

/**
 * chi-square statistics of two binned data sets
 */
public interface Chi {

    <T extends Number> double chiCDF(T[] R, T[] S);

    <T extends Number> double chiPDF(T[] R, T[] S);

    <T extends Number> double chiSquared(T[] R, T[] S);
}

package edu.anadolu.exp;


import org.apache.commons.math3.distribution.PoissonDistribution;

import java.util.Arrays;
import java.util.Random;

/**
 * Simulation for Poisson for Randomness
 */
public class Simulation {


    public int[] generateADoc(int docLength, int vocabularySize) {

        Random random = new Random();

        int[] array = new int[docLength];

        for (int i = 0; i < docLength; i++) {
            array[i] = random.nextInt(vocabularySize) + 1;
        }
        return array;
    }

    public int countOnes(int[] array) {

        int counter = 0;
        for (int i : array) {
            if (i == 1) counter++;
        }

        return counter;

    }

    public int sum(int[] array) {

        int sum = 0;
        for (int i : array) {
            sum += i;
        }

        return sum;

    }

    int[] createAndInitializeArray(int docLength) {

        int[] array = new int[docLength];
        Arrays.fill(array, 0);
        return array;
    }


    /**
     * @param lambda mean TF/c
     * @param k
     * @return probability of K=k
     */
    double getPoisson(double lambda, int k) {
        return Math.pow(lambda, (double) k) * Math.pow(Math.E, (-1 * lambda)) / (double) factorial(k);
    }

    public int factorial(int n) {

        if (n == 0) return 1;

        int fact = 1; // this  will be the result
        for (int i = 1; i <= n; i++) {
            fact *= i;
        }
        return fact;
    }

    /**
     * This method can return <>zero</>zero. Probably we should call this function N times. Where N is the number document in the collection.
     *
     * @param lambda mean TF/c
     * @return small k
     */
    public int inversePoisson(double lambda) {

        double L = Math.pow(Math.E, -lambda);
        int k = 0;
        double p = 1.0;

        do {
            k++;
            double u = new Random().nextDouble();
            p = p * u;
        } while (p > L);

        return k - 1;
    }


    public void the() {

        double lambda = 5;

        for (int i = 1; i < 1000; i++) {
            // returning 7 means a artificial document d tf(d,'the')=7
            // returning 0 means a artificial document d tf(d,'the')=O
            System.out.println(inversePoisson(lambda));

        }

    }

    public void testApp() {


        int vocabularySize = 50;

        int numDocs = 10000;

        int docLength = 500;


        int TF = 0;
        int[] counterArray = createAndInitializeArray(docLength + 1);

        for (int i = 0; i < numDocs; i++) {

            int[] array = generateADoc(docLength, vocabularySize);

            // System.out.println(Arrays.toString(array) + " => " + countOnes(array));

            counterArray[countOnes(array)]++;

            TF += countOnes(array);
        }

        System.out.println("content of radix array including 0 " + Arrays.toString(counterArray));

        System.out.println("sum of elements of radix array " + sum(counterArray));


        System.out.println("TF OR cf(t) " + TF);


        double lambda = (double) TF / numDocs;

        PoissonDistribution poisson = new PoissonDistribution(lambda);

        System.out.println("lambda OR mean " + lambda);


        double cdf_observed = 0.0;
        System.out.format("k \t observed \t possion \t cdf_observed \t cdf_poisson%n");

        for (int i = 0; i < counterArray.length; i++)

        {
            //    System.out.println(i + "\t" + (double) counterArray[i] / numDocs + "\t" + getPoisson(docLength / vocabularySize, i));

            cdf_observed += (double) counterArray[i] / numDocs;


            //   cdf_poisson += getPoisson(lambda, i);
            //   System.out.format("%d \t %.6f \t %.6f \t %.6f \t %.6f%n", i, (double) counterArray[i] / numDocs, getPoisson(lambda, i), cdf_observed, cdf_poisson);

            System.out.format("%d \t %.6f \t %.6f \t %.6f \t %.6f%n", i, (double) counterArray[i] / numDocs, poisson.probability(i), cdf_observed, poisson.cumulativeProbability(i));


        }

        int docLen = 0;

        for (int l = 0; l < numDocs; l++) {


            for (int j = 0; j < vocabularySize; j++) {

                //   docLen += inversePoisson(lambda);

                docLen += poisson.sample();

                //    System.out.print(inversePoisson((double) sum / numDocs));

                //     System.out.print(" ");


            }

            // System.out.println();
        }

        System.out.println();
        System.out.println(docLen / numDocs);

    }

    public static void main(String[] args) {
        Simulation simulation = new Simulation();
        simulation.testApp();
    }
}

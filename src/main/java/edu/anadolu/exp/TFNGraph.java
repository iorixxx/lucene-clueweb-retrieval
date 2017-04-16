package edu.anadolu.exp;

import edu.anadolu.freq.L2;
import edu.anadolu.freq.TFNormalization;

/**
 * Length Normalization L1, L2, etc Graphics
 */
public class TFNGraph {

    public static void main(String[] args) {


        TFNormalization tfn = new L2();

        for (int tf = 1; tf < (int) tfn.avgFieldLength; tf++) {
            System.out.println(tf + "\t" + tfn.tfn(tf, (int) tfn.avgFieldLength));
        }

        System.out.println("===========");

        for (int d = 1; d <= (int) tfn.avgFieldLength * 2; d++) {
            System.out.println(d + "\t" + tfn.tfn(1, d));
        }
    }
}

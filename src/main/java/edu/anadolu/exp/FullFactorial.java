package edu.anadolu.exp;

import edu.anadolu.datasets.Collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


/**
 * Full factorial design
 */
public class FullFactorial {

    public static String print(int[] arr) {
        StringBuilder builder = new StringBuilder();

        for (int i : arr) {
            builder.append(i).append("\t");
        }


        return builder.toString().trim();
    }


    public static class EXP {
        public final Collection collection;
        final String tag;
        public final String op;
        public final int id;

        public EXP(Collection collection, String tag, String op, int id) {
            this.collection = collection;
            this.tag = tag;
            this.op = op;
            this.id = id;
        }

        @Override
        public String toString() {
            return "EXA" + Integer.toString(id) + "(" + collection.toString() + "," + tag + "," + op + ")";
        }

        public String tag() {
            if ("Anchor".equals(tag)) return "KStemAnalyzerAnchor";

            if ("NoAnchor".equals(tag)) return "KStemAnalyzer";

            throw new RuntimeException("unrecognized tag : " + tag);
        }
    }

    public static List<EXP> experiments(String tag) {

        List<EXP> experiments = new ArrayList<>();

        int counter = 0;
        for (Collection collection : collections) {

            for (final String op : operators) {
                if ((Collection.ROB04.equals(collection) || Collection.GOV2.equals(collection)) && "Anchor".equals(tag))
                    continue;

                experiments.add(new EXP(collection, tag, op, ++counter));
            }

        }

        return experiments;
    }


    static final List<Collection> collections = Arrays.asList(Collection.MQ09, Collection.CW09A, Collection.CW09B, Collection.CW12B);
    static final int[] collectionsArray = new int[collections.size()];





    static final List<String> operators = Arrays.asList("OR");

    public static void main(String[] args) {


        String[] colors = {


                "yellow",

                "magenta",

                "cyan",

                "red",

                "green",

                "blue",

                "white",

                "black"
        };


        System.out.print("colors = {");
        for (int i = 0; i < 190; i++) {
            System.out.print("'" + colors[new Random().nextInt(colors.length)] + "',");
        }
        System.out.println("}");

    }
}




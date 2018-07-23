package edu.anadolu.analysis;

/**
 * Enumeration for {@link org.apache.lucene.analysis.Analyzer} Tag
 */
public enum Tag {

    NoStem, KStem, ICU, Latin, Zemberek, NoStemTurkish, KStemField, Script, UAX;

    public static Tag tag(String indexTag) {

        final int i = indexTag.indexOf("Anchor");
        final String name = (i == -1 ? indexTag : indexTag.substring(0, i));

        final int j = name.indexOf("_");

        if (j == -1)
            return valueOf(name);
        else
            return valueOf(name.substring(j + 1));


    }
}

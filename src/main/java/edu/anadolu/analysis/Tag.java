package edu.anadolu.analysis;

/**
 * Enumeration for {@link org.apache.lucene.analysis.Analyzer} Tag
 */
public enum Tag {

    HunspellTurkish, SnowballTurkish, F5, NoStem, KStem, Snowball, Hunspell, ICU, Latin, Zemberek, NoStemTurkish, KStemField, Script, UAX, ASCII;

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

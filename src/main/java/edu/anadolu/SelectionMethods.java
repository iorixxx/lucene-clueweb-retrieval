package edu.anadolu;


import org.apache.lucene.search.IndexSearcher;
import org.clueweb09.InfoNeed;


public class SelectionMethods {

    public static IndexSearcher MST(InfoNeed infoNeed,IndexSearcher... searchers){
        if(searchers.length!=2) throw new RuntimeException("We do not support selective stemming for searchers whose count is not equal to 2 yet!");


        return null;
    }
}

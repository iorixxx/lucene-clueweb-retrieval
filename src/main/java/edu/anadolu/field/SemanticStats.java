package edu.anadolu.field;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;


public class SemanticStats {
    private volatile static SemanticStats semanticStats = new SemanticStats();

    private Analyzer analyzer;

    private  HashMap<String,Integer> docTypeCounter = new HashMap<>();
    private  HashMap<Pair,Integer> textTitleMapper = new HashMap<>();
    private  HashMap<String,Integer> tagTFCounter = new HashMap<>();
    private  HashMap<String,Integer> tagDFCounter = new HashMap<>();

    private SemanticStats(){
        try {
            this.analyzer = MetaTag.whitespaceAnalyzer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static SemanticStats getSemanticObject(){
        return semanticStats;
    }

    public synchronized Integer incDocType(String type){
        type=getNormalizedText(type);
        if(docTypeCounter.containsKey(type)){
            docTypeCounter.put(type,docTypeCounter.get(type)+1);
        }else docTypeCounter.put(type,1);
        return docTypeCounter.get(type);
    }

    public synchronized Integer incTagTF(String tag){
        tag=getNormalizedText(tag);
        if(tagTFCounter.containsKey(tag)){
            tagTFCounter.put(tag,tagTFCounter.get(tag)+1);
        }else tagTFCounter.put(tag,1);
        return tagTFCounter.get(tag);
    }

    public synchronized Integer incTagTFby(String tag,Integer x){
        tag=getNormalizedText(tag);
        if(tagTFCounter.containsKey(tag)){
            tagTFCounter.put(tag,tagTFCounter.get(tag)+x);
        }else tagTFCounter.put(tag,x);
        return tagTFCounter.get(tag);
    }

    public synchronized Integer incTagDF(String tag){
        tag=getNormalizedText(tag);
        if(tagDFCounter.containsKey(tag)){
            tagDFCounter.put(tag,tagDFCounter.get(tag)+1);
        }else tagDFCounter.put(tag,1);
        return tagDFCounter.get(tag);
    }

    public synchronized Pair putTextWithTitle(String title,String text){
        title=getNormalizedText(title);
        text=getNormalizedText(text);
        Pair<String,String> p = Pair.of(title,text);
        if(!textTitleMapper.keySet().contains(p)) {
            textTitleMapper.put(p, 1);
        }else{
            textTitleMapper.put(p,textTitleMapper.get(p)+1);
        }
        return p;
    }

    private String getNormalizedText(String text){
        TokenStream stream = analyzer.tokenStream(null, text);
        StringBuilder builder = new StringBuilder();
        try {
            stream.reset();
            while (stream.incrementToken()) {
                builder.append(stream.getAttribute(CharTermAttribute.class).toString()+" ");
            }
            stream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return builder.toString().trim();
    }
    public void printSemanticStats(){
        try {
            FileWriter writer = new FileWriter("SemanticStats.txt");
            StringBuilder builder = new StringBuilder();

            builder.append("DocTypes"+System.lineSeparator());
            for(Entry<String,Integer> entry: docTypeCounter.entrySet()){
                builder.append(entry.getKey()+": "+entry.getValue()+System.lineSeparator());
            }

            builder.append("Tag TF Amounts"+System.lineSeparator());
            for(Entry<String,Integer> entry: tagTFCounter.entrySet()){
                builder.append(entry.getKey()+": "+entry.getValue()+System.lineSeparator());
            }

            builder.append("Tag DF Amounts"+System.lineSeparator());
            for(Entry<String,Integer> entry: tagDFCounter.entrySet()){
                builder.append(entry.getKey()+": "+entry.getValue()+System.lineSeparator());
            }

            builder.append("Title-Text Pairs"+System.lineSeparator());
            for(Entry<Pair,Integer> entry: textTitleMapper.entrySet()){
                Pair<String,String> p = entry.getKey();
                builder.append(p.getLeft()+": "+p.getRight()+": count("+entry.getValue()+")"+ System.lineSeparator());
            }
            writer.write(builder.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package edu.anadolu.ltr;

public class TextToDocRatio implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        int textLen = base.jDoc.text().length();
        int htmlLen = base.rawHTML.length();
        if(htmlLen==0) return 0;

        if((double) (textLen) / htmlLen>1.0){
            System.out.println("****************************************************************************************************************************************");
            System.out.println("Doc Id = " + base.docId + " TextToDocRatio : " + (double) (textLen) / htmlLen);
            System.out.println("********************************************************************");
            System.out.println(base.jDoc.html());
            System.out.println("****************************************************************************************************************************************");
        }

        return (double) (textLen) / htmlLen;
    }
}

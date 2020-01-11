package edu.anadolu.ltr;

public class TextToDocRatio implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        String text = base.jDoc.text().replaceAll("\\s+", " ");
        String html = base.rawHTML.replaceAll("\\s+", " ");
        if(html.length()==0) return 0;

        if((double) (text.length()) / html.length()>1.0){
            System.out.println("****************************************************************************************************************************************");
            System.out.println("Doc Id = " + base.docId + " TextToDocRatio : " + (double) (text.length()) / html.length());
            System.out.println("********************************************************************");
            System.out.println(base.jDoc.html());
            System.out.println("****************************************************************************************************************************************");
        }

        return (double) (text.length()) / html.length();
    }
}

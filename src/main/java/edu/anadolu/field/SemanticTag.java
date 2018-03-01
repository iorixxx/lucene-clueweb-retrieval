package edu.anadolu.field;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.LinkedList;
import java.util.List;


public class SemanticTag {
    public enum HTMLTag{
        a, abbr, acronym, address, applet, area, article, aside, audio, b, base, basefont, bdi, bdo, big, blockquote,
        body, br, button, canvas, caption, center, cite, code, col, colgroup, data, datalist, dd, del, details, dfn,
        dialog, dir, div, dl, dt, em, embed, fieldset, figcaption, figure, font, footer, form, frame, frameset,
        h1, h2, h3, h4, h5, h6, head, header, hr, html, i, iframe, img, input, ins, kbd, label, legend, li, link, main,
        map, mark, menu, menuitem, meta, meter, nav, noframes, noscript, object, ol, optgroup, option, output, p, param,
        picture, pre, progress, q, rp, rt, ruby, s, samp, script, section, select, small, source, span, strike, strong,
        style, sub, summary, sup, table, tbody, td, template, textarea, tfoot, th, thead, time, title, tr, track, tt, u,
        ul, var, video, wbr


    }

    private HTMLTag tag;
    private String text;
    private List<Pair<String,String>> attrList;


    public SemanticTag(HTMLTag tag, String text) {
        this.tag = tag;
        this.text=text;
        attrList = new LinkedList<>();
    }

    public String getText() {
        return text;
    }

    public void addAttr(Pair<String,String> pair){
        attrList.add(pair);
    }

    public HTMLTag getTag() {
        return tag;
    }

    public List<Pair<String, String>> getAttrList() {
        return attrList;
    }

    public Pair<String, String> findAttr(String key){
        for(Pair<String, String> pair : attrList)
            if(pair.getLeft().equals(key)) return pair;
        return new ImmutablePair<>(key,"");
    }

    public void setAttrList(List<Pair<String, String>> attrList) {
        this.attrList = attrList;
    }

}

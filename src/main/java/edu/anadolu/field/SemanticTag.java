package edu.anadolu.field;

import org.apache.commons.lang3.tuple.ImmutablePair;

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
    private List<ImmutablePair<String,String>> attrList;


    public SemanticTag(HTMLTag tag, String text) {
        this.tag = tag;
        this.text=text;
        attrList = new LinkedList<>();
    }

    public String getText() {
        return text;
    }

    public void addAttr(ImmutablePair pair){
        attrList.add(pair);
    }

    public HTMLTag getTag() {
        return tag;
    }

    public List<ImmutablePair<String, String>> getAttrList() {
        return attrList;
    }

    public void setAttrList(List<ImmutablePair<String, String>> attrList) {
        this.attrList = attrList;
    }

}

package com.vaadin.flow.demo;

import com.vaadin.annotations.StyleSheet;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.html.Div;

/**
 * This builds up source samples and sets it up so that it can be annotated by
 * <a href="http://prismjs.com/">prism</a>
 */
@StyleSheet("frontend://src/css/sources.css")
public class SourceContent extends Div {

    public SourceContent() {
        getElement().setAttribute("class", "sources");
    }

    public void addCode(String text) {
        Element pre = new Element("pre");
        Element code = new Element("code");
        pre.appendChild(code);
        code.setAttribute("spellcheck", "false");
        code.setAttribute("class", "language-java");
        code.setText(text);
        getElement().appendChild(pre);
    }

    public void addCss(String text) {
        Element pre = new Element("pre");
        Element code = new Element("code");
        pre.appendChild(code);
        code.setAttribute("spellcheck", "false");
        code.setAttribute("class", "language-css");
        code.setText(text);
        getElement().appendChild(pre);
    }
}

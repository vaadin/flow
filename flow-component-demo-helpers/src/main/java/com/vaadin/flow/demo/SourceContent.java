/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.demo;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;

/**
 * This builds up source samples and sets it up so that it can be annotated by
 * <a href="http://prismjs.com/">prism</a>
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@StyleSheet("src/css/sources.css")
@JavaScript("src/script/prism.js")
public class SourceContent extends Div {

    /**
     * Default constructor for the component.
     */
    public SourceContent() {
        getElement().getClassList().add("sources");
    }

    /**
     * Adds a text to be rendered as a Java source code inside the component.
     *
     * @param text
     *            The source code to be shown.
     */
    public void addCode(String text) {
        addSourceCodeBlock(text, "language-java");
    }

    /**
     * Adds a text to be rendered as a CSS source code inside the component.
     *
     * @param text
     *            The source code to be shown.
     */
    public void addCss(String text) {
        addSourceCodeBlock(text, "language-css");
    }

    /**
     * Adds a text to be rendered as a HTML source code inside the component.
     *
     * @param text
     *            The source code to be shown.
     */
    public void addHtml(String text) {
        addSourceCodeBlock(text, "language-markup");
    }

    /**
     * Adds an {@link Element} directly into this component, without need to use
     * a component for it.
     *
     * @param element
     *            The element to be added to this component.
     */
    public void add(Element element) {
        getElement().appendChild(element);
    }

    private void addSourceCodeBlock(String text, String className) {
        Element pre = new Element("pre");
        Element code = new Element("code");
        pre.appendChild(code);
        code.setAttribute("spellcheck", "false");
        code.getClassList().add(className);
        code.setText(text);
        getElement().appendChild(pre);
        code.executeJs("Prism.highlightElement(this);");
    }
}

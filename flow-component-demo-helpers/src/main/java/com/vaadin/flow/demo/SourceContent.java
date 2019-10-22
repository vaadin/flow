/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.demo;

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
    }
}

/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import com.vaadin.flow.dom.Element;
import com.vaadin.ui.common.StyleSheet;
import com.vaadin.ui.html.Div;

/**
 * This builds up source samples and sets it up so that it can be annotated by
 * <a href="http://prismjs.com/">prism</a>
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
        Element pre = new Element("pre");
        Element code = new Element("code");
        pre.appendChild(code);
        code.setAttribute("spellcheck", "false");
        code.getClassList().add("language-java");
        code.setText(text);
        getElement().appendChild(pre);
    }

    /**
     * Adds a text to be rendered as a CSS source code inside the component.
     *
     * @param text
     *            The source code to be shown.
     */
    public void addCss(String text) {
        Element pre = new Element("pre");
        Element code = new Element("code");
        pre.appendChild(code);
        code.setAttribute("spellcheck", "false");
        code.getClassList().add("language-css");
        code.setText(text);
        getElement().appendChild(pre);
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
}

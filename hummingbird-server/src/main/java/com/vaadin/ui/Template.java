/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.ui;

import java.io.IOException;
import java.io.InputStream;

import com.vaadin.annotations.HtmlTemplate;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.impl.TemplateElementStateProvider;
import com.vaadin.hummingbird.nodefeature.TemplateMap;
import com.vaadin.hummingbird.router.HasChildView;
import com.vaadin.hummingbird.router.RouterConfiguration;
import com.vaadin.hummingbird.router.View;
import com.vaadin.hummingbird.template.TemplateNode;
import com.vaadin.hummingbird.template.TemplateParseException;
import com.vaadin.hummingbird.template.parser.TemplateParser;

/**
 * Component for declaratively defined element structures. The structure of a
 * template is loaded from an .html file on the classpath.
 * <p>
 * There are two options to specify the HTML file:
 * <ul>
 * <li>By default the HTML file should be in the same package of the class, and
 * the name should be the same as the class name, but with the
 * <code>.html</code> file extension.
 * <li>Annotate your subclass with {@link HtmlTemplate} annotation with the HTML
 * file path as a value. The path can be either relative (without leading "/")
 * or absolute. In the first case the path is considered as a relative for the
 * class package.
 * </ul>
 * <p>
 * A template can be used as a {@link HasChildView} in
 * {@link RouterConfiguration} if the template file contains a
 * <code>@child@</code> slot.
 *
 * @see HtmlTemplate
 *
 * @author Vaadin Ltd
 */
public abstract class Template extends Component implements HasChildView {
    private final StateNode stateNode = TemplateElementStateProvider
            .createRootNode();

    /**
     * Creates a new template.
     */
    public Template() {
        // Will set element later
        super(null);

        HtmlTemplate annotation = getClass().getAnnotation(HtmlTemplate.class);
        if (annotation == null) {
            setTemplateElement(getClass().getSimpleName() + ".html");
        } else {
            setTemplateElement(annotation.value());
        }
    }

    /**
     * Creates a new template using {@code templateFileName} as the template
     * file name.
     *
     * @param templateFileName
     *            the template file name, not {@code null}
     */
    protected Template(String templateFileName) {
        // Will set element later
        super(null);

        setTemplateElement(templateFileName);
    }

    /**
     * Creates a new template using {@code inputStream} as a template content.
     *
     * @param inputStream
     *            the HTML snippet input stream
     *
     */
    protected Template(InputStream inputStream) {
        // Will set element later
        super(null);

        setTemplateElement(inputStream);
    }

    private void setTemplateElement(String templateFileName) {
        if (templateFileName == null) {
            throw new IllegalArgumentException(
                    "HTML template file name cannot be null");
        }
        InputStream templateContentStream = getClass()
                .getResourceAsStream(templateFileName);
        if (templateContentStream == null) {
            throw new IllegalArgumentException(
                    templateFileName + " not found on the classpath");
        }
        setTemplateElement(templateContentStream);
    }

    private void setTemplateElement(InputStream inputStream) {
        try (InputStream templateContentStream = inputStream) {

            TemplateNode templateRoot = TemplateParser
                    .parse(templateContentStream);

            stateNode.getFeature(TemplateMap.class)
                    .setRootTemplate(templateRoot);

            Element rootElement = Element.get(stateNode);

            setElement(this, rootElement);
        } catch (IOException e) {
            throw new TemplateParseException("Error reading template", e);
        }
    }

    @Override
    public void setChildView(View childView) {
        TemplateMap templateMap = stateNode.getFeature(TemplateMap.class);
        if (childView == null) {
            templateMap.setChild(null);
        } else {
            templateMap.setChild(childView.getElement().getNode());
        }
    }

}

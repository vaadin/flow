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

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.nodefeature.ComponentMapping;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.hummingbird.nodefeature.TemplateMap;
import com.vaadin.hummingbird.nodefeature.TemplateOverridesMap;
import com.vaadin.hummingbird.template.TemplateNode;
import com.vaadin.hummingbird.template.TemplateParseException;
import com.vaadin.hummingbird.template.TemplateParser;

/**
 * Component for declaratively defined element structures. The structure of a
 * template is loaded from an .html file on the classpath. The file should be in
 * the same package of the class, and the name should be the same as the class
 * name, but with the <code>.html</code> file extension.
 *
 * @author Vaadin Ltd
 */
public abstract class Template extends Component {
    private final StateNode stateNode = new StateNode(TemplateMap.class,
            TemplateOverridesMap.class, ComponentMapping.class, ModelMap.class);

    /**
     * Creates a new template.
     */
    public Template() {
        // Will set element later
        super(null);

        setTemplateElement(getClass().getSimpleName() + ".html");
    }

    /**
     * Creates a new template using {@code templateFileName} as the template
     * file name.
     */
    protected Template(String templateFileName) {
        // Will set element later
        super(null);

        setTemplateElement(templateFileName);
    }

    /**
     * Creates a new template using {@code inputStream} as a template content.
     */
    protected Template(InputStream inputStream) {
        // Will set element later
        super(null);

        setTemplateElement(inputStream);
    }

    private void setTemplateElement(String templateFileName) {
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
}

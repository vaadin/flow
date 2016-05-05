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
package com.vaadin.hummingbird.template.parser;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Node;

import com.vaadin.hummingbird.template.ElementTemplateBuilder;
import com.vaadin.hummingbird.template.ModelValueBindingProvider;
import com.vaadin.hummingbird.template.StaticBindingValueProvider;
import com.vaadin.hummingbird.template.TemplateParseException;

/**
 * @author Vaadin Ltd
 *
 */
public abstract class AbstractTemplateBuilderFactory<T extends Node>
        implements TemplateNodeBuilderFactory<T> {

    private final Class<T> nodeType;

    protected AbstractTemplateBuilderFactory(Class<T> nodeType) {
        this.nodeType = nodeType;
    }

    @Override
    public boolean isApplicable(Node node) {
        return nodeType.isAssignableFrom(node.getClass())
                && canHandle(nodeType.cast(node));
    }

    @Override
    public boolean isDefault(Node node) {
        return false;
    }

    protected abstract boolean canHandle(T node);

    protected void setBinding(Attribute attribute,
            ElementTemplateBuilder builder) {
        String name = attribute.getKey();

        if (name.startsWith("(")) {
            throw new TemplateParseException(
                    "Dynamic binding support has not yet been implemented");
        } else if (name.startsWith("[")) {
            if (!name.endsWith("]")) {
                StringBuilder msg = new StringBuilder(
                        "Property binding should be in the form [property]='value' but template contains '");
                msg.append(attribute.toString()).append("'.");
                throw new TemplateParseException(msg.toString());
            }
            String key = name;
            key = key.substring(1);
            key = key.substring(0, key.length() - 1);
            builder.setProperty(key,
                    new ModelValueBindingProvider(attribute.getValue()));
        } else {
            /*
             * Regular attribute names in the template, i.e. name not starting
             * with [ or (, are used as static attributes on the target element.
             */
            builder.setAttribute(name,
                    new StaticBindingValueProvider(attribute.getValue()));
        }
    }
}

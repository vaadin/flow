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
package com.vaadin.flow.template.angular.parser;

import java.util.regex.Pattern;

import org.jsoup.nodes.Node;

import com.vaadin.flow.template.angular.AbstractBindingValueProvider;
import com.vaadin.flow.template.angular.JsExpressionBindingProvider;
import com.vaadin.flow.template.angular.ModelValueBindingProvider;
import com.vaadin.flow.template.angular.TemplateParseException;

/**
 * Abstract template builder factory.
 *
 * @param <T>
 *            the node type which factory is able to handle
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public abstract class AbstractTemplateBuilderFactory<T extends Node>
        implements TemplateNodeBuilderFactory<T> {

    private static final Pattern SIMPLE_MODEL_REFERENCE_EXPRESSION = Pattern
            .compile("[\\w\\.]+");

    /**
     * Threadlocal for tracking whether parser is inside a for loop or not. To
     * be completely removed once scopes are properly implemented.
     */
    protected static ThreadLocal<String> insideFor = new ThreadLocal<>();

    private final Class<T> nodeType;

    protected AbstractTemplateBuilderFactory(Class<T> nodeType) {
        this.nodeType = nodeType;
    }

    @Override
    public boolean isApplicable(Node node) {
        return nodeType.isAssignableFrom(node.getClass())
                && canHandle(nodeType.cast(node));
    }

    /**
     * Returns {@code true} if the factory can handle the {@code node}.
     *
     * @param node
     *            the node to check against
     * @return {@code true} if the factory can handle the {@code node}
     */
    protected abstract boolean canHandle(T node);

    /**
     * Ensure the given model key starts with the for loop variable and a dot if
     * inside a ngFor. Also strips the loop variable prefix so the rest of the
     * code, which does not know anything about namespacing, works.
     *
     * @param modelKey
     *            the model key to strip the prefix from
     * @return the original model key if not inside an ngFor or a stripped key
     *         if inside an ngFor
     */
    protected static String stripForLoopVariableIfNeeded(String modelKey) {
        String forLoopVariable = insideFor.get();
        if (forLoopVariable != null) {
            if (!modelKey.startsWith(forLoopVariable + ".")) {
                String msg =
                        "Property binding inside a for loop must currently bind to the loop variable, i.e. start with '"
                                + forLoopVariable + ".'";
                throw new TemplateParseException(msg);
            } else {
                return modelKey.substring(forLoopVariable.length() + 1);
            }
        } else {
            return modelKey;
        }
    }

    /**
     * Extract template parameter key from the {@code parameterString}.
     *
     * @param parameterString
     *            the parameter string enclosing a parameter
     * @param enclosingLength
     *            length of enclosing prefix and suffix in the parameter string
     * @return extracted template parameter key
     */
    protected String extractKey(String parameterString, int enclosingLength) {
        String key = parameterString;
        key = key.substring(enclosingLength);
        return key.substring(0, key.length() - enclosingLength);
    }

    protected static AbstractBindingValueProvider createExpressionBinding(
            String expression) {
        if (SIMPLE_MODEL_REFERENCE_EXPRESSION.matcher(expression).matches()) {
            return new ModelValueBindingProvider(expression);
        } else {
            return new JsExpressionBindingProvider(expression);
        }
    }
}

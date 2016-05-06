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
package com.vaadin.hummingbird.template;

/**
 * Builder for {@link ForTemplateNode}.
 *
 * @author Vaadin Ltd
 */
public class ForTemplateBuilder implements TemplateNodeBuilder {

    private final ElementTemplateBuilder childBuilder;

    private final String loopVariable;

    private final String collectionVariable;

    /**
     * Creates a builder using the given loop and collection variables (
     * <code>let loop of collection</code>) and the given child builder.
     *
     * @param loopVariable
     *            the loop variable
     * @param collectionVariable
     *            the collection variable
     * @param childBuilder
     *            the builder for the mandatory child node
     */
    public ForTemplateBuilder(String loopVariable, String collectionVariable,
            ElementTemplateBuilder childBuilder) {
        this.loopVariable = loopVariable;
        this.collectionVariable = collectionVariable;
        this.childBuilder = childBuilder;
    }

    @Override
    public TemplateNode build(TemplateNode parent) {
        if (parent == null) {
            throw new IllegalArgumentException(
                    "A for loop cannot be the root element in a template");
        }

        assert parent instanceof AbstractElementTemplateNode;

        return new ForTemplateNode((AbstractElementTemplateNode) parent,
                collectionVariable, loopVariable, childBuilder);
    }

}

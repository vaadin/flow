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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.function.Function;

import com.vaadin.external.jsoup.nodes.Element;
import com.vaadin.external.jsoup.nodes.Node;
import com.vaadin.flow.template.angular.ElementTemplateBuilder;
import com.vaadin.flow.template.angular.ForTemplateBuilder;
import com.vaadin.flow.template.angular.TemplateNodeBuilder;
import com.vaadin.flow.template.angular.TemplateParseException;

/**
 * The factory that handles *ngFor template elements.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public class ForElementBuilderFactory
        extends AbstractTemplateBuilderFactory<Element> {

    private static final String NG_FOR = "*ngFor";

    /**
     * Creates a new factory.
     */
    public ForElementBuilderFactory() {
        super(Element.class);
    }

    @Override
    public TemplateNodeBuilder createBuilder(Element element,
            Function<Node, Optional<TemplateNodeBuilder>> builderProducer) {
        String ngFor = element.attr(NG_FOR);
        element.removeAttr(NG_FOR);

        List<String> tokens = parseNgFor(ngFor);
        if (tokens.size() != 4 || !"let".equals(tokens.get(0))
                || !"of".equals(tokens.get(2))) {
            throw new TemplateParseException(
                    "The 'ngFor' template is supported only in the form *ngFor='let item of list', but template contains "
                            + ngFor);
        }

        String loopVariable = tokens.get(1);
        if (insideFor.get() != null) {
            throw new TemplateParseException(
                    "Nested *ngFor are currently not supported");
        }

        Optional<TemplateNodeBuilder> subBuilder;
        try {
            insideFor.set(loopVariable);
            subBuilder = builderProducer.apply(element);
        } finally {
            insideFor.remove();
        }
        if (!subBuilder.isPresent()) {
            throw new IllegalStateException(
                    "Sub builder missing for *ngFor element " + element.html());
        }
        if (!(subBuilder.get() instanceof ElementTemplateBuilder)) {
            throw new IllegalStateException("Sub builder for *ngFor element "
                    + element.html() + " of invalid type: "
                    + subBuilder.get().getClass().getName());
        }

        return new ForTemplateBuilder(loopVariable, tokens.get(3),
                (ElementTemplateBuilder) subBuilder.get());
    }

    @Override
    protected boolean canHandle(Element node) {
        return node.hasAttr(NG_FOR);
    }

    private List<String> parseNgFor(String ngFor) {
        List<String> tokens = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(ngFor);
        while (tokenizer.hasMoreTokens()) {
            tokens.add(tokenizer.nextToken());
        }
        return tokens;
    }

}

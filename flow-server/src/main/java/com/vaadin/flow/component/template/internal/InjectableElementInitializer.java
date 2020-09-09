/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.component.template.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;

/**
 * Initializer the template element with data.
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
public class InjectableElementInitializer
        implements Consumer<Map<String, String>> {

    private final Element element;

    private final Class<? extends Component> templateClass;

    private static final Map<String, ElementInitializationStrategy> INIT_STRATEGIES = createStategies();

    private static final ElementInitializationStrategy DEFAULT_STRATEGY = new PropertyInitializationStrategy();

    /**
     * Creates an initializer for the {@code element}.
     * 
     * @param element
     *            element to initialize
     * @param templateClass
     *            the class of the template component
     */
    public InjectableElementInitializer(Element element,
            Class<? extends Component> templateClass) {
        this.element = element;
        this.templateClass = templateClass;
    }

    @Override
    public void accept(Map<String, String> templateAttributes) {
        templateAttributes
                .forEach((name, value) -> initialize(element, name, value));
    }

    private void initialize(Element element, String name, String value) {
        if (name.endsWith("$")) {
            // this is an attribute binding, ignore it since we don't support
            // bindings: the value is not an expression
            getLogger().debug(
                    "Template {} contains an attribute {} in element {} which "
                            + "ends with $ and ignored by initialization since this is an attribute binding",
                    templateClass.getSimpleName(), name, element.getTag());
            return;
        }
        if (value.contains("{{") && value.contains("}}")) {
            // this is a binding, skip it
            getLogger().debug(
                    "Template {} contains an attribute {} in element {} whose value"
                            + " contains two-way binding and it's ignored by initilization",
                    templateClass.getSimpleName(), name, element.getTag());
            return;
        }
        if (value.contains("[[") && value.contains("]]")) {
            // this is another binding, skip it
            getLogger().debug(
                    "Template {} contains an attribute {} in element {} whose value"
                            + " contains binding and it's ignored by initilization",
                    templateClass.getSimpleName(), name, element.getTag());
            return;
        }
        // anything else is considered as a template attribute value
        getStrategy(name).initialize(element, name, value);
    }

    private ElementInitializationStrategy getStrategy(String attributeName) {
        ElementInitializationStrategy strategy = INIT_STRATEGIES
                .get(attributeName);
        if (strategy == null) {
            return DEFAULT_STRATEGY;
        }
        return strategy;
    }

    private static Map<String, ElementInitializationStrategy> createStategies() {
        Map<String, ElementInitializationStrategy> result = new HashMap<>();
        AttributeInitializationStrategy attributeStrategy = new AttributeInitializationStrategy();
        result.put("id", attributeStrategy);
        result.put("class", attributeStrategy);
        result.put("style", attributeStrategy);
        result.put("theme", attributeStrategy);
        return result;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(InjectableElementInitializer.class);
    }
}

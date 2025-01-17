/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import com.vaadin.flow.dom.Element;

/**
 * Generic initializer logic.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
public abstract class AbstractInjectableElementInitializer
        implements Consumer<Map<String, String>> {

    private final Element element;

    private static final Map<String, ElementInitializationStrategy> INIT_STRATEGIES = createStrategies();
    private static final IdentityHashMap<Pattern, ElementInitializationStrategy> PATTERN_STRATEGIES = createPatternStrategies();

    private static final ElementInitializationStrategy DEFAULT_STRATEGY = new PropertyInitializationStrategy();

    /**
     * Creates an initializer for the {@code element}.
     *
     * @param element
     *            element to initialize
     */
    protected AbstractInjectableElementInitializer(Element element) {
        this.element = element;
    }

    @Override
    public void accept(Map<String, String> templateAttributes) {
        templateAttributes.forEach(this::initialize);
    }

    /**
     * Checks whether the attribute declaration is an attribute with a static
     * value ( so it can be set on the serve side).
     *
     * @param name
     *            the template attribute name
     * @param value
     *            the template attribute value
     * @return whether the attribute declaration is an attribute with a static
     *         value
     */
    protected abstract boolean isStaticAttribute(String name, String value);

    /**
     * Returns server side element to initialize.
     *
     * @return the server side element to initialize
     */
    protected Element getElement() {
        return element;
    }

    private void initialize(String name, String value) {
        if (isStaticAttribute(name, value)) {
            getStrategy(name).initialize(element, name, value);
        }
    }

    private ElementInitializationStrategy getStrategy(String attributeName) {
        ElementInitializationStrategy strategy = INIT_STRATEGIES
                .get(attributeName);
        if (strategy == null) {
            for (Entry<Pattern, ElementInitializationStrategy> entry : PATTERN_STRATEGIES
                    .entrySet()) {
                if (entry.getKey().matcher(attributeName).matches()) {
                    strategy = entry.getValue();
                    break;
                }
            }
        }
        if (strategy == null) {
            strategy = DEFAULT_STRATEGY;
        }
        return strategy;
    }

    private static IdentityHashMap<Pattern, ElementInitializationStrategy> createPatternStrategies() {
        ElementInitializationStrategy attributeStrategy = new AttributeInitializationStrategy();
        IdentityHashMap<Pattern, ElementInitializationStrategy> map = new IdentityHashMap<>(
                1);
        map.put(Pattern.compile("data-.*"), attributeStrategy);
        return map;
    }

    private static Map<String, ElementInitializationStrategy> createStrategies() {
        Map<String, ElementInitializationStrategy> result = new HashMap<>();
        AttributeInitializationStrategy attributeStrategy = new AttributeInitializationStrategy();
        // this is the list of global attributes:
        // https://www.w3schools.com/tags/ref_standardattributes.asp
        result.put("id", attributeStrategy);
        result.put("class", attributeStrategy);
        result.put("style", attributeStrategy);
        result.put("href", attributeStrategy);
        result.put("theme", attributeStrategy);
        result.put("title", attributeStrategy);
        result.put("hidden", attributeStrategy);
        result.put("accesskey", attributeStrategy);
        result.put("contenteditable", attributeStrategy);
        result.put("dir", attributeStrategy);
        result.put("draggable", attributeStrategy);
        result.put("lang", attributeStrategy);
        result.put("spellcheck", attributeStrategy);
        result.put("tabindex", attributeStrategy);
        result.put("translate", attributeStrategy);

        return result;
    }
}

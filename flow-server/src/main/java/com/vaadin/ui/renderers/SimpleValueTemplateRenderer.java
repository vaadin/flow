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
package com.vaadin.ui.renderers;

import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.function.ValueProvider;

/**
 * 
 * Abstract template renderer used as the base implementation for renderers that
 * outputs a simple value in the UI, such as {@link NumberRenderer} and
 * {@link LocalDateRenderer}.
 * <p>
 * It extends {@link TemplateRenderer}, so it expects the components to be
 * stamped in a {@code <template>} element.
 * 
 * @author Vaadin Ltd.
 *
 * @param <SOURCE>
 *            the type of the item used inside the renderer
 * @param <TARGET>
 *            the type of the output object, such as Number or LocalDate
 */
public abstract class SimpleValueTemplateRenderer<SOURCE, TARGET>
        extends TemplateRenderer<SOURCE> {

    private static final AtomicInteger RENDERER_ID_GENERATOR = new AtomicInteger();
    private String template;

    /**
     * Builds a new template renderer using the value provider as the source of
     * values to be rendered.
     * 
     * @param valueProvider
     *            the callback to provide a objects to the renderer, not
     *            <code>null</code>
     */
    protected SimpleValueTemplateRenderer(
            ValueProvider<SOURCE, TARGET> valueProvider) {

        if (valueProvider == null) {
            throw new IllegalArgumentException("valueProvider may not be null");
        }

        int id = RENDERER_ID_GENERATOR.incrementAndGet();
        String propertyName = "_" + getClass().getSimpleName() + "_" + id;
        template = "[[item." + propertyName + "]]";
        withProperty(propertyName,
                value -> getFormattedValue(valueProvider.apply(value)));
    }

    @Override
    public String getTemplate() {
        return template;
    }

    /**
     * Gets the String representation of the target object, to be used inside
     * the template.
     * 
     * @param object
     *            the target object
     * @return the string representation of the object
     */
    protected abstract String getFormattedValue(TARGET object);

}

/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.webcomponent;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.WebComponentExporterFactory;
import com.vaadin.flow.function.SerializableFunction;

/**
 * Retrieves web component tag from a
 * {@link com.vaadin.flow.component.WebComponentExporterFactory} object.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd.
 * @since 2.0
 */
public final class WebComponentExporterTagExtractor implements
        SerializableFunction<WebComponentExporterFactory<? extends Component>, String> {

    @Override
    public String apply(
            WebComponentExporterFactory<? extends Component> factory) {
        return new WebComponentExporter.WebComponentConfigurationFactory()
                .create(factory.create()).getTag();
    }
}

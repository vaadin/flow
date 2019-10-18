/*
 * Copyright 2000-2018 Vaadin Ltd.
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

package com.vaadin.flow.server.webcomponent;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.internal.ExportsWebComponent;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.function.SerializableFunction;

/**
 * Retrieves web component tag from a {@link com.vaadin.flow.component.WebComponentExporter}
 * class.
 *
 * @author Vaadin Ltd.
 * @since 2.0
 */
public final class WebComponentExporterTagExtractor
        implements SerializableFunction<Class<? extends ExportsWebComponent<? extends Component>>, String> {

    @Override
    public String apply(Class<?
            extends ExportsWebComponent<? extends Component>> exporterClass) {
        return new WebComponentExporter.WebComponentConfigurationFactory()
                .create(exporterClass)
                .getTag();
    }
}

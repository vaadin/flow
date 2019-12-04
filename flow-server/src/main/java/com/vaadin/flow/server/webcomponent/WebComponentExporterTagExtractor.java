/*
 * Copyright 2000-2019 Vaadin Ltd.
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
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.WebComponentExporterFactory;
import com.vaadin.flow.function.SerializableFunction;

/**
 * Retrieves web component tag from a
 * {@link com.vaadin.flow.component.WebComponentExporterFactory} object.
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

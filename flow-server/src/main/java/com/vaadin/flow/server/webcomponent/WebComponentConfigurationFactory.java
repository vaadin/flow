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

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.internal.ReflectTools;

/**
 * Produces a web component configuration for the exporter class.
 *
 * @author Vaadin Ltd
 */
public final class WebComponentConfigurationFactory implements Serializable {

    /**
     * Creates a {@link WebComponentConfiguration} from the provided
     * {@link WebComponentExporter} class.
     *
     * @param clazz
     *          exporter class, not {@code null}
     * @return  a web component configuration matching the instance of
     *          received {@code clazz}
     * @throws NullPointerException
     *          when {@code clazz} is {@code null}
     */
    public WebComponentConfiguration<? extends Component> create(Class<?
            extends WebComponentExporter<? extends Component>> clazz) {
        Objects.requireNonNull(clazz, "Parameter 'clazz' cannot be null!");

        WebComponentExporter<? extends Component> exporter = ReflectTools
                .createInstance(clazz);

        return create(exporter);
    }

    /**
     * Creates a {@link WebComponentConfiguration} from the provided
     * {@link WebComponentExporter} instances.
     *
     * @param exporter
     *          exporter instance, not {@code null}
     * @return  a web component configuration matching the instance of
     *          received {@code clazz}
     * @throws NullPointerException
     *          when {@code exporter} is {@code null}
     */
    public WebComponentConfiguration<? extends Component> create(WebComponentExporter<?
            extends Component> exporter) {
        return new WebComponentExporter.WebComponentConfigurationImpl<>(exporter);
    }
}

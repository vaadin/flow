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

package com.vaadin.flow.component;

import java.io.Serializable;

import com.vaadin.flow.component.webcomponent.WebComponentDefinition;

/**
 * Interface for exporting {@link Component components} as embeddable web
 * components. The tag of the exporter web component <b>must be</b> defined
 * using {@link Tag} annotation - otherwise, an exception will be thrown. The
 * tag must be a non-null, non-empty string with dash-separated words, i.e.
 * "dash-separated".
 * <p>
 * The exported web components can be embedded into non-Vaadin
 * applications.
 * <p>
 * Example of exporting {@code MyComponent} component:
 * <pre>
 * {@code
 *@literal @Tag("my-component")
 * public class Exporter implements WebComponentExporter<MyComponent>() {
 *    @literal @Override
 *     public void define(WebComponentDefinition<MyComponent> definition) {
 *         definition.addProperty("name", "John Doe")
 *                 .onChange(MyComponent::setName);
 *     }
 * }
 * </pre>
 *
 * @param <C> type of the component to export
 */
public interface WebComponentExporter<C extends Component> extends Serializable {
    /**
     * Called by the web component export process. Use the given
     * {@link WebComponentDefinition} to define web component's properties,
     * and how the properties interact with the {@link Component} being
     * exported.
     *
     * @param definition instance used to define the component.
     */
    void define(WebComponentDefinition<C> definition);
}

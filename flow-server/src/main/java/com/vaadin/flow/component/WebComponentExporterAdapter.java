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

import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.component.webcomponent.WebComponentDefinition;

/**
 * An abstract adapter class for {@link WebComponentExporter} implementations.
 * <p>
 * The methods in this class are empty. This class exists as convenience for
 * creating exporter objects.
 *
 * @author Vaadin Ltd
 *
 * @param <C>
 *            type of the component to export
 */
public abstract class WebComponentExporterAdapter<C extends Component>
        implements WebComponentExporter<C> {

    @Override
    public void define(WebComponentDefinition<C> definition) {
    }

    @Override
    public void configure(WebComponent<C> webComponent, C component) {
    }

}

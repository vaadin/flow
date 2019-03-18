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

package com.vaadin.flow.component.webcomponent;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.SerializableBiConsumer;

/**
 * IProvides a way to configure the {@link Component} instance created for the
 * exported web component embedded onto a website. An implementation of this
 * interface is given to
 * {@link WebComponentDefinition#setInstanceConfigurator(InstanceConfigurator)},
 * which is received by the user's
 * {@link com.vaadin.flow.component.WebComponentExporter} implementation.
 *
 * @param <C> type of the {@code component} exported as a web component
 */
@FunctionalInterface
public interface InstanceConfigurator<C extends Component> extends SerializableBiConsumer<WebComponent<C>, C> {
    /**
     * If custom initialization for the created {@link Component} instance is
     * needed, it can be done here. It is also possible to configure custom
     * communication between the {@code component} instance and client-side
     * web component using the {@link WebComponent} instance. The {@code
     * webComponent} and {@code component} are in 1-to-1 relation.
     *
     * @param webComponent  instance representing the client-side web
     *                      component instance matching the component
     * @param component     exported component instance
     */
    @Override
    void accept(WebComponent<C> webComponent, C component);
}

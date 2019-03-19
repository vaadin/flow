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

import java.io.Serializable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;

/**
 * Acts as a proxy between the client-side environment and the
 * {@link Component} instance from which web component has been generated. It
 * is assumed that the {@code WebComponentProxy} is embedded into the
 * embedding context's DOM hierarchy as a child of the {@link WebComponentUI}.
 * <p>
 * Proxy receives the {@code component} it is supposed to proxy to from the
 * given {@link WebComponentBinding} by calling {@code getComponent()}.
 */
public interface WebComponentProxy extends HasElement, Serializable {
    /**
     * Sets {@link WebComponentBinding} which offers the component and
     * communication interface to that component.
     *
     * @param binding   binds a {@link WebComponentConfiguration} to a
     *                  {@link Component} instance
     */
    void setWebComponentBinding(WebComponentBinding<? extends Component> binding);
}

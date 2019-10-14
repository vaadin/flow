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
package com.vaadin.flow.component.internal;

import java.io.Serializable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.internal.ReflectTools;

/**
 * Exports a {@link Component} as a web component embeddable in any web page.
 * To export your own web component, do not use this interface directly,
 * instead subclass {@link com.vaadin.flow.component.WebComponentExporter}.
 *
 * @param <C>
 *            type of the component to export
 *
 * @author Vaadin Ltd.
 * @since 2.1
 */
public interface ExportsWebComponent<C extends Component> extends Serializable {

    /**
     * The tag associated with the exported component.
     *
     * @return the tag
     */
    String getTag();

    /**
     * The concrete component class object. By default creates an instance of
     * the actual type parameter.
     *
     * @return component class
     */
    default Class<C> getComponentClass() {
        return (Class<C>) ReflectTools.getGenericInterfaceType(
                this.getClass(), ExportsWebComponent.class);
    }

    /**
     * If custom initialization for the created {@link Component} instance is
     * needed, it can be done here. It is also possible to configure custom
     * communication between the {@code component} instance and client-side web
     * component using the {@link WebComponent} instance. The {@code
     * webComponent} and {@code component} are in 1-to-1 relation.
     * <p>
     * Note that it's incorrect to call any {@code addProperty} method within
     * {@link #configure(WebComponent, Component)} method. All
     * properties have to be configured inside the exporter's constructor.
     *
     * @param webComponent instance representing the client-side web component instance
     *                     matching the component
     * @param component    instance of the exported web component
     */
    default void configure(WebComponent<C> webComponent, C component) {
    }

    /**
     * Always called before {@link #configure(WebComponent, Component)}.
     */
    default void preConfigure() {
    }

    /**
     * Always called after {@link #configure(WebComponent, Component)}.
     */
    default void postConfigure() {
    }
}

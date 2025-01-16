/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import com.vaadin.flow.component.WebComponentExporter.NullTagException;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.internal.ReflectTools;

/**
 * Exports a {@link Component} as a web component.
 * <p>
 * By extending this class you can export a server side {@link Component} with a
 * given tag name so that it can be included in any web page as
 * {@code <tag-name>}. You can add properties/attributes to the element, which
 * are synchronized with the server and you can fire events from the server,
 * which are available as custom events in the browser.
 * <p>
 * The tag name (must contain at least one dash and be unique on the target web
 * page) is provided through the super constructor. Note that the exporter tag
 * is not related to the tag used by the {@link Component} being exported and
 * they cannot be the same.
 * <p>
 * The component class to exported is determined by the parameter given to
 * {@code WebComponentExporterFactory} when extending it, e.g.
 * {@code extends WebComponentExporterFactory<MyComponent>}.
 * <p>
 * You may implement factory instead of direct public implementation of
 * {@link WebComponentExporter} class in case when it's not convenient or not
 * possible to extend an abstract class (e.g. your class already extends another
 * class).
 * <p>
 * NOTE: the implementation class of the object returned by the
 * {@link #create()} method should not be eligible as
 * {@link WebComponentExporter}. Otherwise two instances of the same type will
 * be created which makes a collision. So the implementation class should not be
 * either public or should not have a default no-arguments constructor.
 *
 * @author Vaadin Ltd
 * @see WebComponentExporter
 *
 * @param <C>
 *            type of the component to export
 *
 */
@FunctionalInterface
public interface WebComponentExporterFactory<C extends Component>
        extends Serializable {

    /**
     * Creates a new {@code WebComponentExporter} instance and configures the
     * tag name of the web component created based on this exporter.
     *
     * @see WebComponentExporter#WebComponentExporter(String)
     * @return an exporter instance
     */
    WebComponentExporter<C> create();

    /**
     * Default factory implementation which uses an exporter class to
     * instantiate it using its default constructor.
     *
     * @author Vaadin Ltd
     *
     * @param <C>
     *            type of the component to export
     */
    class DefaultWebComponentExporterFactory<C extends Component>
            implements WebComponentExporterFactory<C> {

        private final Class<? extends WebComponentExporter<C>> exporterClass;

        /**
         * Creates a {@link WebComponentConfiguration} from the provided
         * {@link WebComponentExporter} class.
         *
         * @param exporterClass
         *            exporter class, not {@code null}
         * @throws NullPointerException
         *             when {@code exporterClass} is {@code null}
         */
        public DefaultWebComponentExporterFactory(
                Class<? extends WebComponentExporter<C>> exporterClass) {
            Objects.requireNonNull(exporterClass,
                    "Parameter 'exporterClass' cannot be null!");
            this.exporterClass = exporterClass;
        }

        @Override
        public WebComponentExporter<C> create() {
            try {
                return ReflectTools.createInstance(exporterClass);
            } catch (Exception e) {
                if (e.getCause() != null && e.getCause().getClass()
                        .equals(InvocationTargetException.class)) {
                    Throwable cause2 = e.getCause().getCause();
                    if (cause2 != null && cause2.getClass()
                            .equals(NullTagException.class)) {
                        throw new IllegalArgumentException(String.format(
                                "Unable to construct '%s'! Did "
                                        + "'%s' give null value to "
                                        + "super(String) constructor?",
                                WebComponentConfiguration.class.getSimpleName(),
                                exporterClass.getCanonicalName()), e);
                    }
                }
                // unknown reason, cannot add information
                throw new RuntimeException(
                        "Couldn't create a new instance of class "
                                + exporterClass + " because of "
                                + e.getClass().getSimpleName() + ": "
                                + e.getMessage(),
                        e);
            }
        }

    }

}

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
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.internal.ReflectTools;

/**
 * Produces a web component configuration for the exporter class.
 *
 * @author Vaadin Ltd
 *
 */
public final class WebComponentConfigurationFactory implements Serializable {
    private static final ReentrantLock lock = new ReentrantLock();
    private static boolean creatingConfiguration = false;

    private static WebComponentConfiguration<? extends Component> currentConfiguration;

    /**
     *
     * @param clazz
     * @return
     */
    public static WebComponentConfiguration<? extends Component> create(
            Class<? extends WebComponentExporter<? extends Component>> clazz) {
        Objects.requireNonNull(clazz, "Parameter 'clazz' cannot be null!");

        lock.lock();
        try {
            currentConfiguration = null;
            creatingConfiguration = true;

            // the constructor will call #setConfiguration in order to patch
            // in the WebComponentConfiguration. Exporter instance is not
            // needed (but it is kept alive by the WebComponentConfiguration)
            WebComponentExporter<? extends Component> exporter = ReflectTools
                    .createInstance(clazz);

            if (currentConfiguration == null) {
                throw new IllegalStateException("configuration not set, lol");
            }

            final WebComponentConfiguration<? extends Component> configuration =
                    currentConfiguration;
            creatingConfiguration = false;
            return configuration;
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return
     */
    public static Optional<WebComponentConfiguration<? extends Component>> getPreviousConfiguration() {
        lock.lock();
        try {
            return Optional.ofNullable(currentConfiguration);
        } finally {
            lock.unlock();
        }
    }


    /**
     * @param configuration
     */
    public static void setConfiguration(
            WebComponentConfiguration<? extends Component> configuration) {
        lock.lock();
        try {
            if (!creatingConfiguration) {
                throw new IllegalStateException("setConfiguration can only be" +
                        " called during the execution of #create");
            }
            Objects.requireNonNull(configuration, "Parameter 'configuration' " +
                    "cannot be null!");
            currentConfiguration = configuration;
        } finally {
            lock.unlock();
        }
    }
}

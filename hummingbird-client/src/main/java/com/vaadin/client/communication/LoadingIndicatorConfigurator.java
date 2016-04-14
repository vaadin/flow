/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.client.communication;

import java.util.function.Consumer;

import com.vaadin.client.LoadingIndicator;
import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.namespace.MapNamespace;
import com.vaadin.client.hummingbird.namespace.MapProperty;
import com.vaadin.hummingbird.namespace.LoadingIndicatorConfigurationNamespace;
import com.vaadin.hummingbird.shared.Namespaces;

/**
 * Observes the loading indicator configuration stored in the given node and
 * configures the loading indicator accordingly.
 *
 * @author Vaadin Ltd
 * @since
 */
public class LoadingIndicatorConfigurator {

    private LoadingIndicatorConfigurator() {
        // No instance should ever be created
    }

    /**
     * Observes the given node for loading indicator configuration changes and
     * configures the loading indicator singleton accordingly.
     *
     * @param node
     *            the node containing the loading indicator configuration
     * @param loadingIndicator
     *            the loading indicator to configure
     */
    public static void observe(StateNode node,
            LoadingIndicator loadingIndicator) {
        MapNamespace namespace = node
                .getMapNamespace(Namespaces.LOADING_INDICATOR_CONFIGURATION);

        bindInteger(namespace,
                LoadingIndicatorConfigurationNamespace.FIRST_DELAY_KEY,
                loadingIndicator::setFirstDelay,
                LoadingIndicatorConfigurationNamespace.FIRST_DELAY_DEFAULT);
        bindInteger(namespace,
                LoadingIndicatorConfigurationNamespace.SECOND_DELAY_KEY,
                loadingIndicator::setSecondDelay,
                LoadingIndicatorConfigurationNamespace.SECOND_DELAY_DEFAULT);
        bindInteger(namespace,
                LoadingIndicatorConfigurationNamespace.THIRD_DELAY_KEY,
                loadingIndicator::setThirdDelay,
                LoadingIndicatorConfigurationNamespace.THIRD_DELAY_DEFAULT);
    }

    /**
     * Binds change events for the property identified by the given key in the
     * given namespace to the given setter.
     *
     * @param namespace
     *            the namespace containing the property
     * @param key
     *            the key of the property
     * @param setter
     *            the setter to invoke when the value changes
     * @param defaultValue
     *            the value to use if the property value is removed
     */
    private static void bindInteger(MapNamespace namespace, String key,
            Consumer<Integer> setter, int defaultValue) {
        MapProperty property = namespace.getProperty(key);
        property.addChangeListener(e -> setter
                .accept((int) e.getSource().getValueOrDefault(defaultValue)));
    }

}

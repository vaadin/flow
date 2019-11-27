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
package com.vaadin.client.communication;

import java.util.function.Consumer;

import com.vaadin.client.LoadingIndicator;
import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.flow.internal.nodefeature.LoadingIndicatorConfigurationMap;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;

/**
 * Observes the loading indicator configuration stored in the given node and
 * configures the loading indicator accordingly.
 *
 * @author Vaadin Ltd
 * @since 1.0
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
        NodeMap configMap = node
                .getMap(NodeFeatures.LOADING_INDICATOR_CONFIGURATION);

        bindInteger(configMap, LoadingIndicatorConfigurationMap.FIRST_DELAY_KEY,
                loadingIndicator::setFirstDelay,
                LoadingIndicatorConfigurationMap.FIRST_DELAY_DEFAULT);
        bindInteger(configMap,
                LoadingIndicatorConfigurationMap.SECOND_DELAY_KEY,
                loadingIndicator::setSecondDelay,
                LoadingIndicatorConfigurationMap.SECOND_DELAY_DEFAULT);
        bindInteger(configMap, LoadingIndicatorConfigurationMap.THIRD_DELAY_KEY,
                loadingIndicator::setThirdDelay,
                LoadingIndicatorConfigurationMap.THIRD_DELAY_DEFAULT);

        MapProperty defaultThemeProperty = configMap.getProperty(LoadingIndicatorConfigurationMap.DEFAULT_THEME_APPLIED_KEY);
        defaultThemeProperty.addChangeListener(event -> loadingIndicator.setApplyDefaultTheme(event.getSource().getValueOrDefault(LoadingIndicatorConfigurationMap.DEFAULT_THEME_APPLIED_DEFAULT)));
    }

    /**
     * Binds change events for the property identified by the given key in the
     * given feature to the given setter.
     *
     * @param map
     *            the map containing the property
     * @param key
     *            the key of the property
     * @param setter
     *            the setter to invoke when the value changes
     * @param defaultValue
     *            the value to use if the property value is removed
     */
    private static void bindInteger(NodeMap map, String key,
            Consumer<Integer> setter, int defaultValue) {
        MapProperty property = map.getProperty(key);
        property.addChangeListener(e -> setter
                .accept(e.getSource().getValueOrDefault(defaultValue)));
    }

}

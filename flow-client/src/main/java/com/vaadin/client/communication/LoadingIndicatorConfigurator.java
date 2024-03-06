/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.communication;

import java.util.function.Consumer;

import com.vaadin.client.ConnectionIndicator;
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
     */
    public static void observe(StateNode node) {
        NodeMap configMap = node
                .getMap(NodeFeatures.LOADING_INDICATOR_CONFIGURATION);

        bindInteger(configMap, LoadingIndicatorConfigurationMap.FIRST_DELAY_KEY,
                LoadingIndicatorConfigurator::setFirstDelay,
                LoadingIndicatorConfigurationMap.FIRST_DELAY_DEFAULT);
        bindInteger(configMap,
                LoadingIndicatorConfigurationMap.SECOND_DELAY_KEY,
                LoadingIndicatorConfigurator::setSecondDelay,
                LoadingIndicatorConfigurationMap.SECOND_DELAY_DEFAULT);
        bindInteger(configMap, LoadingIndicatorConfigurationMap.THIRD_DELAY_KEY,
                LoadingIndicatorConfigurator::setThirdDelay,
                LoadingIndicatorConfigurationMap.THIRD_DELAY_DEFAULT);

        MapProperty defaultThemeProperty = configMap.getProperty(
                LoadingIndicatorConfigurationMap.DEFAULT_THEME_APPLIED_KEY);
        defaultThemeProperty.addChangeListener(event -> setApplyDefaultTheme(
                event.getSource().getValueOrDefault(
                        LoadingIndicatorConfigurationMap.DEFAULT_THEME_APPLIED_DEFAULT)));
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

    private static void setFirstDelay(int delay) {
        ConnectionIndicator.setProperty("firstDelay", delay);
    }

    private static void setSecondDelay(int delay) {
        ConnectionIndicator.setProperty("secondDelay", delay);
    }

    private static void setThirdDelay(int delay) {
        ConnectionIndicator.setProperty("thirdDelay", delay);
    }

    private static void setApplyDefaultTheme(boolean apply) {
        ConnectionIndicator.setProperty("applyDefaultTheme", apply);
    }
}

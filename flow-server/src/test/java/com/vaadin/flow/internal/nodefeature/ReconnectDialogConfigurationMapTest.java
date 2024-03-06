/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.nodefeature;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.internal.StateNode;

public class ReconnectDialogConfigurationMapTest
        extends AbstractMapFeatureTest<ReconnectDialogConfigurationMap> {

    private StateNode node = new StateNode(
            ReconnectDialogConfigurationMap.class);
    private final ReconnectDialogConfigurationMap map = new ReconnectDialogConfigurationMap(
            node);

    @Test
    public void defaults() {
        Assert.assertEquals(ReconnectDialogConfigurationMap.DIALOG_TEXT_DEFAULT,
                map.getDialogText());
        Assert.assertEquals(
                ReconnectDialogConfigurationMap.DIALOG_TEXT_GAVE_UP_DEFAULT,
                map.getDialogTextGaveUp());
        Assert.assertEquals(
                ReconnectDialogConfigurationMap.RECONNECT_ATTEMPTS_DEFAULT,
                map.getReconnectAttempts());
        Assert.assertEquals(
                ReconnectDialogConfigurationMap.RECONNECT_INTERVAL_DEFAULT,
                map.getReconnectInterval());
    }

    @Test
    public void setGetDialogText() {
        testString(map, ReconnectDialogConfigurationMap.DIALOG_TEXT_KEY,
                map::setDialogText, map::getDialogText);
    }

    @Test
    public void setGetDialogTextGaveUp() {
        testString(map, ReconnectDialogConfigurationMap.DIALOG_TEXT_GAVE_UP_KEY,
                map::setDialogTextGaveUp, map::getDialogTextGaveUp);
    }

    @Test
    public void setGetReconnectAttempts() {
        testInt(map, ReconnectDialogConfigurationMap.RECONNECT_ATTEMPTS_KEY,
                map::setReconnectAttempts, map::getReconnectAttempts);
    }

    @Test
    public void setGetReconnectInterval() {
        testInt(map, ReconnectDialogConfigurationMap.RECONNECT_INTERVAL_KEY,
                map::setReconnectInterval, map::getReconnectInterval);
    }
}

/*
 * Copyright 2000-2023 Vaadin Ltd.
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

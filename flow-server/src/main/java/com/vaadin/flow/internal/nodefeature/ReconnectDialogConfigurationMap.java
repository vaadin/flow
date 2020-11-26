/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import com.vaadin.flow.component.ReconnectDialogConfiguration;
import com.vaadin.flow.internal.StateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Map for storing the reconnect dialog configuration for a UI.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ReconnectDialogConfigurationMap extends NodeMap
        implements ReconnectDialogConfiguration {

    public static final String DIALOG_TEXT_KEY = "dialogText";
    public static final String DIALOG_TEXT_DEFAULT = "Server connection lost, trying to reconnect...";
    public static final String DIALOG_TEXT_GAVE_UP_KEY = "dialogTextGaveUp";
    public static final String DIALOG_TEXT_GAVE_UP_DEFAULT = "Server connection lost.";
    public static final String RECONNECT_ATTEMPTS_KEY = "reconnectAttempts";
    public static final int RECONNECT_ATTEMPTS_DEFAULT = 10000;
    public static final String RECONNECT_INTERVAL_KEY = "reconnectInterval";
    public static final int RECONNECT_INTERVAL_DEFAULT = 5000;

    /**
     * Creates a new map for the given node.
     *
     * @param node
     *            the node that the map belongs to
     */
    public ReconnectDialogConfigurationMap(StateNode node) {
        super(node);
    }

    @Override
    public String getDialogText() {
        return getOrDefault(DIALOG_TEXT_KEY, DIALOG_TEXT_DEFAULT);
    }

    @Override
    public void setDialogText(String dialogText) {
        put(DIALOG_TEXT_KEY, dialogText);

    }

    @Override
    public String getDialogTextGaveUp() {
        return getOrDefault(DIALOG_TEXT_GAVE_UP_KEY,
                DIALOG_TEXT_GAVE_UP_DEFAULT);
    }

    @Override
    public void setDialogTextGaveUp(String dialogTextGaveUp) {
        put(DIALOG_TEXT_GAVE_UP_KEY, dialogTextGaveUp);
    }

    @Override
    public int getReconnectAttempts() {
        return getOrDefault(RECONNECT_ATTEMPTS_KEY, RECONNECT_ATTEMPTS_DEFAULT);
    }

    @Override
    public void setReconnectAttempts(int reconnectAttempts) {
        put(RECONNECT_ATTEMPTS_KEY, reconnectAttempts);
    }

    @Override
    public int getReconnectInterval() {
        return getOrDefault(RECONNECT_INTERVAL_KEY, RECONNECT_INTERVAL_DEFAULT);
    }

    @Override
    public void setReconnectInterval(int reconnectInterval) {
        put(RECONNECT_INTERVAL_KEY, reconnectInterval);
    }

    @Override
    public int getDialogGracePeriod() {
        getLogger()
                .warn("dialogGracePeriod: this property has been deprecated, "
                        + "and will be permanently removed in Vaadin 20");
        return 0;
    }

    @Override
    public void setDialogGracePeriod(int dialogGracePeriod) {
        getLogger()
                .warn("dialogGracePeriod: this property has been deprecated, "
                        + "and will be permanently removed in Vaadin 20");
    }

    @Override
    public boolean isDialogModal() {
        getLogger()
                .warn("dialogModal: this property has been deprecated, and will"
                        + "be permanently removed in Vaadin 20");
        return false;
    }

    @Override
    public void setDialogModal(boolean dialogModal) {
        getLogger()
                .warn("dialogModal: this property has been deprecated, and will"
                        + "be permanently removed in Vaadin 20");
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(ElementPropertyMap.class);
    }
}

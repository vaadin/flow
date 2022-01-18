/*
 * Copyright 2000-2022 Vaadin Ltd.
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

import static com.vaadin.flow.shared.internal.ReconnectDialogConstants.DIALOG_TEXT_DEFAULT;
import static com.vaadin.flow.shared.internal.ReconnectDialogConstants.DIALOG_TEXT_GAVE_UP_DEFAULT;
import static com.vaadin.flow.shared.internal.ReconnectDialogConstants.DIALOG_TEXT_GAVE_UP_KEY;
import static com.vaadin.flow.shared.internal.ReconnectDialogConstants.DIALOG_TEXT_KEY;
import static com.vaadin.flow.shared.internal.ReconnectDialogConstants.RECONNECT_ATTEMPTS_DEFAULT;
import static com.vaadin.flow.shared.internal.ReconnectDialogConstants.RECONNECT_ATTEMPTS_KEY;
import static com.vaadin.flow.shared.internal.ReconnectDialogConstants.RECONNECT_INTERVAL_DEFAULT;
import static com.vaadin.flow.shared.internal.ReconnectDialogConstants.RECONNECT_INTERVAL_KEY;

import com.vaadin.flow.component.ReconnectDialogConfiguration;
import com.vaadin.flow.internal.StateNode;

/**
 * Map for storing the reconnect dialog configuration for a UI.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ReconnectDialogConfigurationMap extends NodeMap
        implements ReconnectDialogConfiguration {

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
}

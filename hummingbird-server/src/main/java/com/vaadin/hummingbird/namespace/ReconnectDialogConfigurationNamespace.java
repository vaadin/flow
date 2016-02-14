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
package com.vaadin.hummingbird.namespace;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.ui.ReconnectDialogConfiguration;

/**
 * Map for storing the reconnect dialog configuration for a UI.
 *
 * @author Vaadin
 * @since
 */
public class ReconnectDialogConfigurationNamespace extends MapNamespace
        implements ReconnectDialogConfiguration {

    public static final String DIALOG_TEXT_KEY = "dialogText";
    public static final String DIALOG_TEXT_DEFAULT = "Server connection lost, trying to reconnect...";
    public static final String DIALOG_TEXT_GAVE_UP_KEY = "dialogTextGaveUp";
    public static final String DIALOG_TEXT_GAVE_UP_DEFAULT = "Server connection lost.";
    public static final String RECONNECT_ATTEMPTS_KEY = "reconnectAttempts";
    public static final int RECONNECT_ATTEMPTS_DEFAULT = 10000;
    public static final String RECONNECT_INTERVAL_KEY = "reconnectInterval";
    public static final int RECONNECT_INTERVAL_DEFAULT = 5000;
    public static final String DIALOG_GRACE_PERIOD_KEY = "dialogGracePeriod";
    public static final int DIALOG_GRACE_PERIOD_DEFAULT = 400;
    public static final String DIALOG_MODAL_KEY = "dialogModal";
    public static final boolean DIALOG_MODAL_DEFAULT = false;

    /**
     * Creates a new namespace for the given node.
     *
     * @param node
     *            the node that the namespace belongs to
     */
    public ReconnectDialogConfigurationNamespace(StateNode node) {
        super(node);
    }

    @Override
    public String getDialogText() {
        if (!contains(DIALOG_TEXT_KEY)) {
            return DIALOG_TEXT_DEFAULT;
        }
        return (String) get(DIALOG_TEXT_KEY);
    }

    @Override
    public void setDialogText(String dialogText) {
        put(DIALOG_TEXT_KEY, dialogText);

    }

    @Override
    public String getDialogTextGaveUp() {
        if (!contains(DIALOG_TEXT_GAVE_UP_KEY)) {
            return DIALOG_TEXT_GAVE_UP_DEFAULT;
        }
        return (String) get(DIALOG_TEXT_GAVE_UP_KEY);
    }

    @Override
    public void setDialogTextGaveUp(String dialogTextGaveUp) {
        put(DIALOG_TEXT_GAVE_UP_KEY, dialogTextGaveUp);
    }

    @Override
    public int getReconnectAttempts() {
        if (!contains(RECONNECT_ATTEMPTS_KEY)) {
            return RECONNECT_ATTEMPTS_DEFAULT;
        }
        return (int) get(RECONNECT_ATTEMPTS_KEY);
    }

    @Override
    public void setReconnectAttempts(int reconnectAttempts) {
        put(RECONNECT_ATTEMPTS_KEY, reconnectAttempts);
    }

    @Override
    public int getReconnectInterval() {
        if (!contains(RECONNECT_INTERVAL_KEY)) {
            return RECONNECT_INTERVAL_DEFAULT;
        }
        return (int) get(RECONNECT_INTERVAL_KEY);
    }

    @Override
    public void setReconnectInterval(int reconnectInterval) {
        put(RECONNECT_INTERVAL_KEY, reconnectInterval);
    }

    @Override
    public int getDialogGracePeriod() {
        if (!contains(DIALOG_GRACE_PERIOD_KEY)) {
            return DIALOG_GRACE_PERIOD_DEFAULT;
        }
        return (int) get(DIALOG_GRACE_PERIOD_KEY);
    }

    @Override
    public void setDialogGracePeriod(int dialogGracePeriod) {
        put(DIALOG_GRACE_PERIOD_KEY, dialogGracePeriod);
    }

    @Override
    public boolean isDialogModal() {
        if (!contains(DIALOG_MODAL_KEY)) {
            return DIALOG_MODAL_DEFAULT;
        }
        return (boolean) get(DIALOG_MODAL_KEY);
    }

    @Override
    public void setDialogModal(boolean dialogModal) {
        put(DIALOG_MODAL_KEY, dialogModal);
    }

}

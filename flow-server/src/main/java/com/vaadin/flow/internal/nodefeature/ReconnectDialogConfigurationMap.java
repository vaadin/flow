/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.nodefeature;

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

    public static final String DIALOG_TEXT_KEY = "dialogText";
    public static final String DIALOG_TEXT_DEFAULT = "Connection lost, trying to reconnect...";
    public static final String DIALOG_TEXT_GAVE_UP_KEY = "dialogTextGaveUp";
    public static final String DIALOG_TEXT_GAVE_UP_DEFAULT = "Connection lost";
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
}

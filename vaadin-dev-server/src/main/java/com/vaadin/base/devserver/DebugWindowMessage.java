/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.base.devserver;

import java.io.Serializable;

/**
 * A message sent to the debug window.
 */
public class DebugWindowMessage implements Serializable {

    private final String command;
    private final Object data;

    /**
     * Creates a new instance.
     *
     * @param command
     *            the command to send
     * @param data
     *            the data that is specific to the command
     */
    public DebugWindowMessage(String command, Object data) {
        this.command = command;
        this.data = data;

    }

    public String getCommand() {
        return command;
    }

    public Object getData() {
        return data;
    }
}

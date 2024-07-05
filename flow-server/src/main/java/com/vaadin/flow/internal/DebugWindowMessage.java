/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.internal;

import java.io.Serializable;

/**
 * A message sent to the debug window.
 */
public class DebugWindowMessage implements Serializable {

    private final String command;
    private final DebugWindowData data;

    /**
     * Creates a new instance.
     *
     * @param command
     *            the command to send
     * @param data
     *            the data that is specific to the command
     */
    public DebugWindowMessage(String command, DebugWindowData data) {
        this.command = command;
        this.data = data;

    }

    public String getCommand() {
        return command;
    }

    public DebugWindowData getData() {
        return data;
    }

    public String toJson() {
        return String.format("{\"command\": \"%s\", \"data\": %s}", command,
                data.toJson());
    }
}

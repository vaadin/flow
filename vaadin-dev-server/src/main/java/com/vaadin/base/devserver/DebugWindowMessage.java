/*
 * Copyright 2000-2025 Vaadin Ltd.
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

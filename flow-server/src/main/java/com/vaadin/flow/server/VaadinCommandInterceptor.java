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

package com.vaadin.flow.server;

import java.io.Serializable;
import java.util.Map;

/**
 * Used to provide an around-like aspect option around command processing.
 *
 * @author Marcin Grzejszczak
 * @since 24.2
 */
public interface VaadinCommandInterceptor extends Serializable {

    /**
     * Called when command is about to be started.
     *
     * @param context
     *            mutable map passed between methods of this interceptor
     * @param command
     *            command
     */
    void commandExecutionStart(Map<Object, Object> context, Command command);

    /**
     * Called when an exception occurred
     *
     * @param context
     *            mutable map passed between methods of this interceptor
     * @param command
     *            command
     * @param t
     *            exception
     */
    void handleException(Map<Object, Object> context, Command command, Exception t);

    /**
     * Called at the end of processing a command. Will be called
     * regardless of whether there was an exception or not.
     *
     * @param context
     *            mutable map passed between methods of this interceptor
     * @param command
     *            command
     */
    void commandExecutionEnd(Map<Object, Object> context, Command command);
}

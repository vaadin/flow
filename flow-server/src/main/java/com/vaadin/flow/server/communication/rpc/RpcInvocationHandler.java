/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.server.communication.rpc;

import java.io.Serializable;
import java.util.Optional;

import com.vaadin.flow.component.UI;

import elemental.json.JsonObject;

/**
 * RPC invocation handler interface.
 * <p>
 * Each instance must return unique rpc type (see {@link #getRpcType()} and
 * handle a {@link JsonObject} RPC data using {@link #handle(UI, JsonObject)}
 * method.
 * 
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public interface RpcInvocationHandler extends Serializable {

    /**
     * Gets unique RPC type which this handler is applicable for.
     * 
     * @return the unique rpc type
     */
    String getRpcType();

    /**
     * Handles RPC data {@code invocationJson} using {@code ui} as a context.
     * 
     * @param ui
     *            the UI to handle against, not {@code null}
     * @param invocationJson
     *            the RPC data to handle, not {@code null}
     * @return an optional runnable
     */
    Optional<Runnable> handle(UI ui, JsonObject invocationJson);

}

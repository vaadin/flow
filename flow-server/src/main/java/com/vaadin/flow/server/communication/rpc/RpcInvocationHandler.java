/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
 * <p>
 * For internal use only. May be renamed or removed in a future release.
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

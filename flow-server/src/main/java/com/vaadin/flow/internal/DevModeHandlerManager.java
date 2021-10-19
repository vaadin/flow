/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.flow.internal;

import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.VaadinInitializerException;

/**
 * Provides API to access to the {@link DevModeHandler} instance by a
 * {@link VaadinService}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
public interface DevModeHandlerManager {

    /**
     * The annotations the dev mode handler is interested in having scanned from
     * the class path.
     *
     * @return an array of types the dev mode handler is interested in
     */
    Class<?>[] getHandlesTypes();

    /**
     * Initialize the dev mode server if not in production mode.
     *
     * @param classes
     *            classes to check for npm and js modules
     * @param context
     *            VaadinContext we are running in
     *
     * @throws VaadinInitializerException
     *             if the dev mode server cannot be initialized
     */
    void initDevModeHandler(Set<Class<?>> classes, VaadinContext context)
            throws VaadinInitializerException;

    /**
     * Returns a {@link DevModeHandler} instance for the given {@code service}.
     *
     * @return a {@link DevModeHandler} instance
     */
    DevModeHandler getDevModeHandler();

    /**
     * Gets the {@link DevModeHandler}.
     *
     * @param service
     *            a Vaadin service
     * @return an {@link Optional} containing a {@link DevModeHandler} instance
     *         or <code>EMPTY</code> if disabled
     */
    static Optional<DevModeHandler> getDevModeHandler(VaadinService service) {
        return getDevModeHandler(service.getContext());
    }

    /**
     * Gets the {@link DevModeHandler}.
     *
     * @param context
     *            the Vaadin context
     * @return an {@link Optional} containing a {@link DevModeHandler} instance
     *         or <code>EMPTY</code> if disabled
     */
    static Optional<DevModeHandler> getDevModeHandler(VaadinContext context) {
        return Optional.ofNullable(context)
                .map(ctx -> ctx.getAttribute(Lookup.class))
                .map(lu -> lu.lookup(DevModeHandlerManager.class))
                .flatMap(dmha -> Optional.ofNullable(dmha.getDevModeHandler()));
    }
}

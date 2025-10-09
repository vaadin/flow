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
package com.vaadin.flow.hotswap;

import java.util.Set;

import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

/**
 * Implementor ot this interface are responsible for update Vaadin components
 * when application classes change.
 * <p>
 * Listener instances are by default discovered using Flow
 * {@link com.vaadin.flow.di.Lookup} mechanisms. Implementors are usually
 * discovered and instantiated using {@link java.util.ServiceLoader}, meaning
 * that all implementations must have a zero-argument constructor and the fully
 * qualified name of the implementation class must be listed on a separate line
 * in a META-INF/services/com.vaadin.flow.hotswap.VaadinHotSwapper file present
 * in the jar file containing the implementation class. Integrations for
 * specific runtime environments, such as Spring and CDI, might also provide
 * other ways of discovering implementors.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 24.5
 */
public interface VaadinHotswapper {

    /**
     * Called by Vaadin hotswap entry point when one or more application classes
     * have been updated.
     * <p>
     *
     * This method is meant to perform application-wide updates. Operation
     * targeting Vaadin session should be implemented in
     * {@link #onClassLoadEvent(VaadinSession, Set, boolean)} method.
     *
     * @param vaadinService
     *            active {@link VaadinService} instance.
     * @param classes
     *            the set of changed classes.
     * @param redefined
     *            {@literal true} if the classes have been redefined by hotswap
     *            mechanism, {@literal false} if they have been loaded for the
     *            first time by the ClassLoader.
     * @return {@literal true} if a browser page reload is required,
     *         {@literal false} otherwise.
     * @see #onClassLoadEvent(VaadinSession, Set, boolean)
     */
    default boolean onClassLoadEvent(VaadinService vaadinService,
            Set<Class<?>> classes, boolean redefined) {
        // no-op by default
        return false;
    }

    /**
     * Called by Vaadin hotswap entry point when one or more application classes
     * have been updated.
     * <p>
     *
     * This method is meant to perform updates at {@link VaadinSession} level.
     * Operation targeting the entire application should be implemented in
     * {@link #onClassLoadEvent(VaadinService, Set, boolean)} method.
     *
     * @param vaadinSession
     *            the {@link VaadinSession} to be potentially updated.
     * @param classes
     *            the set of changed classes.
     * @param redefined
     *            {@literal true} if the classes have been redefined by hotswap
     *            mechanism, {@literal false} if they have been loaded for the
     *            first time by the ClassLoader.
     * @return {@literal true} if a browser page reload is required,
     *         {@literal false} otherwise.
     * @see #onClassLoadEvent(VaadinService, Set, boolean)
     */
    default boolean onClassLoadEvent(VaadinSession vaadinSession,
            Set<Class<?>> classes, boolean redefined) {
        // no-op by default
        return false;
    }

    /**
     * Called by Vaadin hotswap entry point after all hotswap related operations
     * have been completed.
     *
     * @param event
     *            an event containing information about the hotswap operation.
     */
    default void onHotswapComplete(HotswapCompleteEvent event) {
        // no-op by default
        return;
    }

}

/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.router.legacy;

import java.io.Serializable;
import java.util.Optional;

import com.vaadin.flow.router.NavigationHandler;
import com.vaadin.flow.router.NavigationEvent;

/**
 * Resolves the details in a navigation event to find a handler for the event.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
@FunctionalInterface
public interface Resolver extends Serializable {
    /**
     * Resolves the details in the given navigation event to find a handler for
     * the event.
     *
     * @param navigationEvent
     *            the navigation event to resolve
     * @return an optional navigation handler that should handle the event or an
     *         empty optional if no handler matched the event
     */
    Optional<NavigationHandler> resolve(NavigationEvent navigationEvent);
}

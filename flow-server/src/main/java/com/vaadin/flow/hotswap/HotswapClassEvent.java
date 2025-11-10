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

import java.util.Objects;
import java.util.Set;

import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;

/**
 * Event object passed to {@link VaadinHotswapper} implementations during
 * hotswap processing of Java classes.
 * <p>
 * This event provides methods for {@link VaadinHotswapper} implementations to:
 * <ul>
 * <li>Trigger UI updates with {@link #triggerUpdate(UIUpdateStrategy)} or
 * {@link #triggerUpdate(UI, UIUpdateStrategy)}</li>
 * <li>Update client-side resources via
 * {@link #updateClientResource(String, String)}</li>
 * <li>Send Hot Module Replacement messages via
 * {@link #sendHMRMessage(String, JsonNode)}</li>
 * </ul>
 * <p>
 * The event enforces a priority system where {@link UIUpdateStrategy#RELOAD}
 * takes precedence over {@link UIUpdateStrategy#REFRESH}. Once a RELOAD
 * strategy is set (either globally or for a specific UI), it cannot be changed
 * to REFRESH. This ensures that critical updates requiring a full page reload
 * are not downgraded to partial refreshes.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.0
 */
public class HotswapClassEvent extends HotswapEvent {
    private final Set<Class<?>> classes;
    private final boolean redefined;

    /**
     * Creates a new hotswap class event.
     *
     * @param vaadinService
     *            the active {@link VaadinService} instance
     * @param classes
     *            the set of classes that were updated
     * @param redefined
     *            {@literal true} if the classes have been redefined by the
     *            hotswap mechanism, {@literal false} if they have been loaded
     *            for the first time by the ClassLoader
     */
    public HotswapClassEvent(VaadinService vaadinService, Set<Class<?>> classes,
            boolean redefined) {
        super(vaadinService);
        this.classes = Set.copyOf(Objects.requireNonNull(classes,
                "Changed classes cannot be null"));
        this.redefined = redefined;
    }

    /**
     * Retrieves the set of classes that were modified during a hotswap
     * operation.
     *
     * @return a set of {@link Class} objects representing the classes that have
     *         been updated
     */
    public final Set<Class<?>> getChangedClasses() {
        return classes;
    }

    /**
     * Determines if the classes have been redefined by the hotswap mechanism.
     *
     * @return true if the classes have been redefined, false if they were
     *         loaded for the first time
     */
    public final boolean isRedefined() {
        return redefined;
    }

}

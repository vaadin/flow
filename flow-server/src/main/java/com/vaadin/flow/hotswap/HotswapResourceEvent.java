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

import java.net.URI;
import java.util.Objects;
import java.util.Set;

import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;

/**
 * Event object passed to {@link VaadinHotswapper} implementations during
 * hotswap processing of resources.
 * <p>
 * The type of modification (creation, modification, deletion) is not considered
 * because IDEs may triggers multiple events for a single file; for example, a
 * modification will trigger a creation and a deletion event.
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
public class HotswapResourceEvent extends HotswapEvent {

    private final Set<URI> changedResources;

    /**
     * Creates a new hotswap class event.
     *
     * @param vaadinService
     *            the active {@link VaadinService} instance
     * @param changedResources
     *            the set of resources that were updated
     */
    public HotswapResourceEvent(VaadinService vaadinService,
            Set<URI> changedResources) {
        super(vaadinService);
        this.changedResources = Set.copyOf(Objects.requireNonNull(
                changedResources, "Changed resources cannot be null"));
    }

    public Set<URI> getChangedResources() {
        return changedResources;
    }
}

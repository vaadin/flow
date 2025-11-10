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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.server.VaadinService;

/**
 * Represents an event that facilitates hot-swapping operations, such as
 * dynamically updating resources or triggering client-side updates, in a Vaadin
 * application.
 * <p>
 * The {@code HotswapEvent} class provides mechanisms for dynamically refreshing
 * or reloading client-side resources, managing update strategies for UIs, and
 * sending Hot Module Replacement (HMR) notifications to the client. This class
 * abstracts common functionalities required for live-reload scenarios.
 * <p>
 * Subclasses should provide specific implementations or additional features for
 * handling custom behaviors associated with these events.
 */
abstract class HotswapEvent {
    protected final VaadinService vaadinService;
    private final Map<UI, UIUpdateStrategy> uiUpdateStrategies = new IdentityHashMap<>();
    private final List<ClientCommand> clientCommands = new ArrayList<>();
    private UIUpdateStrategy globalUIUpdateStrategy;

    public HotswapEvent(VaadinService vaadinService) {
        this.vaadinService = Objects.requireNonNull(vaadinService,
                "VaadinService cannot be null");
    }

    /**
     * Sets the global UI update strategy for all UIs.
     * <p>
     * This method sets the update strategy that will be applied to all UIs. The
     * {@link UIUpdateStrategy#RELOAD} strategy has priority and cannot be
     * changed once set. If the current strategy is already RELOAD, calling this
     * method with REFRESH will have no effect.
     *
     * @param uiUpdateStrategy
     *            the UI update strategy to set
     */
    public final void triggerUpdate(UIUpdateStrategy uiUpdateStrategy) {
        Objects.requireNonNull(uiUpdateStrategy,
                "UI update strategy cannot be null");
        if (this.globalUIUpdateStrategy != UIUpdateStrategy.RELOAD) {
            this.globalUIUpdateStrategy = uiUpdateStrategy;
        }
    }

    /**
     * Sets the UI update strategy for a specific UI.
     * <p>
     * This method sets the update strategy for an individual UI. The
     * {@link UIUpdateStrategy#RELOAD} strategy has priority and cannot be
     * changed once set for a specific UI. The first call for a given UI sets
     * the strategy, and subsequent calls will only be honored if attempting to
     * upgrade from REFRESH to RELOAD.
     * <p>
     * NOTE: setting the strategy is a hint for {@link Hotswapper} that can
     * however decide to perform a full page reload based on strategies selected
     * for other UIs by any other event users.
     *
     * @param ui
     *            the UI to set the update strategy for
     * @param uiUpdateStrategy
     *            the UI update strategy to set
     */
    public final void triggerUpdate(UI ui, UIUpdateStrategy uiUpdateStrategy) {
        Objects.requireNonNull(ui, "UI cannot be null");
        Objects.requireNonNull(uiUpdateStrategy,
                "UI update strategy cannot be null");
        uiUpdateStrategies.compute(ui, (k, existing) -> {
            if (existing == UIUpdateStrategy.RELOAD) {
                return UIUpdateStrategy.RELOAD;
            }
            return uiUpdateStrategy;
        });
    }

    /**
     * Updates a client-side resource for Hot Module Replacement (HMR).
     * <p>
     * This method registers a client resource that should be updated in the
     * browser without requiring a full page reload. The path identifies the
     * resource to update, and the content provides the new resource content.
     * <p>
     * Note: This method delegates to the BrowserLiveReload instance and is
     * provided as a convenience to avoid requiring VaadinHotswapper
     * implementations to use Lookup directly.
     *
     * @param path
     *            the path of the resource to update, must not be null or empty
     * @param content
     *            the new content for the resource, can be null to indicate
     *            resource deletion
     * @throws IllegalArgumentException
     *             if path is null or empty
     */
    public final void updateClientResource(String path, String content) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path cannot be null or empty");
        }
        clientCommands.add(new ClientResource(path, content));
    }

    /**
     * Sends a Hot Module Replacement (HMR) message to the client.
     * <p>
     * This method queues a custom HMR message to be sent to the browser. HMR
     * messages can be used to notify the client about specific changes that
     * require custom handling beyond standard resource updates.
     * <p>
     * Note: This method delegates to the BrowserLiveReload instance and is
     * provided as a convenience to avoid requiring VaadinHotswapper
     * implementations to use Lookup directly.
     *
     * @param event
     *            the event name/type, must not be null or empty
     * @param eventData
     *            additional data for the event, can be null if no additional
     *            data is needed
     * @throws IllegalArgumentException
     *             if event is null or empty
     */
    public final void sendHMRMessage(String event, JsonNode eventData) {
        if (event == null || event.isBlank()) {
            throw new IllegalArgumentException("event cannot be null or empty");
        }
        clientCommands.add(new HMRMessage(event, eventData));
    }

    /**
     * Retrieves the active instance of {@link VaadinService} associated with
     * the event.
     *
     * @return the current {@link VaadinService} instance, which provides
     *         various services and functionalities required to manage Vaadin
     *         applications.
     */
    public final VaadinService getVaadinService() {
        return vaadinService;
    }

    /**
     * Determines whether a full page reload is required based on the current
     * global UI update strategy.
     *
     * @return true if the global UI update strategy is set to
     *         {@code UIUpdateStrategy.RELOAD}, indicating that a full page
     *         reload is needed; false otherwise
     */
    public boolean requiresPageReload() {
        return globalUIUpdateStrategy == UIUpdateStrategy.RELOAD;
    }

    /**
     * Determines whether any of the active UIs requires a full page reload,
     * based on global or specific UI update strategies.
     *
     * @return true if any UI needs a page reload; false otherwise
     */
    public boolean anyUIRequiresPageReload() {
        return globalUIUpdateStrategy == UIUpdateStrategy.RELOAD
                || uiUpdateStrategies.containsValue(UIUpdateStrategy.RELOAD);
    }

    /**
     * Retrieves the UI update strategy for a specific UI, or the global UI
     * update strategy if none is set for the given UI.
     *
     * @param ui
     *            the UI for which the update strategy is to be retrieved
     * @return an {@link Optional} containing the {@link UIUpdateStrategy} for
     *         the specified UI, or the global UI update strategy if no specific
     *         strategy is found; an empty {@link Optional} if no strategy is
     *         defined
     */
    public final Optional<UIUpdateStrategy> getUIUpdateStrategy(UI ui) {
        return Optional.ofNullable(uiUpdateStrategies.get(ui))
                .or(() -> Optional.ofNullable(globalUIUpdateStrategy));
    }

    /**
     * Applies the queued client commands to the provided BrowserLiveReload
     * instance. Each command in the {@code clientCommands} collection is
     * executed with the given {@code browserLiveReload} parameter.
     *
     * @param browserLiveReload
     *            the BrowserLiveReload instance used to apply client commands
     */
    final void applyClientCommands(BrowserLiveReload browserLiveReload) {
        clientCommands.forEach(update -> update.apply(browserLiveReload));
    }

    /**
     * Represents an operation that can be executed on an associated
     * {@link BrowserLiveReload} instance.
     * <p>
     * This interface is used to encapsulate client-side commands that need to
     * be applied dynamically during a hot module replacement (HMR) or other
     * live-reload scenarios. Implementations of this interface define specific
     * actions to be performed on the client through the
     * {@link BrowserLiveReload}.
     * <p>
     * The {@code apply} method must be implemented by subclasses to define the
     * specific command to be executed.
     */
    private interface ClientCommand {
        void apply(BrowserLiveReload browserLiveReload);
    }

    /**
     * Package-private record handling a client resource path and its content
     * for Hot Module Replacement.
     *
     * @param path
     *            the resource path
     * @param content
     *            the resource content; can be null
     */
    private record ClientResource(String path,
            String content) implements ClientCommand {
        @Override
        public void apply(BrowserLiveReload browserLiveReload) {
            browserLiveReload.update(path, content);
        }
    }

    /**
     * Package-private record handling a Hot Module Replacement message event
     * name and associated data.
     *
     * @param event
     *            the event name/type
     * @param eventData
     *            the event data; can be null
     */
    private record HMRMessage(String event,
            JsonNode eventData) implements ClientCommand {
        @Override
        public void apply(BrowserLiveReload browserLiveReload) {
            browserLiveReload.sendHmrEvent(event, eventData);
        }
    }

    /**
     * Merges the given HotswapEvent into the current event instance by applying
     * its update strategies and client commands.
     * <p>
     * This method performs the following operations: 1. Uses the global UI
     * update strategy from the given event, if present, to trigger an update.
     * 2. Iterates through individual UI update strategies in the given event
     * and triggers updates for each. 3. Appends all client commands from the
     * given event to the current event's client commands collection.
     *
     * @param event
     *            the HotswapEvent containing update strategies and client
     *            commands to merge, must not be null
     */
    void merge(HotswapEvent event) {
        if (event.globalUIUpdateStrategy != null) {
            triggerUpdate(event.globalUIUpdateStrategy);
        }
        event.uiUpdateStrategies.forEach(this::triggerUpdate);
        clientCommands.addAll(event.clientCommands);
    }
}

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
package com.vaadin.flow.component.page;

import java.io.Serializable;
import java.util.EventObject;
import java.util.Optional;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.shared.ApplicationConstants;

import elemental.json.JsonValue;

/**
 * Represents <code>window.history</code> in the browser. See e.g.
 * <a href="https://developer.mozilla.org/en-US/docs/Web/API/History_API">
 * documentation on MDN</a> for detailed information on how the API works in the
 * browser.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class History implements Serializable {

    /**
     * Event fired when the history state has changed.
     * <p>
     * This happens when <code>PopStateEvent</code> is fired in the browser, or
     * when routing has been triggered by user clicking a link marked with
     * attribute {@value ApplicationConstants#ROUTER_LINK_ATTRIBUTE}.
     * <p>
     * Note that this event is not fired when only the hash has changed!
     */
    public static class HistoryStateChangeEvent extends EventObject {
        private final Location location;
        private final transient JsonValue state;
        private final NavigationTrigger trigger;

        /**
         * Creates a new event.
         *
         * @param history
         *            the history instance that fired the event, not
         *            <code>null</code>
         * @param state
         *            the history state from the browser, <code>null</code> if
         *            no state was provided
         * @param location
         *            the new browser location, not <code>null</code>
         * @param trigger
         *            the type of user action that triggered this history
         *            change, not <code>null</code>
         */
        public HistoryStateChangeEvent(History history, JsonValue state,
                Location location, NavigationTrigger trigger) {
            super(history);

            assert location != null;
            assert trigger != null;

            this.location = location;
            this.state = state;
            this.trigger = trigger;
        }

        @Override
        public History getSource() {
            return (History) super.getSource();
        }

        /**
         * Gets the location that was opened. This is relative to the base url.
         *
         * @return the location, not null
         */
        public Location getLocation() {
            return location;
        }

        /**
         * Gets the history state value as JSON.
         *
         * @return an optional JSON state value or an empty optional if no state
         *         has been provided
         */
        public Optional<JsonValue> getState() {
            return Optional.ofNullable(state);
        }

        /**
         * Gets the type of user action that triggered this history change.
         *
         * @return the type of user action that triggered this history change,
         *         not <code>null</code>
         */
        public NavigationTrigger getTrigger() {
            return trigger;
        }
    }

    /**
     * Handles location change events.
     *
     * @see History.HistoryStateChangeEvent
     */
    @FunctionalInterface
    public interface HistoryStateChangeHandler extends Serializable {
        /**
         * Invoked when a history state change event is fired.
         *
         * @param event
         *            the event
         */
        void onHistoryStateChange(HistoryStateChangeEvent event);
    }

    private final UI ui;
    private HistoryStateChangeHandler historyStateChangeHandler;

    /**
     * Creates a history API endpoint for the given UI.
     *
     * @param ui
     *            the ui, not null
     */
    public History(UI ui) {
        assert ui != null;
        this.ui = ui;
    }

    /**
     * Gets the UI that this instance belongs to.
     *
     * @return the ui, not null
     */
    public UI getUI() {
        return ui;
    }

    /**
     * Invokes <code>history.pushState</code> in the browser with the given
     * parameters. This is a shorthand method for
     * {@link History#pushState(JsonValue, Location)}, creating {@link Location}
     * from the string provided.
     *
     * @param state
     *            the JSON state to push to the history stack, or
     *            <code>null</code> to only change the location
     * @param location
     *            the new location to set in the browser, or <code>null</code>
     *            to only change the JSON state
     */
    public void pushState(JsonValue state, String location) {
        pushState(state,
                Optional.ofNullable(location).map(Location::new).orElse(null));
    }

    /**
     * Invokes <code>history.pushState</code> in the browser with the given
     * parameters.
     *
     * @param state
     *            the JSON state to push to the history stack, or
     *            <code>null</code> to only change the location
     * @param location
     *            the new location to set in the browser, or <code>null</code>
     *            to only change the JSON state
     */
    public void pushState(JsonValue state, Location location) {
        // Second parameter is title which is currently ignored according to
        // https://developer.mozilla.org/en-US/docs/Web/API/History_API
        ui.getPage().executeJs("history.pushState($0, '', $1)", state,
                location.getPathWithQueryParameters());
    }

    /**
     * Invokes <code>history.replaceState</code> in the browser with the given
     * parameters. This is a shorthand method for
     * {@link History#replaceState(JsonValue, Location)}, creating
     * {@link Location} from the string provided.
     *
     * @param state
     *            the JSON state to push to the history stack, or
     *            <code>null</code> to only change the location
     * @param location
     *            the new location to set in the browser, or <code>null</code>
     *            to only change the JSON state
     */
    public void replaceState(JsonValue state, String location) {
        replaceState(state,
                Optional.ofNullable(location).map(Location::new).orElse(null));
    }

    /**
     * Invokes <code>history.replaceState</code> in the browser with the given
     * parameters.
     *
     * @param state
     *            the JSON state to push to the history stack, or
     *            <code>null</code> to only change the location
     * @param location
     *            the new location to set in the browser, or <code>null</code>
     *            to only change the JSON state
     */
    public void replaceState(JsonValue state, Location location) {
        // Second parameter is title which is currently ignored according to
        // https://developer.mozilla.org/en-US/docs/Web/API/History_API
        ui.getPage().executeJs("history.replaceState($0, '', $1)",
                state, location.getPathWithQueryParameters());
    }

    /**
     * Sets a handler that will be notified when the history state has changed.
     * <p>
     * History state changes are triggered when a <code>popstate</code> event is
     * fired in the browser or when the user has navigated using a router link.
     * There can only be one handler at a time.
     *
     * @param historyStateChangeHandler
     *            the handler to set, or <code>null</code> to remove the current
     *            handler
     * @see History.HistoryStateChangeEvent
     */
    public void setHistoryStateChangeHandler(
            HistoryStateChangeHandler historyStateChangeHandler) {
        this.historyStateChangeHandler = historyStateChangeHandler;
    }

    /**
     * Gets the handler that is notified history state has changed.
     *
     * @return the history state handler, or <code>null</code> if no handler is
     *         set
     * @see History.HistoryStateChangeEvent
     */
    public HistoryStateChangeHandler getHistoryStateChangeHandler() {
        return historyStateChangeHandler;
    }

    /**
     * Navigates back. This has the same effect as if the user would press the
     * back button in the browser. This causes a {@link HistoryStateChangeEvent}
     * to be fired asynchronously if the conditions described in the <a href=
     * "https://developer.mozilla.org/en-US/docs/Web/API/WindowEventHandlers/onpopstate">
     * onpopstate documentation</a> are met.
     */
    public void back() {
        ui.getPage().executeJs("history.back()");
    }

    /**
     * Navigates forward. This has the same effect as if the user would press
     * the forward button in the browser. This causes a
     * {@link HistoryStateChangeEvent} to be fired asynchronously if the
     * conditions described in the <a href=
     * "https://developer.mozilla.org/en-US/docs/Web/API/WindowEventHandlers/onpopstate">
     * onpopstate documentation</a> are met.
     */
    public void forward() {
        ui.getPage().executeJs("history.forward()");
    }

    /**
     * Navigates a number of steps forward or backward in the history. This has
     * the same effect as if the user would press the forward button in the
     * browser. This causes a {@link HistoryStateChangeEvent} to be fired
     * asynchronously if the conditions described in the <a href=
     * "https://developer.mozilla.org/en-US/docs/Web/API/WindowEventHandlers/onpopstate">
     * onpopstate documentation</a> are met.
     *
     * @param steps
     *            the number of steps to navigate, positive numbers navigate
     *            forward, negative numbers backward. <code>0</code> causes the
     *            current page to be reloaded
     */
    public void go(int steps) {
        ui.getPage().executeJs("history.go($0)", steps);
    }
}

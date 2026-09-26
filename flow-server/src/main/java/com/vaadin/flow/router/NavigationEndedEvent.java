/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.router;

import java.io.Serializable;
import java.util.EventObject;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;

/**
 * Event fired through the {@link VaadinService#getEventBus() service event bus}
 * when the server has finished handling a navigation for a UI, telling how it
 * ended.
 * <p>
 * It is always fired after the matching {@link NavigationStartedEvent}, on the
 * same thread, also when the navigation throws. A listener can therefore keep
 * timing state in a {@link ThreadLocal}. Both events are fired on the request
 * thread while the session is locked. The listeners of this event are called in
 * the reverse order of registration, so that they nest around the listeners of
 * the started event.
 * <p>
 * Only the outermost navigation is reported. A
 * {@link BeforeEvent#forwardTo(String) forward}, a
 * {@link BeforeEvent#rerouteTo(String) reroute}, the redirect that adds or
 * removes a trailing slash and the rendering of an error view are part of the
 * navigation that caused them and fire no events of their own. Their effect is
 * in the {@link #getOutcome() outcome}.
 * <p>
 * Use a {@code switch} over the outcome to tell the cases apart:
 *
 * <pre>
 * String result = switch (event.getOutcome()) {
 * case Completed completed -&gt; completed.navigationTarget().getSimpleName();
 * case Postponed postponed -&gt; "postponed";
 * case Failed failed -&gt; "failed: " + failed.error();
 * case NotShown notShown -&gt; "no view shown";
 * };
 * </pre>
 *
 * @see NavigationStartedEvent
 */
public class NavigationEndedEvent extends EventObject {

    /**
     * How a navigation ended.
     */
    public sealed interface Outcome extends Serializable
            permits Completed, Postponed, Failed, NotShown {
    }

    /**
     * The navigation showed a view.
     * <p>
     * The navigation target is the view that is shown after all forwards and
     * reroutes, so it can differ from the view the requested location resolves
     * to.
     *
     * @param navigationTarget
     *            the class of the view that is shown, not {@code null}
     */
    public record Completed(
            Class<? extends Component> navigationTarget) implements Outcome {

        /**
         * Creates a new outcome.
         *
         * @param navigationTarget
         *            the class of the view that is shown, not {@code null}
         */
        public Completed {
            Objects.requireNonNull(navigationTarget,
                    "navigationTarget cannot be null");
        }
    }

    /**
     * A {@link BeforeLeaveEvent} listener of the current view postponed the
     * navigation, so the current view is still shown.
     * <p>
     * If the navigation is resumed later with
     * {@link BeforeLeaveEvent.ContinueNavigationAction#proceed()}, that is not
     * reported as a new navigation.
     */
    public record Postponed() implements Outcome {
    }

    /**
     * The navigation failed.
     * <p>
     * In most cases Flow has shown an error view for the error, such as the
     * "not found" view for a {@link NotFoundException}. When the navigation
     * throws instead, for example because there is no error view for the
     * exception, the error is reported here and then thrown on.
     *
     * @param error
     *            the error that made the navigation fail, not {@code null}
     */
    public record Failed(Throwable error) implements Outcome {

        /**
         * Creates a new outcome.
         *
         * @param error
         *            the error that made the navigation fail, not {@code null}
         */
        public Failed {
            Objects.requireNonNull(error, "error cannot be null");
        }
    }

    /**
     * The server finished handling the navigation without showing a view, and
     * the view shown before is still in the UI.
     * <p>
     * This happens when the location is a client-side route that the browser
     * renders, when a listener forwarded to an external URL, and when a
     * {@link PreserveOnRefresh} view first has to ask the browser for the
     * window name. In the last case the view is shown when the browser answers,
     * in a later request that fires no navigation events.
     */
    public record NotShown() implements Outcome {
    }

    private final Location location;
    private final NavigationTrigger trigger;
    private final Outcome outcome;

    /**
     * Creates a new event.
     *
     * @param ui
     *            the UI that navigated, not {@code null}
     * @param location
     *            the requested location, not {@code null}
     * @param trigger
     *            the action that triggered the navigation, not {@code null}
     * @param outcome
     *            how the navigation ended, not {@code null}
     */
    public NavigationEndedEvent(UI ui, Location location,
            NavigationTrigger trigger, Outcome outcome) {
        super(ui);
        this.location = location;
        this.trigger = trigger;
        this.outcome = outcome;
    }

    /**
     * Gets the UI that navigated.
     *
     * @return the UI, never {@code null}
     */
    @Override
    public UI getSource() {
        return (UI) super.getSource();
    }

    /**
     * Gets the UI that navigated.
     *
     * @return the UI, never {@code null}
     */
    public UI getUI() {
        return getSource();
    }

    /**
     * Gets the location that was requested, before any forward or reroute. It
     * is the same as in the matching {@link NavigationStartedEvent}.
     *
     * @return the requested location, never {@code null}
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Gets the action that triggered the navigation. It is the same as in the
     * matching {@link NavigationStartedEvent}.
     *
     * @return the navigation trigger, never {@code null}
     */
    public NavigationTrigger getTrigger() {
        return trigger;
    }

    /**
     * Gets how the navigation ended.
     *
     * @return the outcome, never {@code null}
     */
    public Outcome getOutcome() {
        return outcome;
    }
}

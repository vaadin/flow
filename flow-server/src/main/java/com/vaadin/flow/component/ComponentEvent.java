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
package com.vaadin.flow.component;

import java.util.EventObject;

import com.vaadin.flow.server.Command;

/**
 * An event whose source is a {@link Component}.
 * <p>
 * Typically used together with {@link ComponentEventBus}.
 *
 * @param <T>
 *            the event source type
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ComponentEvent<T extends Component> extends EventObject {

    private boolean fromClient = false;
    private Command unregisterListenerCommand = null;
    private UI ui;

    /**
     * Creates a new event using the given source and indicator whether the
     * event originated from the client side or the server side.
     *
     * @param source
     *            the source component
     * @param fromClient
     *            <code>true</code> if the event originated from the client
     *            side, <code>false</code> otherwise
     */
    public ComponentEvent(T source, boolean fromClient) {
        super(source);
        this.fromClient = fromClient;
    }

    /**
     * Creates a new event using the given source, indicator whether the event
     * originated from the client side or the server side, and an explicit UI to
     * associate with the event.
     * <p>
     * Use this constructor when the source component may not be attached to a
     * UI at the time the event is fired, but the UI is still known (for
     * example, when the event is dispatched from code that has access to the
     * current UI). The supplied UI is then returned by {@link #getUI()} without
     * relying on the source's attachment state.
     *
     * @param source
     *            the source component
     * @param fromClient
     *            <code>true</code> if the event originated from the client
     *            side, <code>false</code> otherwise
     * @param ui
     *            the UI associated with the event, or <code>null</code> if not
     *            available
     * @since 25.2
     */
    public ComponentEvent(T source, boolean fromClient, UI ui) {
        super(source);
        this.fromClient = fromClient;
        this.ui = ui;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getSource() {
        return (T) super.getSource();
    }

    /**
     * Gets the UI associated with this event.
     * <p>
     * The UI is resolved in the following order:
     * <ol>
     * <li>If a {@link UI} was explicitly provided at
     * {@linkplain #ComponentEvent(Component, boolean, UI) construction time},
     * that instance is returned.</li>
     * <li>If the source component is itself a {@link UI}, it is returned
     * directly.</li>
     * <li>Otherwise, the UI is obtained from
     * {@code getSource().getUI().get()}.</li>
     * </ol>
     * <p>
     * In the common case the source is attached to a UI when the event fires,
     * so this method is a convenient shorthand for
     * {@code getSource().getUI().get()}.
     * <p>
     * If none of the above applies and the source component is not currently
     * attached to a UI, this method throws an {@link IllegalStateException}.
     * This can happen, for example, when an initial value is set on a field
     * before it is added to the UI and a value-change listener is invoked. If
     * your listener can run while the source is detached, use
     * {@code getSource().getUI()} instead, which returns an
     * {@link java.util.Optional} and lets you handle the detached case
     * explicitly.
     *
     * @return the UI associated with this event, never {@code null}
     * @throws IllegalStateException
     *             if the source component is not currently attached to a UI and
     *             no UI was provided at construction time
     * @since 25.2
     */
    public UI getUI() {
        if (ui != null) {
            return ui;
        }
        T source = getSource();
        if (source instanceof UI sourceUI) {
            return sourceUI;
        }
        return source.getUI()
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot resolve UI for event source " + source
                                + ": the component is not currently attached "
                                + "to a UI. Use getSource().getUI() to handle "
                                + "the detached case explicitly."));
    }

    /**
     * Checks if this event originated from the client side.
     *
     * @return <code>true</code> if the event originated from the client side,
     *         <code>false</code> otherwise
     */
    public boolean isFromClient() {
        return fromClient;
    }

    /**
     * Sets the command which is executed to unregister the listener.
     * <p>
     * For internal use.
     *
     * @param unregisterListenerCommand
     *            the unregister command
     */
    void setUnregisterListenerCommand(Command unregisterListenerCommand) {
        this.unregisterListenerCommand = unregisterListenerCommand;
    }

    /**
     * Unregisters the event listener currently being invoked.
     * <p>
     * This method can only be called from within an event listener. Calling it
     * will remove the current event listener so no further events are passed to
     * it.
     */
    public void unregisterListener() {
        if (unregisterListenerCommand == null) {
            throw new IllegalStateException(
                    "unregisterListener can only be called inside the event listener");
        }
        unregisterListenerCommand.execute();
    }

}

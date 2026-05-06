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

    @SuppressWarnings("unchecked")
    @Override
    public T getSource() {
        return (T) super.getSource();
    }

    /**
     * Gets the UI the source component is attached to.
     * <p>
     * This is a convenience for {@code getSource().getUI().get()} when the
     * event is fired while the source is attached to a UI, which is the common
     * case.
     * <p>
     * If the source component is not currently attached to a UI, this method
     * throws an {@link IllegalStateException}. This can happen, for example,
     * when an initial value is set on a field before it is added to the UI and
     * a value-change listener is invoked. If your listener can run while the
     * source is detached, use {@code getSource().getUI()} instead, which
     * returns an {@link java.util.Optional} and lets you handle the detached
     * case explicitly.
     *
     * @return the UI the source component is attached to, never {@code null}
     * @throws IllegalStateException
     *             if the source component is not currently attached to a UI
     */
    public UI getUI() {
        T source = getSource();
        if (source instanceof UI ui) {
            return ui;
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

/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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

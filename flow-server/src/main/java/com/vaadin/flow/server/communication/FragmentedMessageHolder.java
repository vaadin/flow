package com.vaadin.flow.server.communication;

import java.io.Serializable;

import org.atmosphere.cpr.AtmosphereResource;

import com.vaadin.flow.server.communication.AtmospherePushConnection.FragmentedMessage;

public interface FragmentedMessageHolder extends Serializable {

    /**
     * Gets the partial message that is currently being received, if any.
     *
     * @param resource
     *            the resource to get the partial message forder
     * @return the fragmented message being received or a new empty instance
     */
    public FragmentedMessage getOrCreateFragmentedMessage(
            AtmosphereResource resource);

    /**
     * Clears the partial message that is currently being received. Should be
     * called when the whole message has been received.
     *
     * @param resource
     *            the related resource
     */
    public void clearFragmentedMessage(AtmosphereResource resource);
}

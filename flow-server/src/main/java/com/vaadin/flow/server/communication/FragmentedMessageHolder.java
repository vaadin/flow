package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;

import org.atmosphere.cpr.AtmosphereRequest;

import com.vaadin.flow.server.communication.AtmospherePushConnection.FragmentedMessage;

public interface FragmentedMessageHolder extends Serializable {

    /**
     * Gets the partial message that is currently being received, if any.
     *
     * @param request
     *            the request to get the partial message for
     * @param reader
     *            the request body reader
     * @return the partial message or null
     */
    public FragmentedMessage getOrCreateFragmentedMessage(
            AtmosphereRequest request, Reader reader) throws IOException;

    /**
     * Clears the partial message that is currently being received. Should be
     * called when the whole message has been received.
     *
     * @param request
     *            the related request
     */
    public void clearFragmentedMessage(AtmosphereRequest request);
}

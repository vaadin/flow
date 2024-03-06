/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.flow;

import java.util.Objects;

import com.google.web.bindery.event.shared.HandlerRegistration;
import com.vaadin.client.Registry;
import com.vaadin.client.communication.ResponseHandlingEndedEvent;

import elemental.client.Browser;

/**
 * Handler that makes sure that scroll to fragment and hash change event work
 * when there has been navigation via {@link RouterLinkHandler router link} to a
 * path with fragment.
 * <p>
 * This class will trigger scroll to fragment and hash change event once the
 * response from server has been processed, but only if the server did not
 * override the location.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class FragmentHandler {

    private final String previousHref;
    private final String newHref;
    private final Registry registry;

    private HandlerRegistration handlerRegistration;

    /**
     * Creates a new fragment handler.
     *
     * @param previousHref
     *            the href before the navigation
     * @param newHref
     *            the href being navigated into
     * @param registry
     *            the registry to bind to
     */
    public FragmentHandler(String previousHref, String newHref,
            Registry registry) {
        assert previousHref != null;
        assert newHref != null;

        this.previousHref = previousHref;
        this.newHref = newHref;
        this.registry = registry;
    }

    /**
     * Adds a request response tracker to the given registry for making sure the
     * fragment is handled correctly if the location has not been updated during
     * the response.
     */
    public void bind() {
        handlerRegistration = registry.getRequestResponseTracker()
                .addResponseHandlingEndedHandler(this::onResponseHandlingEnded);
    }

    private void onResponseHandlingEnded(
            ResponseHandlingEndedEvent responseHandlingEndedEvent) {
        assert handlerRegistration != null;

        String currentHref = Browser.getWindow().getLocation().getHref();

        if (Objects.equals(currentHref, newHref)) {
            // make scroll position handler ignore pop state event
            registry.getScrollPositionHandler()
                    .setIgnoreScrollRestorationOnNextPopStateEvent(true);
            // trigger possible scroll to fragment identifier
            Browser.getWindow().getLocation().replace(newHref);
            // fire fragment change event
            fireHashChangeEvent(previousHref, newHref);

            registry.getScrollPositionHandler()
                    .setIgnoreScrollRestorationOnNextPopStateEvent(false);
        }

        handlerRegistration.removeHandler();
    }

    /*
     * This method is used instead because Elemental's
     * HashChangeEvent.initHashChange gives errors.
     */
    private static native void fireHashChangeEvent(String oldUrl, String newUrl)
    /*-{
        var event = new HashChangeEvent('hashchange', {
            'view': window,
            'bubbles': true,
            'cancelable': false,
            'oldURL': oldUrl,
            'newURL': newUrl
        });
        window.dispatchEvent(event);
     }-*/;
}

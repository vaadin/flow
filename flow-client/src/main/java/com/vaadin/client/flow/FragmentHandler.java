/*
 * Copyright 2000-2020 Vaadin Ltd.
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

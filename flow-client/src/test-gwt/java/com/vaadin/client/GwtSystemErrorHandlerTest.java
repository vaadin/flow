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
package com.vaadin.client;

import com.vaadin.client.bootstrap.ErrorMessage;
import com.vaadin.client.flow.reactive.Reactive;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.events.Event;

public class GwtSystemErrorHandlerTest extends ClientEngineTestBase {

    private Registry registry;

    // A script cannot dispatch a trusted beforeunload, and navigating the test
    // page is not possible in the GWT test runner, so the handler is told which
    // events count as trusted.
    private boolean beforeUnloadTrusted;

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();
        Reactive.reset();
        beforeUnloadTrusted = false;
        clearNotifications();
    }

    @Override
    protected void gwtTearDown() throws Exception {
        // Clears the leaving state of the handlers created by the cases
        dispatch("pointerdown");
        clearNotifications();
        super.gwtTearDown();
    }

    // With a caption the expiration is shown as a notification, which the
    // cases can find, instead of a redirect that would leave the test page.
    private SystemErrorHandler makeSessionExpiredHandler(
            boolean webComponentMode) {
        ApplicationConfiguration configuration = new ApplicationConfiguration();
        configuration.setWebComponentMode(webComponentMode);
        configuration.setSessionExpiredError(createSessionExpiredError());
        registry = new Registry() {
            {
                set(ApplicationConfiguration.class, configuration);
                set(SystemErrorHandler.class, new SystemErrorHandler(this) {
                    @Override
                    boolean isTrusted(Event event) {
                        return beforeUnloadTrusted;
                    }
                });
            }
        };
        return registry.getSystemErrorHandler();
    }

    public void testHandleSessionExpiredError_syntheticBeforeUnload_shownRightAway() {
        SystemErrorHandler handler = makeSessionExpiredHandler(false);
        // As vite-devmode does when its HMR socket is closed with 1008.
        dispatch("beforeunload");

        handler.handleSessionExpiredError(null);

        assertEquals("Session ended", findNotification().getTextContent());
    }

    public void testHandleSessionExpiredError_leavingPage_deferredUntilPageUsedAgain() {
        SystemErrorHandler handler = makeSessionExpiredHandler(false);
        beforeUnloadTrusted = true;
        dispatch("beforeunload");

        handler.handleSessionExpiredError(null);
        assertNull(findNotification());

        dispatch("pointerdown");
        assertEquals("Session ended", findNotification().getTextContent());
    }

    public void testHandleSessionExpiredError_webComponentModeLeavingPage_notDeferred() {
        SystemErrorHandler handler = makeSessionExpiredHandler(true);
        beforeUnloadTrusted = true;
        dispatch("beforeunload");

        handler.handleSessionExpiredError(null);

        assertEquals(1, Browser.getDocument()
                .querySelectorAll(".v-system-error").getLength());
    }

    private static Element findNotification() {
        return Browser.getDocument().querySelector(".v-system-error");
    }

    private static native void clearNotifications()
    /*-{
        var elements = $doc.querySelectorAll('.v-system-error');
        for (var i = 0; i < elements.length; i++) {
            elements[i].parentNode.removeChild(elements[i]);
        }
    }-*/;

    private static native void dispatch(String type)
    /*-{
        $wnd.dispatchEvent(new Event(type));
    }-*/;

    private static native ErrorMessage createSessionExpiredError()
    /*-{
        return { caption: 'Session ended', url: 'about:blank' };
    }-*/;
}

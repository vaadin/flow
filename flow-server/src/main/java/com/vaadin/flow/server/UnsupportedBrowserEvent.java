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
package com.vaadin.flow.server;

import java.util.EventObject;

/**
 * Event fired through the {@link VaadinService#getEventBus() service event bus}
 * when a browser is shown the page telling that it is too old to run the
 * application, just before the page is written to the response.
 * <p>
 * The bootstrap page detects a browser that lacks features the application
 * needs and redirects it to that page, so the event is fired on the request
 * thread of the redirected request. A browser too old to run the detection
 * script at all never makes that request, and no event is fired for it.
 * <p>
 * Listen to the event to, for example, record which users run an outdated
 * browser:
 *
 * <pre>
 * service.getEventBus().addListener(UnsupportedBrowserEvent.class,
 *         event -&gt; log(event.getRequest().getHeader("User-Agent")));
 * </pre>
 *
 * @see UnsupportedBrowserHandler
 */
public class UnsupportedBrowserEvent extends EventObject {

    private final VaadinSession session;
    private final transient VaadinRequest request;

    /**
     * Creates a new event.
     *
     * @param service
     *            the service that handles the request, not {@code null}
     * @param session
     *            the session of the browser, not {@code null}
     * @param request
     *            the request for the page, not {@code null}
     */
    public UnsupportedBrowserEvent(VaadinService service, VaadinSession session,
            VaadinRequest request) {
        super(service);
        this.session = session;
        this.request = request;
    }

    /**
     * Gets the service that handles the request.
     *
     * @return the service, not {@code null}
     */
    public VaadinService getService() {
        return (VaadinService) getSource();
    }

    /**
     * Gets the session of the browser.
     *
     * @return the session, not {@code null}
     */
    public VaadinSession getSession() {
        return session;
    }

    /**
     * Gets the request for the page. The {@code User-Agent} header of the
     * request tells which browser it is.
     *
     * @return the request, not {@code null}
     */
    public VaadinRequest getRequest() {
        return request;
    }
}

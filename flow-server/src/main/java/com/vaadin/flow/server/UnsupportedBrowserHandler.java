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

import java.io.IOException;
import java.net.URL;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.server.HandlerHelper.RequestType;
import com.vaadin.flow.shared.ApplicationConstants;

/**
 * A {@link RequestHandler} that presents an informative page that the browser
 * in use will not work.
 * <p>
 * An {@link UnsupportedBrowserEvent} is fired through the service event bus
 * before the page is written.
 *
 * @since 1.0
 */
public class UnsupportedBrowserHandler implements RequestHandler {

    private static final String BROWSER_TOO_OLD_HTML = "browser-too-old.html";

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {
        if (!HandlerHelper.isRequestType(request,
                RequestType.BROWSER_TOO_OLD)) {
            return false;
        }
        VaadinService service = session.getService();
        service.getEventBus().fireEvent(
                new UnsupportedBrowserEvent(service, session, request));

        response.setContentType(
                ApplicationConstants.CONTENT_TYPE_TEXT_HTML_UTF_8);

        // Use a file from the application resources folder, if available
        Lookup lookup = service.getContext().getAttribute(Lookup.class);
        if (lookup != null) {
            final ResourceProvider resourceProvider = lookup
                    .lookup(ResourceProvider.class);
            if (resourceProvider != null) {
                final URL applicationBrowserTooOldPage = resourceProvider
                        .getApplicationResource(BROWSER_TOO_OLD_HTML);
                if (applicationBrowserTooOldPage != null) {
                    try (var input = applicationBrowserTooOldPage
                            .openStream()) {
                        input.transferTo(response.getOutputStream());
                    }
                    return true;
                }
            }
        }

        try (var input = getClass().getResourceAsStream(BROWSER_TOO_OLD_HTML)) {
            input.transferTo(response.getOutputStream());
        }
        return true;
    }

}

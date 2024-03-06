/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.server.HandlerHelper.RequestType;
import com.vaadin.flow.shared.ApplicationConstants;

/**
 * A {@link RequestHandler} that presents an informative page that the browser
 * in use will not work.
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
        response.setContentType(
                ApplicationConstants.CONTENT_TYPE_TEXT_HTML_UTF_8);

        // Use a file from the application resources folder, if available
        Lookup lookup = session.getService().getContext()
                .getAttribute(Lookup.class);
        if (lookup != null) {
            final ResourceProvider resourceProvider = lookup
                    .lookup(ResourceProvider.class);
            if (resourceProvider != null) {
                final URL applicationBrowserTooOldPage = resourceProvider
                        .getApplicationResource(BROWSER_TOO_OLD_HTML);
                if (applicationBrowserTooOldPage != null) {
                    IOUtils.copy(applicationBrowserTooOldPage,
                            response.getOutputStream());
                    return true;
                }
            }
        }

        IOUtils.copy(getClass().getResourceAsStream(BROWSER_TOO_OLD_HTML),
                response.getOutputStream());
        return true;
    }

}

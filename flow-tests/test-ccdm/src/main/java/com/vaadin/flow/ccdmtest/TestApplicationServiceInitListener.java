/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.ccdmtest;

import org.jsoup.nodes.Element;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.communication.IndexHtmlResponse;

public class TestApplicationServiceInitListener
        implements VaadinServiceInitListener {
    private static String getBaseUrl(IndexHtmlResponse indexHtmlResponse) {
        VaadinServletRequest request = (VaadinServletRequest) indexHtmlResponse
                .getVaadinRequest();
        String scheme = request.getScheme() + "://";
        String serverName = request.getServerName();
        String serverPort = (request.getServerPort() == 80) ? ""
                : ":" + request.getServerPort();
        String contextPath = request.getContextPath();
        return scheme + serverName + serverPort + contextPath;
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addIndexHtmlRequestListener(response -> {
            Element el = new Element("output");
            el.text("Modified page");
            response.getDocument().body().appendChild(el);
        });

        event.addIndexHtmlRequestListener(indexHtmlResponse -> {
            Element meta = new Element("meta");
            meta.attr("name", "og:image");
            meta.attr("content",
                    getBaseUrl(indexHtmlResponse) + "/image/my_app.png");
            indexHtmlResponse.getDocument().head().appendChild(meta);
        });

        event.addIndexHtmlRequestListener(response -> {
            Element styleElem = new Element("style");
            styleElem.text("body,#outlet{height:50vh;width:50vw;}");
            response.getDocument().head().appendChild(styleElem);
        });

        // Stub logout request handler for CsrfCookieIT
        event.addRequestHandler((session, request, response) -> {
            if (!request.getMethod().equals("POST")) {
                return false;
            }

            if (!request.getPathInfo().equals("/logout")) {
                return false;
            }

            request.getWrappedSession().invalidate();

            response.setStatus(200);
            response.setContentLength(0);

            return true;
        });
    }
}

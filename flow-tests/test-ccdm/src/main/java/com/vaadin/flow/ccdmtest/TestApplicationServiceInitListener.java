/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.ccdmtest;

import org.jsoup.nodes.Element;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.communication.IndexHtmlResponse;

public class TestApplicationServiceInitListener
        implements VaadinServiceInitListener {
    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addIndexHtmlRequestListener(response -> {
            Element el = new Element("label");
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
    }

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
}

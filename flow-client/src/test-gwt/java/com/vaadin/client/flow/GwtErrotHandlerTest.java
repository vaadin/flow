/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.flow;

import com.vaadin.client.ApplicationConfiguration;
import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.Registry;
import com.vaadin.client.SystemErrorHandler;
import com.vaadin.client.flow.reactive.Reactive;

import elemental.client.Browser;
import elemental.dom.Element;

public class GwtErrotHandlerTest extends ClientEngineTestBase {

    private Registry registry;

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();
        Reactive.reset();

        registry = new Registry() {
            {
                set(ApplicationConfiguration.class,
                        new ApplicationConfiguration());
                set(SystemErrorHandler.class, new SystemErrorHandler(this));
            }
        };

    }

    public void testhandleUnrecoverableError_textContentIsSetInDivsNotInnerHtml() {
        registry.getSystemErrorHandler().handleUnrecoverableError("<foo></foo>",
                "<bar></bar>", "<baz></baz>", null);
        Element container = Browser.getDocument().getBody()
                .querySelector(".v-system-error");
        Element caption = container.querySelector(".caption");
        Element message = container.querySelector(".message");
        Element details = container.querySelector(".details");

        assertEquals("&lt;foo&gt;&lt;/foo&gt;", caption.getInnerHTML());
        assertEquals("<foo></foo>", caption.getTextContent());

        assertEquals("&lt;bar&gt;&lt;/bar&gt;", message.getInnerHTML());
        assertEquals("<bar></bar>", message.getTextContent());

        assertEquals("&lt;baz&gt;&lt;/baz&gt;", details.getInnerHTML());
        assertEquals("<baz></baz>", details.getTextContent());
    }
}

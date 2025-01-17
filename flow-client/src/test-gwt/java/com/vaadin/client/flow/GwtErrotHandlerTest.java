/*
 * Copyright 2000-2025 Vaadin Ltd.
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

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
package com.vaadin.flow.component.html;

import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.StreamResourceWriter;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlObjectTest extends ComponentTest {

    @AfterEach
    void tearDown() {
        CurrentInstance.clearAll();
    }

    @Override
    protected void addProperties() {
        addStringProperty("data", "");
        addOptionalStringProperty("type");
    }

    @Test
    @Override
    protected void testHasOrderedComponents() {
        super.testHasOrderedComponents();
    }

    @Test
    void setData_dataAsAResource() {
        UI ui = new UI();
        UI.setCurrent(ui);
        HtmlObject object = new HtmlObject();
        StreamResource resource = new StreamResource("foo",
                Mockito.mock(StreamResourceWriter.class));
        object.setData(resource);

        URI uri = StreamResourceRegistry.getURI(resource);

        assertEquals(uri.toASCIIString(),
                object.getElement().getAttribute("data"));
    }

    @Test
    void setData_dataAsAResourceinCTOR() {
        UI ui = new UI();
        UI.setCurrent(ui);
        StreamResource resource = new StreamResource("foo",
                Mockito.mock(StreamResourceWriter.class));

        HtmlObject object = new HtmlObject(resource);

        URI uri = StreamResourceRegistry.getURI(resource);

        assertEquals(uri.toASCIIString(),
                object.getElement().getAttribute("data"));
    }

    @Test
    void setDownloadHandlerData_dataAsAResource() {
        UI ui = new UI();
        UI.setCurrent(ui);
        HtmlObject object = new HtmlObject();
        object.setData(event -> event.getWriter().write("foo"));

        assertTrue(
                object.getElement().getAttribute("data")
                        .startsWith("VAADIN/dynamic/resource/-1/"),
                "Data should be set as dynamic resource.");
    }

    @Test
    void setDownloadHandlerData_dataAsAResourceinCTOR() {
        UI ui = new UI();
        UI.setCurrent(ui);

        HtmlObject object = new HtmlObject(
                event -> event.getWriter().write("foo"), "foo");

        assertTrue(
                object.getElement().getAttribute("data")
                        .startsWith("VAADIN/dynamic/resource/-1/"),
                "Data should be set as dynamic resource.");
    }

    @Test
    void downloadHandler_isSetToInline() {
        Element element = Mockito.mock(Element.class);
        StateNode node = Mockito.mock(StateNode.class);
        Mockito.when(element.getNode()).thenReturn(node);
        Mockito.when(node.getFeatureIfInitialized(Mockito.any()))
                .thenReturn(Optional.empty());

        class TestHtmlObject extends HtmlObject {
            public TestHtmlObject(DownloadHandler downloadHandler) {
                super(downloadHandler);
            }

            public TestHtmlObject(DownloadHandler downloadHandler,
                    Param... params) {
                super(downloadHandler, params);
            }

            public TestHtmlObject(DownloadHandler data, String type) {
                super(data, type);
            }

            public TestHtmlObject(DownloadHandler data, String type,
                    Param... params) {
                super(data, type, params);
            }

            @Override
            public Element getElement() {
                return element;
            }
        }
        InputStreamDownloadHandler handler = createDummyDownloadHandler();
        assertFalse(handler.isInline());
        new TestHtmlObject(handler);
        assertTrue(handler.isInline());

        handler = createDummyDownloadHandler();
        new TestHtmlObject(handler, "type");
        assertTrue(handler.isInline());

        handler = createDummyDownloadHandler();
        new TestHtmlObject(handler, "type", new Param("param", "paramValue"));
        assertTrue(handler.isInline());

        handler = createDummyDownloadHandler();
        new TestHtmlObject(handler, new Param("param", "paramValue"));
        assertTrue(handler.isInline());
    }

    private InputStreamDownloadHandler createDummyDownloadHandler() {
        return DownloadHandler
                .fromInputStream(event -> DownloadResponse.error(500));
    }
}

/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.html;

import java.io.IOException;
import java.net.URI;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.StreamResourceWriter;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;

public class HtmlObjectTest extends ComponentTest {

    @After
    public void tearDown() {
        CurrentInstance.clearAll();
    }

    @Override
    protected void addProperties() {
        addStringProperty("data", "");
        addOptionalStringProperty("type");
    }

    @Test
    @Override
    public void testHasOrderedComponents() {
        super.testHasOrderedComponents();
    }

    @Test
    public void setData_dataAsAResource() {
        UI ui = new UI();
        UI.setCurrent(ui);
        HtmlObject object = new HtmlObject();
        StreamResource resource = new StreamResource("foo",
                Mockito.mock(StreamResourceWriter.class));
        object.setData(resource);

        URI uri = StreamResourceRegistry.getURI(resource);

        Assert.assertEquals(uri.toASCIIString(),
                object.getElement().getAttribute("data"));
    }

    @Test
    public void setData_dataAsAResourceinCTOR() {
        UI ui = new UI();
        UI.setCurrent(ui);
        StreamResource resource = new StreamResource("foo",
                Mockito.mock(StreamResourceWriter.class));

        HtmlObject object = new HtmlObject(resource);

        URI uri = StreamResourceRegistry.getURI(resource);

        Assert.assertEquals(uri.toASCIIString(),
                object.getElement().getAttribute("data"));
    }

    @Test
    public void setDownloadHandlerData_dataAsAResource() {
        UI ui = new UI();
        UI.setCurrent(ui);
        HtmlObject object = new HtmlObject();
        object.setData(event -> event.getWriter().write("foo"));

        Assert.assertTrue("Data should be set as dynamic resource.",
                object.getElement().getAttribute("data")
                        .startsWith("VAADIN/dynamic/resource/-1/"));
    }

    @Test
    public void setDownloadHandlerData_dataAsAResourceinCTOR() {
        UI ui = new UI();
        UI.setCurrent(ui);

        HtmlObject object = new HtmlObject(
                event -> event.getWriter().write("foo"), "foo");

        Assert.assertTrue("Data should be set as dynamic resource.",
                object.getElement().getAttribute("data")
                        .startsWith("VAADIN/dynamic/resource/-1/"));
    }

    @Test
    public void downloadHandler_isSetToInline() {
        Element element = Mockito.mock(Element.class);
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
        Assert.assertFalse(handler.isInline());
        new TestHtmlObject(handler);
        Assert.assertTrue(handler.isInline());

        handler = createDummyDownloadHandler();
        new TestHtmlObject(handler, "type");
        Assert.assertTrue(handler.isInline());

        handler = createDummyDownloadHandler();
        new TestHtmlObject(handler, "type", new Param("param", "paramValue"));
        Assert.assertTrue(handler.isInline());

        handler = createDummyDownloadHandler();
        new TestHtmlObject(handler, new Param("param", "paramValue"));
        Assert.assertTrue(handler.isInline());
    }

    private InputStreamDownloadHandler createDummyDownloadHandler() {
        return DownloadHandler
                .fromInputStream(event -> DownloadResponse.error(500));
    }
}

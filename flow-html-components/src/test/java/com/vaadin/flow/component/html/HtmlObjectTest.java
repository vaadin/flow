/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.html;

import java.net.URI;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.StreamResourceWriter;

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
}

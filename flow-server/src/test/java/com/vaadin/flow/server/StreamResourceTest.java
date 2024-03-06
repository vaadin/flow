/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import javax.servlet.ServletContext;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.function.ContentTypeResolver;

public class StreamResourceTest {

    @Test
    public void getDefaultContentTypeResolver() {
        StreamResource resource = new StreamResource("foo",
                () -> makeEmptyStream());
        ContentTypeResolver resolver = resource.getContentTypeResolver();

        Assert.assertNotNull(resolver);

        assertContentType(resource, resolver);
    }

    @Test
    public void setContentTypeResolver() {
        StreamResource resource = new StreamResource("foo",
                () -> makeEmptyStream());
        resource.setContentTypeResolver((res, context) -> "bar");

        Assert.assertNotNull(resource.getContentTypeResolver());

        assertContentType(resource, resource.getContentTypeResolver());
    }

    @Test
    public void setContentType() {
        StreamResource resource = new StreamResource("foo",
                () -> makeEmptyStream());
        resource.setContentType("bar");

        Assert.assertNotNull(resource.getContentTypeResolver());

        assertContentType(resource, resource.getContentTypeResolver());
    }

    @Test
    public void setHeader_headerIsInHeadersListAndGetterReturnsTheValue() {
        StreamResource resource = new StreamResource("foo",
                () -> makeEmptyStream());

        resource.setHeader("foo", "bar");

        Assert.assertEquals("bar", resource.getHeader("foo").get());

        Map<String, String> headers = resource.getHeaders();
        Assert.assertEquals(1, headers.size());
        Assert.assertEquals("bar", headers.get("foo"));
    }

    private void assertContentType(StreamResource resource,
            ContentTypeResolver resolver) {
        ServletContext context = Mockito.mock(ServletContext.class);
        Mockito.when(context.getMimeType("foo")).thenReturn("bar");
        String mimeType = resolver.apply(resource, context);

        Assert.assertEquals("bar", mimeType);
    }

    private InputStream makeEmptyStream() {
        return new ByteArrayInputStream(new byte[0]);
    }
}

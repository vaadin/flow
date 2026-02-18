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

import jakarta.servlet.ServletContext;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.function.ContentTypeResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StreamResourceTest {

    @Test
    public void getDefaultContentTypeResolver() {
        StreamResource resource = new StreamResource("foo",
                () -> makeEmptyStream());
        ContentTypeResolver resolver = resource.getContentTypeResolver();

        assertNotNull(resolver);

        assertContentType(resource, resolver);
    }

    @Test
    public void setContentTypeResolver() {
        StreamResource resource = new StreamResource("foo",
                () -> makeEmptyStream());
        resource.setContentTypeResolver((res, context) -> "bar");

        assertNotNull(resource.getContentTypeResolver());

        assertContentType(resource, resource.getContentTypeResolver());
    }

    @Test
    public void setContentType() {
        StreamResource resource = new StreamResource("foo",
                () -> makeEmptyStream());
        resource.setContentType("bar");

        assertNotNull(resource.getContentTypeResolver());

        assertContentType(resource, resource.getContentTypeResolver());
    }

    @Test
    public void setHeader_headerIsInHeadersListAndGetterReturnsTheValue() {
        StreamResource resource = new StreamResource("foo",
                () -> makeEmptyStream());

        resource.setHeader("foo", "bar");

        assertEquals("bar", resource.getHeader("foo").get());

        Map<String, String> headers = resource.getHeaders();
        assertEquals(1, headers.size());
        assertEquals("bar", headers.get("foo"));
    }

    private void assertContentType(StreamResource resource,
            ContentTypeResolver resolver) {
        ServletContext context = Mockito.mock(ServletContext.class);
        Mockito.when(context.getMimeType("foo")).thenReturn("bar");
        String mimeType = resolver.apply(resource, context);

        assertEquals("bar", mimeType);
    }

    private InputStream makeEmptyStream() {
        return new ByteArrayInputStream(new byte[0]);
    }
}

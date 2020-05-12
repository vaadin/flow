/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.servlet.ServletContext;

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

    private void assertContentType(StreamResource resource,
            ContentTypeResolver resolver) {
        VaadinService service = Mockito.mock(VaadinService.class);
        Mockito.when(service.getMimeType("foo")).thenReturn("bar");
        String mimeType = resolver.apply(resource, service);

        Assert.assertEquals("bar", mimeType);
    }

    private InputStream makeEmptyStream() {
        return new ByteArrayInputStream(new byte[0]);
    }
}

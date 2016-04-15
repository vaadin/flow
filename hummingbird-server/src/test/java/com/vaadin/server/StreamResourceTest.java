/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.server;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vaadin Ltd
 *
 */
public class StreamResourceTest {

    @Test
    public void setCacheTimeUsingBuilder_constructedInstanceHasCacheTime() {
        StreamResource.Builder builder = new StreamResource.Builder("name",
                () -> createEmptyStream());
        long cacheTime = 11L;
        builder.setCacheTime(cacheTime);
        StreamResource resource = builder.build();
        Assert.assertEquals(cacheTime, resource.getCacheTime());
    }

    @Test
    public void setContentTypeUsingBuilder_constructedInstanceHasContentType() {
        StreamResource.Builder builder = new StreamResource.Builder("name",
                () -> createEmptyStream());
        String contentType = "my content-type";
        builder.setContentType(contentType);
        StreamResource resource = builder.build();
        Assert.assertEquals(contentType, resource.getContentType());
    }

    @Test
    public void streamResourceInstanceIsImmutable() {
        StreamResource.Builder builder = new StreamResource.Builder("name",
                () -> createEmptyStream());
        long cacheTime = 101L;
        builder.setCacheTime(cacheTime);
        StreamResource resource = builder.build();

        long newCacheTime = 3L;
        builder.setCacheTime(newCacheTime);
        Assert.assertNotEquals(newCacheTime, resource.getCacheTime());
    }

    private InputStream createEmptyStream() {
        return new ByteArrayInputStream(new byte[0]);
    }
}

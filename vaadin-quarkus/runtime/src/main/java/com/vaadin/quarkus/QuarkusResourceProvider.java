/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.quarkus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vaadin.flow.di.ResourceProvider;

/**
 * A {@link ResourceProvider} implementation that delegates resource loading to
 * current thread context ClassLoader.
 */
public class QuarkusResourceProvider implements ResourceProvider {

    private final Map<String, CachedStreamData> cache = new ConcurrentHashMap<>();

    @Override
    public URL getApplicationResource(String path) {
        return Thread.currentThread().getContextClassLoader().getResource(path);
    }

    @Override
    public List<URL> getApplicationResources(String path) throws IOException {
        return Collections.list(Thread.currentThread().getContextClassLoader()
                .getResources(path));
    }

    @Override
    public URL getClientResource(String path) {
        return getApplicationResource(path);
    }

    @Override
    public InputStream getClientResourceAsStream(String path)
            throws IOException {
        // the client resource should be available in the classpath, so
        // its content is cached once. If an exception is thrown then
        // something is broken and it's also cached and will be rethrown on
        // every subsequent access
        CachedStreamData cached = cache.computeIfAbsent(path, key -> {
            URL url = getClientResource(key);
            try (InputStream stream = url.openStream()) {
                ByteArrayOutputStream tempBuffer = new ByteArrayOutputStream();
                stream.transferTo(tempBuffer);
                return new CachedStreamData(tempBuffer.toByteArray(), null);
            } catch (IOException e) {
                return new CachedStreamData(null, e);
            }
        });

        IOException exception = cached.exception;
        if (exception == null) {
            return new ByteArrayInputStream(cached.data);
        }
        throw exception;
    }

    private static class CachedStreamData {

        private final byte[] data;
        private final IOException exception;

        private CachedStreamData(byte[] data, IOException exception) {
            this.data = data;
            this.exception = exception;
        }
    }

}

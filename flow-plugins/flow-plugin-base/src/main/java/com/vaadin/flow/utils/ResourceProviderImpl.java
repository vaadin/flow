package com.vaadin.flow.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;

import com.vaadin.flow.di.ResourceProvider;

/**
 * Default implementation of {@link ResourceProvider}.
 *
 * @author Vaadin Ltd
 * @since
 */
public class ResourceProviderImpl implements ResourceProvider {

    private Map<String, CachedStreamData> cache = new ConcurrentHashMap<>();

    /**
     * Creates a new instance.
     */
    public ResourceProviderImpl() {
    }

    @Override
    public URL getApplicationResource(String path) {
        return ResourceProviderImpl.class.getClassLoader().getResource(path);
    }

    @Override
    public List<URL> getApplicationResources(String path) throws IOException {
        return Collections.list(
                ResourceProviderImpl.class.getClassLoader().getResources(path));
    }

    @Override
    public URL getClientResource(String path) {
        return getApplicationResource(path);
    }

    @Override
    public InputStream getClientResourceAsStream(String path)
            throws IOException {
        InputStream result;
        // the client resource should be available in the classpath, so
        // its content is cached once. If an exception is thrown then
        // something is broken and it's also cached and will be rethrown on
        // every subsequent access
        CachedStreamData cached = cache.computeIfAbsent(path, key -> {
            URL url = getClientResource(key);
            try (InputStream stream = url.openStream()) {
                ByteArrayOutputStream tempBuffer = new ByteArrayOutputStream();
                IOUtils.copy(stream, tempBuffer);
                return new CachedStreamData(tempBuffer.toByteArray(), null);
            } catch (IOException e) {
                return new CachedStreamData(null, e);
            }
        });

        IOException exception = cached.exception;
        if (exception == null) {
            result = new ByteArrayInputStream(cached.data);
        } else {
            throw exception;
        }
        return result;
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
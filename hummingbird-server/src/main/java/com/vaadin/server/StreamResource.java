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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * Represents dynamically generated data.
 * <p>
 * The instance should be registered via
 * {@link StreamResourceRegistry#registerResource(StreamResource)}. This method
 * returns an object which may be used to get resource URI.
 * <p>
 * This class is immutable. Use {@link Builder} to construct customized
 * instance.
 * 
 * @author Vaadin Ltd
 *
 */
public class StreamResource implements Serializable, Cloneable {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final String fileName;

    private String contentType = DEFAULT_CONTENT_TYPE;

    private long cacheTime = 0L;

    private final StreamResourceWriter writer;

    /**
     * Builder for {@link StreamResource}.
     */
    public static class Builder {

        private final StreamResource resource;

        /**
         * Creates a builder for {@link StreamResource} using mandatory
         * parameters {@code name} as a resource file name and output stream
         * {@code writer} as a data producer. {@code writer} should write data
         * in the output stream provided as an argument to its
         * {@link StreamResourceWriter#accept(OutputStream, VaadinSession)}
         * method.
         * <p>
         * {@code name} parameter value will be used in URI (generated when
         * resource is registered) in a way that the {@name} is the last segment
         * of the path. So this is synthetic file name (not real one).
         * 
         * @param name
         *            resource file name. May not be null.
         * @param writer
         *            data output stream consumer
         */
        public Builder(String name, StreamResourceWriter writer) {
            resource = new StreamResource(name, writer);
        }

        /**
         * Creates a builder for {@link StreamResource} using mandatory
         * parameters {@code name} as a resource file name and input stream
         * {@code factory} as a factory for data.
         * <p>
         * {@code name} parameter value will be used in URI (generated when
         * resource is registered) in a way that the {@name} is the last segment
         * of the path. So this is synthetic file name (not real one).
         * 
         * @param name
         *            resource file name. May not be null.
         * @param factory
         *            data input stream factory. May not be null.
         */
        public Builder(String name, InputStreamFactory factory) {
            resource = new StreamResource(name, factory);
        }

        /**
         * Set cache time in millis. Zero or negative value disables the caching
         * of this stream.
         * 
         * @param cacheTime
         *            cache time
         */
        public Builder setCacheTime(long cacheTime) {
            resource.cacheTime = cacheTime;
            return this;
        }

        /**
         * Set content type for the resource.
         * 
         * @param contentType
         *            resource content type
         */
        public Builder setContentType(String contentType) {
            resource.contentType = contentType;
            return this;
        }

        /**
         * Builds {@link StreamResource} instance using values set via the
         * builder. Constructed instance is immutable.
         * 
         * @return constructed {@link StreamResource} instance
         */
        public StreamResource build() {
            try {
                return (StreamResource) resource.clone();
            } catch (CloneNotSupportedException e) {
                // Cannot happen since StreamResource implements Cloneable
                throw new RuntimeException(e);
            }
        }
    }

    private static class Pipe implements StreamResourceWriter {

        private static final int BUFFER_SIZE = 1024;

        private InputStreamFactory factory;

        private Pipe(InputStreamFactory factory) {
            this.factory = factory;
        }

        @Override
        public void accept(OutputStream stream, VaadinSession session)
                throws IOException {
            try (InputStream input = createInputStream(session)) {
                copy(session, input, stream);
            }
        }

        private InputStream createInputStream(VaadinSession session) {
            session.lock();
            try {
                return factory.createInputStream();
            } finally {
                session.unlock();
            }
        }

        private void copy(VaadinSession session, InputStream source,
                OutputStream out) throws IOException {
            byte[] buf = new byte[BUFFER_SIZE];
            int n;
            while ((n = read(session, source, buf)) > 0) {
                out.write(buf, 0, n);
            }
        }

        private int read(VaadinSession session, InputStream source,
                byte[] buffer) throws IOException {
            if (factory.requiresLock()) {
                session.lock();
                try {
                    return source.read(buffer);
                } finally {
                    session.unlock();
                }
            } else {
                return source.read(buffer);
            }
        }
    }

    /**
     * Creates {@link StreamResource} instance using mandatory parameters
     * {@code name} as a resource file name and output stream {@code writer} as
     * a data producer. {@code writer} should write data in the output stream
     * provided as an argument to its
     * {@link StreamResourceWriter#accept(OutputStream, VaadinSession)} method.
     * <p>
     * {@code name} parameter value will be used in URI (generated when resource
     * is registered) in a way that the {@code name} is the last segment of the
     * path. So this is synthetic file name (not real one).
     * 
     * @param name
     *            resource file name. May not be null.
     * @param writer
     *            data output stream consumer
     */
    public StreamResource(String name, StreamResourceWriter writer) {
        assert name != null;
        assert writer != null;

        if (name.indexOf('/') != -1) {
            throw new IllegalArgumentException(
                    "Resource file name parameter contains '/'");
        }
        fileName = name;
        this.writer = writer;
    }

    /**
     * Creates {@link StreamResource} instance using mandatory parameters
     * {@code name} as a resource file name and input stream {@code factory} as
     * a factory for data.
     * <p>
     * {@code name} parameter value will be used in URI (generated when resource
     * is registered) in a way that the {@code name} is the last segment of the
     * path. So this is synthetic file name (not real one).
     * 
     * @param name
     *            resource file name. May not be null.
     * @param factory
     *            data input stream factory. May not be null.
     */
    public StreamResource(String name, InputStreamFactory factory) {
        this(name, new Pipe(factory));
        assert name != null;
    }

    /**
     * Returns the content type of the resource.
     * 
     * @return resource content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Gets the length of cache expiration time. This gives the possibility to
     * cache the resource. "Cache-Control" HTTP header will be set based on this
     * value.
     * <p>
     * Default value is {@code 0}. So caching is disabled.
     * 
     * @return cache time in milliseconds.
     */
    public long getCacheTime() {
        return cacheTime;
    }

    /**
     * Get the resource file name.
     * <p>
     * The value will be used in URI (generated when resource is registered) in
     * a way that the {@code name} is the last segment of the path. So this is
     * synthetic file name (not real one).
     * 
     * @return resource file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the stream resource writer.
     * <p>
     * Writer writes data in the output stream provided as an argument to its
     * {@link StreamResourceWriter#accept(OutputStream, VaadinSession)} method.
     * 
     * @return stream resource writer
     */
    public StreamResourceWriter getWriter() {
        return writer;
    }

}

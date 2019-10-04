/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import javax.servlet.ServletContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.ContentTypeResolver;

/**
 * Represents dynamically generated data.
 * <p>
 * Resource URI registration is automatically handled by components that
 * explicitly support stream resources and by
 * {@link Element#setAttribute(String, AbstractStreamResource)}. In other cases,
 * the resource must manually be registered using
 * {@link StreamResourceRegistry#registerResource(AbstractStreamResource)} to
 * get a URI from which the browser can load the contents of the resource.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class StreamResource extends AbstractStreamResource {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private static final DefaultResolver DEFAULT_RESOLVER = new DefaultResolver();

    private final String fileName;

    private final StreamResourceWriter writer;

    private ContentTypeResolver resolver = DEFAULT_RESOLVER;

    private static class DefaultResolver implements ContentTypeResolver {

        @Override
        public String apply(StreamResource resource, ServletContext context) {
            return Optional.ofNullable(context.getMimeType(resource.getName()))
                    .orElse(DEFAULT_CONTENT_TYPE);
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
            while ((n = read(session, source, buf)) >= 0) {
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

    /**
     * Sets the resolver which is used to lookup the content type of the
     * resource.
     * <p>
     * By default a resolver based on servletContext.getMimeType() is used.
     *
     * @param resolver
     *            content type resolver, not <code>null</code>
     * @return this resource
     */
    public StreamResource setContentTypeResolver(ContentTypeResolver resolver) {
        if (resolver == null) {
            throw new IllegalArgumentException("Resolver cannot be null");
        }
        this.resolver = resolver;
        return this;
    }

    /**
     * Set content type for the resource.
     * <p>
     * This is a shorthand for
     * {@link #setContentTypeResolver(ContentTypeResolver)} with resolver which
     * always returns {@code contentType}
     *
     * @param contentType
     *            resource content type, not <code>null</code>
     * @return this resource
     */
    public StreamResource setContentType(String contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException("Content type cannot be null");
        }
        setContentTypeResolver((resource, context) -> contentType);
        return this;
    }

    /**
     * Gets the resolver which is used to lookup the content type of the
     * resource.
     *
     * @return content type resolver
     */
    public ContentTypeResolver getContentTypeResolver() {
        return resolver;
    }

    @Override
    public String getName() {
        return fileName;
    }
}

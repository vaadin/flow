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

import java.io.InputStream;
import java.io.Serializable;
import java.util.Objects;

/**
 * Represents dynamically generated data.
 * <p>
 * The instance should be registered via
 * {@link VaadinSession#registerResource(StreamResource)}. This method returns
 * an object which may be used to get resource URL.
 * 
 * @author Vaadin Ltd
 *
 */
public class StreamResource implements Serializable {

    /**
     * Creates input stream instances that provides the actual data of the
     * resource.
     * <p>
     * The instance of this class should generate {@link InputStream} for the
     * resource.
     * 
     * @author Vaadin Ltd
     *
     */
    @FunctionalInterface
    public interface InputStreamFactory extends Serializable {
        /**
         * Produce {@link InputStream} instance to read resource data from.
         * <p>
         * Return value may not be null.
         * 
         * @return data input stream. May not be null.
         */
        InputStream createInputStream();
    }

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final String fileName;

    private String contentType = DEFAULT_CONTENT_TYPE;

    private long cacheTime = 0L;

    private final InputStreamFactory streamFactory;

    private boolean requiresLock = true;

    /**
     * Creates {@link StreamResource} instance using mandatory parameters
     * {@code name} as a resource file name and input stream {@code factory} as
     * a factory for data.
     * <p>
     * {@code name} parameter value will be used in URL (generated when resource
     * is registered) in a way that the {@name} is the last segment of the path.
     * So this is synthetic file name (not real one).
     * 
     * @param name
     *            resource file name. May not be null.
     * @param factory
     *            data input stream factory. May not be null.
     */
    public StreamResource(String name, InputStreamFactory factory) {
        assert name != null;
        assert factory != null;
        fileName = name;
        streamFactory = factory;
    }

    /**
     * Returns content type of the resource.
     * 
     * @return resource content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Creates binary input stream which will be used to generate resource
     * content.
     * 
     * @return resource input stream to generate data
     */
    public InputStream createInputStream() {
        return streamFactory.createInputStream();
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
     * If this method returns {@code true} (by default) then reading data from
     * input stream (via {@link #createInputStream()} will be done under session
     * lock and it's safe to access application data within {@link InputStream}
     * read methods. Otherwise session lock won't be acquired. In the latter
     * case one must not try to access application data.
     * <p>
     * {@link #createInputStream()} is called under the session lock. Normally
     * it should be enough to get all required data from the application at this
     * point and use it to produce the data via {@link InputStream}. In this
     * case one should override {@link #requiresLock()} method to return
     * {@code false}. E.g. if {@link InputStream} instance is remote URL input
     * stream then you don't want to lock session on reading data from it.
     * 
     * @return {@code true} if data from the input stream should be read under
     *         the session lock, {@code false} otherwise
     */
    public boolean requiresLock() {
        return requiresLock;
    }

    /**
     * Get the resource file name.
     * <p>
     * The value will be used in URL (generated when resource is registered) in
     * a way that the {@name} is the last segment of the path. So this is
     * synthetic file name (not real one).
     * 
     * @return resource file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Return the input stream factory.
     * 
     * @return input stream factory
     */
    public InputStreamFactory getStreamFactory() {
        return streamFactory;
    }

    /**
     * Set 'requiresLock' property. See {@link #requiresLock()} method for
     * details.
     * 
     * @param requires
     *            'requiresLock' value
     */
    public void setRequiresLock(boolean requires) {
        requiresLock = requires;
    }

    /**
     * Set content type for the resource.
     * 
     * @param contentType
     *            resource content type
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Set cache time in millis. Zero or negative value disables the caching of
     * this stream.
     * 
     * @param cacheTime
     *            cache time
     */
    public void setCacheTime(long cacheTime) {
        this.cacheTime = cacheTime;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        } else if (getClass().equals(obj.getClass())) {
            StreamResource that = (StreamResource) obj;
            return Objects.equals(getCacheTime(), that.getCacheTime())
                    && Objects.equals(getContentType(), that.getContentType())
                    && Objects.equals(getFileName(), that.getFileName())
                    && Objects.equals(getStreamFactory(),
                            that.getStreamFactory())
                    && Objects.equals(requiresLock(), that.requiresLock());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCacheTime(), getContentType(), getFileName(),
                getStreamFactory(), requiresLock());
    }

}

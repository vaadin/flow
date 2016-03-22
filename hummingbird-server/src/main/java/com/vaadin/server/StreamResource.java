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

/**
 * Instance of this class represents dynamically generated data.
 * <p>
 * The instance should be registered via
 * {@link VaadinSession#register(StreamResource)}. This method generates unique
 * URI which may be used to request the resource.
 * 
 * @author Vaadin Ltd
 *
 */
public class StreamResource implements Serializable {

    /**
     * Input stream factory.
     * <p>
     * The instance of this class should generate {@link InputStream} for the
     * resource.
     * 
     * @author Vaadin Ltd
     *
     */
    @FunctionalInterface
    public interface InputStreamFactory {
        /**
         * Produce {@link InputStream} instance to resource read data from.
         * <p>
         * Return value may not be null.
         * 
         * @return data input stream. May not be null.
         */
        InputStream createInputStream();
    }

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private String fileName;

    private String contentType = DEFAULT_CONTENT_TYPE;

    private long cacheTime = 1000L * 60 * 60 * 24;

    private InputStreamFactory streamFactory;

    private boolean requiresLock = true;

    /**
     * Creates {@link StreamResource} instance using mandatory parameters
     * {@code name} as a resource file name and input stream {@code factory} as
     * a factory for data.
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
     * Binary input stream which will be used to generate resource content.
     * 
     * @return resource input stream to generate data
     */
    public InputStream createInputStream() {
        return streamFactory.createInputStream();
    }

    /**
     * Gets the length of cache expiration time. This gives the possibility
     * cache resource.
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
     * Method {@link #createInputStream()} is called under the session lock.
     * Normally it should be enough to get all required data from the
     * application at this point and use it to produce the data via
     * {@link InputStream}. In this case one should override
     * {@link #requiresLock()} method to return {@code false}. F.e. if
     * {@link InputStream} instance is remote URL input stream then you don't
     * want to lock session on reading data from it.
     * 
     * @return
     */
    public boolean requiresLock() {
        return requiresLock;
    }

    /**
     * Get resource file name.
     * 
     * @return
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Set resource file name.
     * <p>
     * {@code name} may not be null.
     * 
     * @param name
     *            resource file name. May not be null.
     */
    public void setFileName(String name) {
        assert name != null;
        fileName = name;
    }

    /**
     * Return input stream factory.
     * 
     * @return input stream factory
     */
    public InputStreamFactory getStreamFactory() {
        return streamFactory;
    }

    /**
     * Set input stream factory.
     * 
     * @param factory
     *            input stream factory
     */
    public void setStreamFactory(InputStreamFactory factory) {
        streamFactory = factory;
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
     * Set cache time;
     * 
     * @param time
     *            resource cache time
     */
    public void setCacheTime(long time) {
        cacheTime = time;
    }

}

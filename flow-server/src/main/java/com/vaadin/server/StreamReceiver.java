/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import java.io.OutputStream;
import java.io.Serializable;
import java.util.UUID;

public class StreamReceiver implements Serializable {

    private long cacheTime = 0L;

    private int nodeId;

    private final String attributeName;

    private final StreamResourceWriter writer;

    private final StreamVariable streamVariable;

    private final String id = UUID.randomUUID().toString();

    /**
     * Creates {@link StreamReceiver} instance using mandatory parameter output
     * stream {@code writer} as a data receiver. {@code writer} should write
     * data in the output stream provided as an argument to its
     * {@link StreamResourceWriter#accept(OutputStream, VaadinSession)} method.
     * <p>
     * {@code name} parameter value will be used in URI (generated when resource
     * is registered) in a way that the {@code name} is the last segment of the
     * path. So this is synthetic file name (not real one).
     *
     * @param writer
     *            data output stream consumer
     */
    public StreamReceiver(int nodeId, String attributeName,
            StreamVariable streamVariable, StreamResourceWriter writer) {
        assert writer != null;
        assert attributeName != null;
        assert streamVariable != null;

        this.nodeId = nodeId;
        this.writer = writer;
        this.streamVariable = streamVariable;
        this.attributeName = attributeName;
    }

    /**
     * Creates {@link StreamResource} instance using mandatory parameter input
     * stream {@code factory} as a factory for data.
     * <p>
     * {@code name} parameter value will be used in URI (generated when resource
     * is registered) in a way that the {@code name} is the last segment of the
     * path. So this is synthetic file name (not real one).
     *
     * @param factory
     *            data input stream factory. May not be null.
     */
    public StreamReceiver(int nodeId, String attributeName,
            StreamVariable streamVariable, InputStreamFactory factory) {
        this(nodeId, attributeName, streamVariable,
                new StreamResource.Pipe(factory));
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
     * Set cache time in millis. Zero or negative value disables the caching of
     * this stream.
     *
     * @param cacheTime
     *            cache time
     * @return this resource
     */
    public StreamReceiver setCacheTime(long cacheTime) {
        this.cacheTime = cacheTime;
        return this;
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
     * Gets unique identifier of the resource.
     *
     * @return the resource unique id
     */
    public final String getId() {
        return id;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public int getNodeId() {
        return nodeId;
    }

    public StreamVariable getStreamVariable() {
        return streamVariable;
    }
}

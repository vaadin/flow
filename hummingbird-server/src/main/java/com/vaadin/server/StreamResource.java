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
 * @author Vaadin Ltd
 *
 */
public interface StreamResource extends Serializable {

    /**
     * Get resource filename.
     * 
     * @return resource filename
     */
    String getFilename();

    /**
     * Returns content type of the resource. Default is
     * "application/octet-stream".
     * 
     * @return resource content type
     */
    default String getContentType() {
        return "application/octet-stream";
    }

    /**
     * Binary input stream which will be used to generate resource content.
     * 
     * @return resource input stream to generate data
     */
    InputStream createInputStream();

    /**
     * Gets the length of cache expiration time. This gives the possibility
     * cache resource.
     * 
     * @return Cache time in milliseconds.
     */
    default long getCacheTime() {
        return 1000 * 60 * 60 * 24;
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
    default boolean requiresLock() {
        return true;
    }

}

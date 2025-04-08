/*
 * Copyright 2000-2025 Vaadin Ltd.
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

package com.vaadin.flow.server.streams;

import java.io.IOException;
import java.io.InputStream;

import com.vaadin.flow.server.DownloadHandler;
import com.vaadin.flow.server.VaadinSession;

/**
 * Abstract class for common methods used in pre-made download handlers.
 *
 * @since 24.8
 */
public abstract class AbstractDownloadHandler implements DownloadHandler {

    /**
     * Read buffer amount of bytes from the input stream.
     *
     * @param session
     *            vaadin session in use
     * @param source
     *            input stream source
     * @param buffer
     *            byte buffer to read into
     * @return amount of bytes read into buffer
     * @throws IOException
     *             If the first byte cannot be read for any reason other than
     *             the end of the file, if the input stream has been closed, or
     *             if some other I/O error occurs.
     */
    protected int read(VaadinSession session, InputStream source, byte[] buffer)
            throws IOException {
        session.lock();
        try {
            return source.read(buffer);
        } finally {
            session.unlock();
        }
    }

}

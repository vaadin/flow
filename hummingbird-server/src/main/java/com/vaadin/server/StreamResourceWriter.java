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
import java.io.OutputStream;

/**
 * Output stream consumer. Implementation of this interface should write data
 * into {@link OutputStream} instance provided as an argument to its
 * {@link #accept(OutputStream, VaadinSession)} method.
 * 
 * @author Vaadin Ltd
 *
 */
@FunctionalInterface
public interface StreamResourceWriter {

    /**
     * Handles {@code stream} (writes data to it) using {@code session} as a
     * context.
     * 
     * @param stream
     *            data output stream
     * @param session
     *            vaadin session
     * @throws IOException
     */
    void accept(OutputStream stream, VaadinSession session) throws IOException;
}

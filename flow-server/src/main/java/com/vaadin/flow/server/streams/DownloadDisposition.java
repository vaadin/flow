/*
 * Copyright 2000-2026 Vaadin Ltd.
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

/**
 * Whether the browser should show downloaded content or save it as a file, as
 * sent in the {@code Content-Disposition} header.
 * <p>
 * Whether content is shown or saved depends on where it is used, not on what it
 * is: the same PDF can be shown in a new browser tab and saved from a download
 * link. The place that registers a {@link DownloadHandler}, such as a component
 * that shows an image, sets the disposition on the registered resource. The
 * handler then keeps the file name it gives and the type is taken from the
 * disposition.
 *
 * @see com.vaadin.flow.server.StreamResourceRegistry.ElementStreamResource
 * @since 25.4
 */
public enum DownloadDisposition {

    /**
     * The browser shows the content, for example as an image, in an iframe or
     * in a new tab.
     */
    INLINE("inline"),

    /**
     * The browser saves the content as a file.
     */
    ATTACHMENT("attachment");

    private final String headerValue;

    DownloadDisposition(String headerValue) {
        this.headerValue = headerValue;
    }

    /**
     * Gets the type as written in the {@code Content-Disposition} header.
     *
     * @return the header type, not {@code null}
     */
    String getHeaderValue() {
        return headerValue;
    }
}

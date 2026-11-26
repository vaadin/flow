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
package com.vaadin.flow.component.html;

/**
 * Mode settings for anchor.
 *
 * {@link #DOWNLOAD} will set the download attribute to Anchor, where as
 * {@link #INLINE} will remove it. When the anchor links to a
 * {@link com.vaadin.flow.server.streams.DownloadHandler}, the type also decides
 * whether its content is served as an attachment or inline, when the handler
 * gives only a file name, as the built-in handlers do.
 * <p>
 * Content served inline is rendered by the browser on the application's origin.
 * Do not serve untrusted content, such as user uploaded HTML or SVG files,
 * inline.
 *
 * @since 24.8
 */
public enum AttachmentType {
    /**
     * The linked resource is downloaded instead of being opened.
     */
    DOWNLOAD,

    /**
     * The linked resource is opened by the browser, which is the default.
     */
    INLINE
}

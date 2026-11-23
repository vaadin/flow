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
/**
 * Server-side API for delivering content to the browser from buttons and other
 * clickable components, either saved as a file or opened in a new browser tab.
 * <p>
 * Use
 * {@link com.vaadin.flow.component.download.Download#onClick(com.vaadin.flow.component.Component, com.vaadin.flow.server.streams.DownloadHandler)
 * Download.onClick(component, handler)} to make a click download the content of
 * a {@link com.vaadin.flow.server.streams.DownloadHandler DownloadHandler} or a
 * URL, and pass a {@link com.vaadin.flow.component.download.DownloadTarget
 * DownloadTarget} to open it in a new tab instead.
 * <p>
 * Downloads go through {@code onClick} rather than an ordinary server-side
 * click listener so that the download starts, or the tab opens, in the
 * browser's own click handler, inside the user gesture and without waiting for
 * a server round trip.
 */
@NullMarked
package com.vaadin.flow.component.download;

import org.jspecify.annotations.NullMarked;

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

import java.io.IOException;

import com.vaadin.flow.server.streams.DownloadEvent.Disposition;

/**
 * A view of a download handler that serves the content inline or as an
 * attachment, whatever the handler itself asks for. Returned by
 * {@link DownloadHandler#serveInline()} and
 * {@link DownloadHandler#serveAsAttachment()}.
 */
class DispositionDownloadHandler extends DelegatingDownloadHandler {

    private final Disposition disposition;

    DispositionDownloadHandler(DownloadHandler delegate,
            Disposition disposition) {
        super(delegate);
        this.disposition = disposition;
    }

    /**
     * Returns a view of the given handler that serves its content with the
     * given disposition, or the handler itself if it already is such a view.
     */
    static DownloadHandler wrap(DownloadHandler handler,
            Disposition disposition) {
        if (handler instanceof DispositionDownloadHandler wrapper
                && wrapper.disposition == disposition) {
            return handler;
        }
        return new DispositionDownloadHandler(handler, disposition);
    }

    @Override
    public void handleDownloadRequest(DownloadEvent event) throws IOException {
        event.requireDisposition(disposition);
        super.handleDownloadRequest(event);
    }
}

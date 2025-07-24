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

import java.util.Optional;

import com.vaadin.flow.server.VaadinResponse;

/**
 * Abstract class for common methods used in pre-made download handlers.
 *
 * @param <R>
 *            the type of the subclass implementing this abstract class
 * @since 24.8
 */
public abstract class AbstractDownloadHandler<R extends AbstractDownloadHandler>
        extends TransferProgressAwareHandler<DownloadEvent, R>
        implements DownloadHandler {

    // Content-Disposition: attachment by default
    private boolean inline = false;

    @Override
    protected TransferContext getTransferContext(DownloadEvent transferEvent) {
        return new TransferContext(transferEvent.getRequest(),
                transferEvent.getResponse(), transferEvent.getSession(),
                transferEvent.getFileName(), transferEvent.getOwningElement(),
                transferEvent.getContentLength());
    }

    protected String getContentType(String fileName, VaadinResponse response) {
        return Optional.ofNullable(response.getService().getMimeType(fileName))
                .orElse("application/octet-stream");
    }

    /**
     * Sets this download content to be displayed inside the Web page, or as the
     * Web page, e.g. as an image or inside an iframe.
     * <p>
     * Implementations of this class should ensure that the
     * 'Content-Disposition' attribute is 'inline', if this method is called.
     *
     * @return this instance for method chaining
     */
    public R inline() {
        inline = true;
        return (R) this;
    }

    /**
     * Returns if the download content to be displayed inside the Web page or
     * downloaded as a file.
     *
     * @return true if the content is to be displayed inline, false if it is to
     *         be downloaded as a file
     */
    public boolean isInline() {
        return inline;
    }
}

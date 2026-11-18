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

import com.vaadin.flow.dom.DisabledUpdateMode;

/**
 * A download handler that forwards everything to another handler. Subclasses
 * override what their view of the handler changes, such as
 * {@link DownloadHandler#allowDisabled()}.
 */
abstract class DelegatingDownloadHandler implements DownloadHandler {

    private final DownloadHandler delegate;

    DelegatingDownloadHandler(DownloadHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void handleDownloadRequest(DownloadEvent event) throws IOException {
        delegate.handleDownloadRequest(event);
    }

    @Override
    public String getUrlPostfix() {
        return delegate.getUrlPostfix();
    }

    @Override
    public boolean isAllowInert() {
        return delegate.isAllowInert();
    }

    @Override
    public boolean isRangeRequestsEnabled() {
        return delegate.isRangeRequestsEnabled();
    }

    @Override
    public DisabledUpdateMode getDisabledUpdateMode() {
        return delegate.getDisabledUpdateMode();
    }
}

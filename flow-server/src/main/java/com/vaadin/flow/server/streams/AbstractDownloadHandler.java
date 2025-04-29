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
import java.util.Collection;

import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.server.DownloadHandler;
import com.vaadin.flow.server.DownloadRequest;
import com.vaadin.flow.server.TransferProgressAware;
import com.vaadin.flow.server.TransferProgressListener;

/**
 * Abstract class for common methods used in pre-made download handlers.
 *
 * @since 24.8
 */
public abstract class AbstractDownloadHandler extends
        TransferProgressAwareHandler<DownloadRequest, AbstractDownloadHandler>
        implements DownloadHandler {

    @Override
    public void handleDownloadRequest(DownloadRequest request) {
        Collection<TransferProgressListener> listeners = getListeners();
        TransferContext transferContext = getTransferContext(request);
        listeners.forEach(listener -> {
            request.getUI().access(() -> {
                listener.onStart(transferContext);
            });
        });
        handleTransfer(request);
    }

    @Override
    protected TransferContext getTransferContext(
            DownloadRequest transferEvent) {
        return new TransferContext(transferEvent.getRequest(),
                transferEvent.getResponse(), transferEvent.getSession(),
                transferEvent.getFileName(), transferEvent.owningElement(), -1);
    }
}

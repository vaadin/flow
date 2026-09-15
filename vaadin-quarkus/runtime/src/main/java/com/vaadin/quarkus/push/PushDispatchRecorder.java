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
package com.vaadin.quarkus.push;

import io.quarkus.runtime.annotations.Recorder;
import org.jboss.logging.Logger;

/**
 * PushDispatchRecorder is responsible for logging information about the
 * dispatch behavior of Vaadin Push websocket frames in the context of the
 * Quarkus framework. The logging provides clarity on how websocket frames are
 * dispatched with respect to the Vert.x event loop and the worker thread pool.
 * <p>
 * By default, websocket frames are dispatched on the worker thread pool
 * (`quarkus.websocket.dispatch-to-worker=true`) to ensure the Vert.x event loop
 * remains free. This is particularly important when application code blocks
 * while holding the Vaadin session lock, such as in `BeforeEnterObserver` or
 * `AfterNavigationListener` implementations.
 * <p>
 * The provided logging documentation offers insights into the behavior and
 * implications of changing the dispatch behavior by altering the relevant
 * Quarkus configuration property.
 */
@Recorder
public class PushDispatchRecorder {

    private static final Logger LOG = Logger
            .getLogger(PushDispatchRecorder.class);

    public void logDispatchOnWorker() {
        LOG.debug(
                """
                        Vaadin Push websocket frames are dispatched on the worker thread pool \
                        (quarkus.websocket.dispatch-to-worker=true) to keep the Vert.x event \
                        loop free when application code blocks while holding the Vaadin session \
                        lock (e.g. in BeforeEnterObserver or AfterNavigationListener). Setting \
                        this property to false reverts to the Quarkus default of dispatching on \
                        the event loop, which may cause the application to hang in such scenarios.\
                        """);
    }
}

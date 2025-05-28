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

package com.vaadin.flow.internal.streams;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;

/**
 * Event notifying the upload component that the upload has been completed.
 * <p>
 * This event is typically used in conjunction with file upload components and
 * {@link com.vaadin.flow.server.streams.UploadHandler} to indicate that the
 * upload process has finished. This event is internal and is not intended for
 * public use.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param <C>
 *            the event source type, which is a subclass of {@link Component}
 *
 * @since 24.8
 */
public class UploadCompleteEvent<C extends Component>
        extends ComponentEvent<C> {
    /**
     * Creates a new event using the given source. Always fired on the server
     * side.
     *
     * @param source
     *            the source component
     */
    public UploadCompleteEvent(C source) {
        super(source, false);
    }
}

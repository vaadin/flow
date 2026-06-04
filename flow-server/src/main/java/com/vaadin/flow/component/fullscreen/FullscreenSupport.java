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
package com.vaadin.flow.component.fullscreen;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Per-UI holder for the fullscreen state signal, the DOM listener that tracks
 * browser fullscreen changes, and the executeJs call that exits fullscreen.
 * Created lazily on first access via {@link Fullscreen} and attached to the UI
 * through {@link com.vaadin.flow.component.ComponentUtil ComponentUtil} data.
 */
final class FullscreenSupport implements Serializable {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(FullscreenSupport.class);

    private final UI ui;
    private final ValueSignal<FullscreenState> state = new ValueSignal<>(
            FullscreenState.UNKNOWN);
    private final Signal<FullscreenState> stateReadOnly = state.asReadonly();

    FullscreenSupport(UI ui) {
        this.ui = ui;
        ui.getElement()
                .addEventListener("vaadin-fullscreen-change",
                        e -> setStateFromClient(e.getEventDetail(String.class)))
                .addEventDetail().allowInert();
    }

    Signal<FullscreenState> stateSignal() {
        return stateReadOnly;
    }

    void exit() {
        ui.getPage()
                .executeJs("window.Vaadin.Flow.fullscreen.exitFullscreen()");
    }

    void setStateFromClient(String value) {
        if (value == null) {
            return;
        }
        try {
            state.set(FullscreenState.valueOf(value));
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Unknown fullscreen state value from client: {}",
                    value);
        }
    }
}

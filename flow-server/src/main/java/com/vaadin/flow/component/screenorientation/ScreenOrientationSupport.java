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
package com.vaadin.flow.component.screenorientation;

import java.io.Serializable;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Per-UI holder for the screen orientation signal, the DOM listener that tracks
 * browser orientation changes, and the executeJs calls that lock and unlock the
 * orientation. Created lazily on first access via {@link ScreenOrientation} and
 * attached to the UI through {@link com.vaadin.flow.component.ComponentUtil
 * ComponentUtil} data.
 */
final class ScreenOrientationSupport implements Serializable {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ScreenOrientationSupport.class);

    private final UI ui;
    private final ValueSignal<ScreenOrientationData> orientation = new ValueSignal<>(
            new ScreenOrientationData(ScreenOrientationType.UNKNOWN, 0));
    private final Signal<ScreenOrientationData> orientationReadOnly = orientation
            .asReadonly();

    ScreenOrientationSupport(UI ui) {
        this.ui = ui;
        ui.getElement().addEventListener("vaadin-screen-orientation-change",
                e -> setStateFromClient(
                        e.getEventDetail(ScreenOrientationDetail.class)))
                .addEventDetail().allowInert();
    }

    Signal<ScreenOrientationData> orientationSignal() {
        return orientationReadOnly;
    }

    void lock(ScreenOrientationType orientation, SerializableRunnable onSuccess,
            SerializableConsumer<ScreenOrientationLockError> onError) {
        ui.getElement()
                .executeJs(
                        "return window.Vaadin.Flow.screenOrientation.lock($0)",
                        orientation.getClientValue())
                .then(LockResult.class, result -> {
                    if (result.success()) {
                        onSuccess.run();
                    } else {
                        onError.accept(new ScreenOrientationLockError(
                                result.name() == null ? "UnknownError"
                                        : result.name(),
                                result.message() == null ? ""
                                        : result.message()));
                    }
                }, bridgeError -> onError.accept(new ScreenOrientationLockError(
                        "BridgeError", bridgeError)));
    }

    void unlock() {
        ui.getElement()
                .executeJs("window.Vaadin.Flow.screenOrientation.unlock()");
    }

    void unlock(SerializableRunnable onComplete) {
        ui.getElement()
                .executeJs("window.Vaadin.Flow.screenOrientation.unlock()")
                .then(ignored -> onComplete.run());
    }

    /**
     * Sets the orientation from raw client-side values. {@code null} or empty
     * type means the bootstrap parameters are absent (e.g. in a unit-test
     * scenario) and the previous value is preserved. The client reports
     * {@code "unsupported"} when the browser does not implement the Screen
     * Orientation API, which maps to {@link ScreenOrientationType#UNSUPPORTED}.
     * Unknown type values are logged at debug level so a forward-compatible
     * client value does not silently disappear.
     */
    void setStateFromClient(@Nullable String type, @Nullable String angle) {
        if (type == null || type.isEmpty()) {
            return;
        }
        try {
            int angleValue = angle == null ? 0 : Integer.parseInt(angle);
            orientation.set(new ScreenOrientationData(
                    ScreenOrientationType.fromClientValue(type), angleValue));
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Unknown screen orientation value from client: "
                    + "type={} angle={}", type, angle);
        }
    }

    private void setStateFromClient(@Nullable ScreenOrientationDetail detail) {
        if (detail == null) {
            return;
        }
        setStateFromClient(detail.type(), String.valueOf(detail.angle()));
    }

    private record ScreenOrientationDetail(@Nullable String type,
            int angle) implements Serializable {
    }

    private record LockResult(boolean success, @Nullable String name,
            @Nullable String message) implements Serializable {
    }
}

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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.geolocation.GeolocationError;
import com.vaadin.flow.component.geolocation.GeolocationOptions;
import com.vaadin.flow.component.geolocation.GeolocationPosition;
import com.vaadin.flow.component.geolocation.GeolocationTracker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.GeolocationView", layout = ViewTestLayout.class)
public class GeolocationView extends AbstractDivView {

    private GeolocationTracker tracker;
    private int trackUpdateCount;

    @Override
    protected void onShow() {
        Div debug = new Div();
        debug.setId("debug");
        add(debug);

        // Check if geolocation API is available and report status
        UI.getCurrent().getPage().executeJs(
                """
                        const debugEl = document.getElementById('debug');
                        const info = [];
                        info.push('Vaadin: ' + (typeof window.Vaadin));
                        info.push('Flow: ' + (typeof window.Vaadin?.Flow));
                        info.push('geolocation: ' + (typeof window.Vaadin?.Flow?.geolocation));
                        if (window.Vaadin?.Flow?.geolocation) {
                            info.push('get: ' + (typeof window.Vaadin.Flow.geolocation.get));
                            info.push('watch: ' + (typeof window.Vaadin.Flow.geolocation.watch));
                        }
                        debugEl.textContent = info.join(', ');
                        """);

        // Mock navigator.geolocation so tests work without real permissions.
        // The mock reacts to the options passed in:
        // - getCurrentPosition with `maximumAge === 9999` returns an error
        // (used to drive the error branch deterministically).
        // - watchPosition emits a new position every 50 ms, each with a
        // different timestamp, until clearWatch is called.
        UI.getCurrent().getPage().executeJs(
                """
                        const mockCoords = (lat, lon) => ({
                            coords: {
                                latitude: lat,
                                longitude: lon,
                                accuracy: 10.0,
                                altitude: 25.5,
                                altitudeAccuracy: 5.0,
                                heading: 90.0,
                                speed: 1.5
                            },
                            timestamp: Date.now()
                        });
                        const watches = new Map();
                        let nextWatchId = 1;
                        Object.defineProperty(navigator, 'geolocation', {
                            value: {
                                getCurrentPosition: function(success, error, options) {
                                    setTimeout(function() {
                                        if (options && options.maximumAge === 9999) {
                                            error({ code: 1, message: 'User denied geolocation' });
                                        } else {
                                            success(mockCoords(60.1699, 24.9384));
                                        }
                                    }, 0);
                                },
                                watchPosition: function(success, error, options) {
                                    const id = nextWatchId++;
                                    const interval = setInterval(function() {
                                        success(mockCoords(60.1699, 24.9384));
                                    }, 50);
                                    watches.set(id, interval);
                                    return id;
                                },
                                clearWatch: function(id) {
                                    const interval = watches.get(id);
                                    if (interval) {
                                        clearInterval(interval);
                                        watches.delete(id);
                                    }
                                }
                            },
                            configurable: true
                        });
                        """);

        NativeButton getButton = createButton("Get Position", "getButton",
                e -> UI.getCurrent().getGeolocation().get(outcome -> {
                    Div out = new Div();
                    out.setId("getResult");
                    switch (outcome) {
                    case GeolocationPosition pos ->
                        out.setText("lat=" + pos.coords().latitude() + ", lon="
                                + pos.coords().longitude());
                    case GeolocationError error -> out.setText(
                            "error=" + error.code() + ":" + error.message());
                    }
                    add(out);
                }));

        // Uses the mock's "maximumAge == -1 → error" trigger to exercise
        // the error branch.
        NativeButton getErrorButton = createButton("Get Position (error)",
                "getErrorButton", e -> UI.getCurrent().getGeolocation().get(
                        new GeolocationOptions(null, null, 9999), outcome -> {
                            Div out = new Div();
                            out.setId("getErrorResult");
                            switch (outcome) {
                            case GeolocationPosition pos -> out.setText(
                                    "unexpected position: " + pos.coords());
                            case GeolocationError error ->
                                out.setText("error=" + error.errorCode() + ":"
                                        + error.message());
                            }
                            add(out);
                        }));

        NativeButton trackButton = createButton("Track Position", "trackButton",
                e -> {
                    tracker = UI.getCurrent().getGeolocation().track(this);
                    trackUpdateCount = 0;
                    getElement().addEventListener("vaadin-geolocation-position",
                            ev -> {
                                var value = tracker.valueSignal().peek();
                                if (value instanceof GeolocationPosition pos) {
                                    trackUpdateCount++;
                                    Div out = new Div();
                                    out.setId("trackResult" + trackUpdateCount);
                                    out.setText("lat=" + pos.coords().latitude()
                                            + ", lon="
                                            + pos.coords().longitude());
                                    add(out);
                                }
                            }).addEventDetail();
                });

        NativeButton stopButton = createButton("Stop tracking", "stopButton",
                e -> {
                    if (tracker != null) {
                        tracker.stop();
                        Div out = new Div();
                        out.setId("stopResult");
                        out.setText("stopped after " + trackUpdateCount
                                + " updates");
                        add(out);
                    }
                });

        add(getButton, getErrorButton, trackButton, stopButton);
    }
}

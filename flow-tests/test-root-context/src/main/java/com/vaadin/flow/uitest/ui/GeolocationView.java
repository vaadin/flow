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
import com.vaadin.flow.component.geolocation.Geolocation;
import com.vaadin.flow.component.geolocation.GeolocationPosition;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.GeolocationView", layout = ViewTestLayout.class)
public class GeolocationView extends AbstractDivView {

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
        // getCurrentPosition calls success immediately with fixed coordinates.
        // watchPosition calls success once, then returns a watch id.
        UI.getCurrent().getPage().executeJs(
                """
                        const mockPosition = {
                            coords: {
                                latitude: 60.1699,
                                longitude: 24.9384,
                                accuracy: 10.0,
                                altitude: 25.5,
                                altitudeAccuracy: 5.0,
                                heading: 90.0,
                                speed: 1.5
                            },
                            timestamp: Date.now()
                        };
                        let nextWatchId = 1;
                        Object.defineProperty(navigator, 'geolocation', {
                            value: {
                                getCurrentPosition: function(success, error, options) {
                                    setTimeout(function() { success(mockPosition); }, 0);
                                },
                                watchPosition: function(success, error, options) {
                                    const id = nextWatchId++;
                                    setTimeout(function() { success(mockPosition); }, 0);
                                    return id;
                                },
                                clearWatch: function(id) {}
                            },
                            configurable: true
                        });
                        """);

        NativeButton getButton = createButton("Get Position", "getButton",
                e -> {
                    Geolocation.get(pos -> {
                        Div result = new Div();
                        result.setId("getResult");
                        result.setText("lat=" + pos.coords().latitude()
                                + ", lon=" + pos.coords().longitude());
                        add(result);
                    }, error -> {
                        Div result = new Div();
                        result.setId("getResult");
                        result.setText("error=" + error.code() + ":"
                                + error.message());
                        add(result);
                    });
                });

        NativeButton trackButton = createButton("Track Position", "trackButton",
                e -> {
                    Geolocation geo = Geolocation.track(this);
                    // Listen for the position event directly to avoid
                    // race conditions with signal update timing
                    getElement().addEventListener("vaadin-geolocation-position",
                            ev -> {
                                var state = geo.state().peek();
                                Div result = new Div();
                                result.setId("trackResult");
                                if (state instanceof GeolocationPosition pos) {
                                    result.setText("lat="
                                            + pos.coords().latitude() + ", lon="
                                            + pos.coords().longitude());
                                } else {
                                    result.setText(
                                            "unexpected state: " + state);
                                }
                                add(result);
                            }).addEventDetail();
                });

        add(getButton, trackButton);
    }
}

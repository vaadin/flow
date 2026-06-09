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

import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.screenorientation.ScreenOrientation;
import com.vaadin.flow.component.screenorientation.ScreenOrientationData;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ScreenOrientationView", layout = ViewTestLayout.class)
public class ScreenOrientationView extends AbstractDivView {

    @Override
    protected void onShow() {
        Div type = new Div();
        type.setId("type");
        Div angle = new Div();
        angle.setId("angle");
        Div updates = new Div();
        updates.setId("updates");
        updates.setText("0");
        add(type, angle, updates);

        Signal<ScreenOrientationData> signal = ScreenOrientation
                .orientationSignal();
        AtomicInteger count = new AtomicInteger();
        Signal.effect(this, () -> {
            ScreenOrientationData data = signal.get();
            type.setText(data.type().name());
            angle.setText(String.valueOf(data.angle()));
            updates.setText(String.valueOf(count.incrementAndGet()));
        });
    }
}
